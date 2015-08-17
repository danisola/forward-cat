package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import com.google.inject.AbstractModule;
import models.MailSender;
import models.Repository;
import org.apache.mailet.MailAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;

import javax.mail.internet.AddressException;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static controllers.TestUtils.toMailAddress;
import static controllers.TestUtils.whenAddressExistsReturn;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class AddProxyTest extends PlayTest {

    MailAddress proxyMail = toMailAddress("test@forward.cat");

    @Mock MailSender mailSender;
    @Mock Repository repository;

    @Override
    public AbstractModule getModule() throws IOException, AddressException {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class).toInstance(repository);
                bind(MailSender.class).toInstance(mailSender);
            }
        };
    }

    @Test
    public void wrongEmail_sendBadRequest() throws Exception {
        Result route = route(request("test", "not an address"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void wrongUsername_sendBadRequest() throws Exception {
        Result route = route(request("not valid", "user@mail.com"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void chainedProxy_sendBadRequest() throws Exception {
        Result route = route(request("test", "email@forward.cat"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void proxyAlreadyExists_sendBadRequest() throws Exception {
        whenAddressExistsReturn(repository, proxyMail, true);

        Result route = route(request("test", "user@mail.com"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test(expected = RuntimeException.class)
    public void connectionError_sendBadRequest() throws Exception {
        when(repository.proxyExists(proxyMail)).thenThrow(new RuntimeException());

        Result route = route(request("test", "user@mail.com"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void tooManyProxies_sendBadRequest() throws Exception {
        ProxyMail[] proxies = {mock(ProxyMail.class), mock(ProxyMail.class), mock(ProxyMail.class)};
        User user = User.create(toMailAddress("user@mail.com"), new Date(), proxies);
        whenAddressExistsReturn(repository, proxyMail, false);
        when(repository.getUser(toMailAddress("user@mail.com"))).thenReturn(Optional.of(user));

        Result route = route(request("test", "user@mail.com"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void everythingFine_sendMailAndGoodResponse() throws Exception {
        whenAddressExistsReturn(repository, proxyMail, false);
        when(repository.getUser(toMailAddress("user@mail.com"))).thenReturn(empty());

        Result route = route(request("test", "user@mail.com"));
        assertThat(status(route), is(OK));

        verify(repository).save(any(User.class));
        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }

    private FakeRequest request(String proxy, String email) {
        return fakeRequest(GET, "/add?proxy=" + proxy + "&email=" + email);
    }
}
