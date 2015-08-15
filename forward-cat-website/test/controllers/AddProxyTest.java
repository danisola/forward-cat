package controllers;

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

import static controllers.TestUtils.toMailAddress;
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
    public void wrongDuration_sendBadRequest() throws Exception {
        Result route = route(request("test", "user@mail.com", 10));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void wrongEmail_sendBadRequest() throws Exception {
        Result route = route(request("test", "not an address", 3));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void wrongUsername_sendBadRequest() throws Exception {
        Result route = route(request("not valid", "user@mail.com", 3));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void chainedProxy_sendBadRequest() throws Exception {
        Result route = route(request("test", "email@forward.cat", 3));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void proxyAlreadyExists_sendBadRequest() throws Exception {
        when(repository.proxyExists(proxyMail)).thenReturn(Boolean.TRUE);

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test(expected = RuntimeException.class)
    public void connectionError_sendBadRequest() throws Exception {
        when(repository.proxyExists(proxyMail)).thenThrow(new RuntimeException());

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void everythingFine_sendMailAndGoodResponse() throws Exception {
        when(repository.proxyExists(proxyMail)).thenReturn(Boolean.FALSE);
        when(repository.getUser(toMailAddress("user@mail.com"))).thenReturn(empty());

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(OK));

        verify(repository).save(any(User.class));
        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }

    private FakeRequest request(String proxy, String email, Integer duration) {
        return fakeRequest(GET, "/add?proxy=" + proxy + "&email=" + email + "&duration=" + duration);
    }
}
