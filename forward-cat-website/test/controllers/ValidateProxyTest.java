package controllers;

import com.google.inject.AbstractModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getMailAddress;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ValidateProxyTest extends PlayTest {

    public static final String USER_IN_USE = "in-use";
    public static final String USER_NOT_IN_USE = "not-in-use";
    @Mock JedisPool jedisPool;
    @Mock Jedis jedis;

    @Override
    public AbstractModule getModule() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);

        String inUseKey = generateProxyKey(getMailAddress(USER_IN_USE, "forward.cat").get());
        when(jedis.exists(inUseKey)).thenReturn(true);

        String notInUseKey = generateProxyKey(getMailAddress(USER_NOT_IN_USE, "forward.cat").get());
        when(jedis.exists(notInUseKey)).thenReturn(false);

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(JedisPool.class).toInstance(jedisPool);
            }
        };
    }

    @Test
    public void noProxyParam_sendBadRequest() throws Exception {
        Result route = route(request(null));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void incorrectProxyParam_sendBadRequest() throws Exception {
        Result route = route(request("has space"));
        assertThat(status(route), is(BAD_REQUEST));
    }

    @Test
    public void redisConnectionError_sendFalse() throws Exception {
        doThrow(JedisException.class).when(jedis).exists(anyString());
        Result route = route(request(USER_NOT_IN_USE));
        assertThat(status(route), is(OK));
        assertThat(contentAsString(route), is("false"));
    }

    @Test
    public void userNameInUse_sendFalse() throws Exception {
        Result route = route(request(USER_IN_USE));
        assertThat(status(route), is(OK));
        assertThat(contentAsString(route), is("false"));
    }

    @Test
    public void userNameFree_sendTrue() throws Exception {
        Result route = route(request(USER_NOT_IN_USE));
        assertThat(status(route), is(OK));
        assertThat(contentAsString(route), is("true"));
    }

    private FakeRequest request(String proxy) {
        if (proxy == null) {
            return fakeRequest(GET, "/validate");
        }
        return fakeRequest(GET, "/validate?proxy=" + proxy);
    }
}
