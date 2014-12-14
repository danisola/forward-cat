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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ReportTest extends PlayTest {

    String nonActiveProxy = "nonActive@forward.cat";
    String nonActive = "{\"ua\":\"my_address@mail.com\",\"ts\":\"2013-01-27T01:58:53.874+01:00\",\"ex\":\"2013-02-01T01:58:53.874+01:00\",\"ac\":false}";

    String blockedProxy = "blocked@forward.cat";
    String blocked = "{\"ua\":\"my_address@mail.com\",\"ts\":\"2013-01-27T01:58:53.874+01:00\",\"ex\":\"2013-02-01T01:58:53.874+01:00\",\"ac\":false,\"bl\":true}";

    String spammerProxy = "spammer@forward.cat";
    String spammer = "{\"ua\":\"my_address@mail.com\",\"ts\":\"2013-01-27T01:58:53.874+01:00\",\"ex\":\"2013-02-01T01:58:53.874+01:00\",\"ac\":true,\"bl\":false}";

    @Mock JedisPool jedisPool = mock(JedisPool.class);
    @Mock Jedis jedis;
    @Mock Pipeline pipeline;
    @Mock MailSender mailSender;
    @Mock Response<Long> expireResponse;

    @Override
    public AbstractModule getModule() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("p:" + nonActiveProxy)).thenReturn(nonActive);
        when(jedis.get("p:" + blockedProxy)).thenReturn(blocked);
        when(jedis.get("p:" + spammerProxy)).thenReturn(spammer);
        when(jedis.pipelined()).thenReturn(pipeline);
        when(pipeline.expire(anyString(), anyInt())).thenReturn(expireResponse);
        when(expireResponse.get()).thenReturn(1L);

        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(JedisPool.class).toInstance(jedisPool);
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
        Result route = route(request(nonActiveProxy));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void blockedProxy_dontSendEmail() throws Exception {
        Result route = route(request(blockedProxy));
        verifyZeroInteractions(mailSender);
        assertThat(status(route), is(OK));
    }

    @Test
    public void regularProxy_sendEmail() throws Exception {
        Result route = route(request(spammerProxy));
        assertThat(status(route), is(OK));
        verify(mailSender).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }

    private FakeRequest request(String proxy) {
        return fakeRequest(GET, "/report-user?proxy=" + proxy + "&message=hey%20there");
    }
}
