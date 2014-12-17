package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.google.inject.AbstractModule;
import models.MailSender;
import org.apache.mailet.MailAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.FakeRequest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

import static models.ControllerUtils.getHash;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class AddProxyTest extends PlayTest {

    String proxyMail = "test@forward.cat";
    String proxyData = "{\"ua\":\"my_address@mail.com\",\"ts\":\"2013-01-27T01:58:53.874+01:00\",\"ex\":\"2013-02-01T01:58:53.874+01:00\",\"ac\":false}";
    String proxyHash;

    @Mock JedisPool jedisPool = mock(JedisPool.class);
    @Mock Jedis jedis;
    @Mock Pipeline pipeline;
    @Mock MailSender mailSender;
    @Mock Response<Long> expireResponse;

    @Override
    public AbstractModule getModule() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("p:" + proxyMail)).thenReturn(proxyData);
        when(jedis.pipelined()).thenReturn(pipeline);
        when(pipeline.expire(anyString(), anyInt())).thenReturn(expireResponse);
        when(expireResponse.get()).thenReturn(1L);

        ProxyMail proxy = new ObjectMapper().readValue(proxyData, ProxyMail.class);
        proxyHash = getHash(proxy);

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(JedisPool.class).toInstance(jedisPool);
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
        when(jedis.exists("p:test@forward.cat")).thenReturn(Boolean.TRUE);

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(BAD_REQUEST));
        verify(jedisPool).returnResource(jedis);
    }

    @Test
    public void connectionError_sendBadRequest() throws Exception {
        when(jedis.exists("p:test@forward.cat")).thenThrow(JedisException.class);

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(BAD_REQUEST));
        verify(jedisPool).returnResource(jedis);
    }

    @Test
    public void everythingFine_sendMailAndGoodResponse() throws Exception {
        when(jedis.exists("p:test@forward.cat")).thenReturn(Boolean.FALSE);

        Result route = route(request("test", "user@mail.com", 3));
        assertThat(status(route), is(OK));
        verify(jedisPool).returnResource(jedis);

        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
        verify(jedisPool).returnResource(jedis);
    }

    private FakeRequest request(String proxy, String email, Integer duration) {
        return fakeRequest(GET, "/add?proxy=" + proxy + "&email=" + email + "&duration=" + duration);
    }
}
