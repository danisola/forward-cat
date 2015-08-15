package controllers;

import com.forwardcat.common.ProxyMail;
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

import java.io.IOException;
import java.util.Optional;

import static controllers.TestUtils.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ReportTest extends PlayTest {

    MailAddress inactiveProxy = toMailAddress("inactive@forward.cat");
    ProxyMail inactive = inactiveProxy(inactiveProxy);

    MailAddress blockedProxy = toMailAddress("blocked@forward.cat");
    ProxyMail blocked = activeProxy(blockedProxy, true);

    MailAddress spammerProxy = toMailAddress("spammer@forward.cat");
    ProxyMail spammer = activeProxy(blockedProxy, false);

    @Mock Repository repository;
    @Mock MailSender mailSender;

    @Override
    public AbstractModule getModule() throws IOException {
        when(repository.getProxy(any(MailAddress.class))).thenAnswer(invocationOnMock -> {
            String strAddress = invocationOnMock.getArguments()[0].toString();
            if (inactive.toString().equals(strAddress)) {
                return Optional.of(inactive);
            } else if (blocked.toString().equals(strAddress)) {
                return Optional.of(blocked);
            } else if (spammerProxy.toString().equals(strAddress)) {
                return Optional.of(spammer);
            }
            return Optional.empty();
        });

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class).toInstance(repository);
                bind(MailSender.class).toInstance(mailSender);
            }
        };
    }

    @Test
    public void nonLocalEmail_dontSendEmail() throws Exception {
        Result route = route(request("user@mail.com"));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void nonExistantProxy_dontSendEmail() throws Exception {
        Result route = route(request("not_there@forward.cat"));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void nonActiveProxy_dontSendEmail() throws Exception {
        Result route = route(request(inactiveProxy.toString()));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void blockedProxy_dontSendEmail() throws Exception {
        Result route = route(request(blockedProxy.toString()));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void regularProxy_sendEmail() throws Exception {
        Result route = route(request(spammerProxy.toString()));
        assertThat(status(route), is(OK));
        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }

    private FakeRequest request(String proxy) {
        return fakeRequest(GET, "/report-user?proxy=" + proxy + "&message=hey%20there");
    }
}
