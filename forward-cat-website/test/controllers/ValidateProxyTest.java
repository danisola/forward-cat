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
    public void noProxyParam_sendInvalidUsername() throws Exception {
        Result route = route(request(""));
        assertThatIsInvalid(route);
    }

    @Test
    public void incorrectProxyParam_sendInvalidUsername() throws Exception {
        Result route = route(request("has space"));
        assertThatIsInvalid(route);
    }

    @Test
    public void redisConnectionError_sendInvalidUsername() throws Exception {
        doThrow(JedisException.class).when(jedis).exists(anyString());
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
