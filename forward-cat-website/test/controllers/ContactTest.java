package controllers;

import com.google.inject.AbstractModule;
import models.MailSender;
import org.apache.mailet.MailAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ContactTest extends PlayTest {

    @Mock MailSender mailSender;

    @Override
    public AbstractModule getModule() throws IOException {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(MailSender.class).toInstance(mailSender);
            }
        };
    }

    @Test
    public void contactPageRequested_isServed() throws Exception {
        Result route = route(contactLandingRequest());
        assertThat(status(route), is(OK));
    }

    @Test
    public void contactSentRequested_emailSentAndPageServed() throws Exception {
        Result route = route(contactRequest());
        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
        assertThat(status(route), is(OK));
    }

    private FakeRequest contactLandingRequest() {
        return fakeRequest(GET, "/contact");
    }

    private FakeRequest contactRequest() {
        return fakeRequest(GET, "/contact-sent?email=me@test.com&message=Nice website!");
    }
}
