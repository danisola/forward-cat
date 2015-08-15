package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.AbstractModule;
import models.Repository;
import models.StatsRepository;
import org.apache.mailet.MailAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;

import javax.mail.internet.AddressException;
import java.io.IOException;

import static controllers.TestUtils.*;
import static models.ControllerUtils.getHash;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmProxyTest extends PlayTest {

    MailAddress proxyMail = toMailAddress("test@forward.cat");
    ProxyMail proxy = inactiveProxy(proxyMail);
    String proxyHash = getHash(proxy);

    @Mock Repository repository;
    @Mock StatsRepository statsRepo;

    @Override
    public AbstractModule getModule() throws IOException, AddressException {
        whenAddressReturnProxy(repository, proxyMail, proxy);

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class).toInstance(repository);
                bind(StatsRepository.class).toInstance(statsRepo);
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

    @Test
    public void everythingFine_sendConfirmationPage() throws Exception {
        Result route = route(request(proxyMail.toString(), proxyHash));
        assertThat(status(route), is(OK));
        verify(repository).save(proxy);
        verify(statsRepo).incrementCounter(RedisKeys.PROXIES_ACTIVATED_COUNTER);
    }

    private FakeRequest request(String proxy, String hash) {
        if (proxy == null) {
            return fakeRequest(GET, "/confirm?h=" + hash);
        } else if (hash == null) {
            return fakeRequest(GET, "/confirm?p=" + proxy);
        }
        return fakeRequest(GET, "/confirm?p=" + proxy + "&h=" + hash);
    }
}
