package controllers;

import com.google.inject.AbstractModule;
import models.ProxyRepository;
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
import static org.mockito.Mockito.when;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ValidateProxyTest extends PlayTest {

    public static final String USER_IN_USE = "in-use";
    public static final String USER_NOT_IN_USE = "not-in-use";
    @Mock ProxyRepository proxyRepo;

    @Override
    public AbstractModule getModule() throws IOException {
        when(proxyRepo.exists(any(MailAddress.class))).thenAnswer(invocationOnMock -> {
            MailAddress passedAddress = (MailAddress) invocationOnMock.getArguments()[0];
            return (USER_IN_USE + "@forward.cat").equals(passedAddress.toString());
        });

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProxyRepository.class).toInstance(proxyRepo);
            }
        };
    }

    @Test
    public void noProxyParam_sendInvalidUsername() throws Exception {
        Result route = route(request(""));
        assertThatIsInvalid(route);
    }

    @Test
    public void incorrectProxyParam_sendInvalidUsername() throws Exception {
        Result route = route(request("has space"));
        assertThatIsInvalid(route);
    }

    @Test(expected = RuntimeException.class)
    public void redisConnectionError_sendInvalidUsername() throws Exception {
        when(proxyRepo.exists(any(MailAddress.class))).thenThrow(new RuntimeException());
        Result route = route(request(USER_NOT_IN_USE));
        assertThatIsInvalid(route);
    }

    @Test
    public void usernameInUse_sendInvalidUsername() throws Exception {
        Result route = route(request(USER_IN_USE));
        assertThatIsInvalid(route);
    }

    @Test
    public void usernameFree_sendValidUsername() throws Exception {
        Result route = route(request(USER_NOT_IN_USE));
        assertThatIsValid(route);
    }

    private FakeRequest request(String proxy) {
        if (proxy == null) {
            return fakeRequest(GET, "/validate");
        }
        return fakeRequest(GET, "/validate?proxy=" + proxy);
    }

    private void assertThatIsValid(Result route) {
        assertThat(status(route), is(OK));
        assertThat(contentAsString(route), is("true"));
    }

    private void assertThatIsInvalid(Result route) {
        assertThat(status(route), is(OK));
        assertThat(contentAsString(route), is("false"));
    }
}
