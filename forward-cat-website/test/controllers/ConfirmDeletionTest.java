package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.AbstractModule;
import models.Repository;
import org.apache.mailet.MailAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;

import static controllers.TestUtils.*;
import static models.ControllerUtils.getHash;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmDeletionTest extends PlayTest {

    MailAddress proxyMail = toMailAddress("test@forward.cat");
    ProxyMail proxy = activeProxy(proxyMail, false);
    String proxyHash = getHash(proxy);

    @Mock Repository repository;

    @Override
    public AbstractModule getModule() throws IOException {
        whenAddressReturnProxy(repository, proxyMail, proxy);

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class).toInstance(repository);
            }
        };
    }

    @Test
    public void proxyMailMissing_sendBadRequest() throws Exception {
        Result route = route(request(null, proxyHash));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void hashMissing_sendBadRequest() throws Exception {
        Result route = route(request(proxyMail.toString(), null));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void proxyNonExisting_sendBadRequest() throws Exception {
        Result route = route(request("nonValid@forward.cat", proxyHash));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void invalidHash_sendBadRequest() throws Exception {
        Result route = route(request(proxyMail.toString(), "wrongHash"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test(expected = RuntimeException.class)
    public void redisException_sendBadRequest() throws Exception {
        when(repository.getProxy(proxyMail)).thenThrow(new RuntimeException());
        Result route = route(request(proxyMail.toString(), proxyHash));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void everythingFine_sendConfirmationPage() throws Exception {
        Result route = route(request(proxyMail.toString(), proxyHash));
        assertThat(status(route), is(OK));
    }

    private FakeRequest request(String proxy, String hash) {
        if (proxy == null) {
            return fakeRequest(GET, "/confirm-deletion?h=" + hash);
        } else if (hash == null) {
            return fakeRequest(GET, "/confirm-deletion?p=" + proxy);
        }
        return fakeRequest(GET, "/confirm-deletion?p=" + proxy + "&h=" + hash);
    }
}
