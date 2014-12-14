package alerts;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import models.MailSender;
import models.Options;
import org.apache.mailet.MailAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendAlertJobTest {

    SendAlertJob job;

    String proxyData = "{\"ua\":\"my_address@mail.com\",\"ts\":\"2013-01-27T01:58:53.874+01:00\",\"ex\":\"2013-02-01T01:58:53.874+01:00\",\"l\":\"en\",\"ac\":true}";

    @Mock JedisPool jedisPool;
    @Mock Jedis jedis;
    @Mock MailSender mailSender;
    @Mock JobExecutionContext ctx;
    @Mock Options opts;

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws IOException {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(anyString())).thenReturn(proxyData);
        when(opts.getTimeBetweenAlertMailsMillis()).thenReturn(1);

        job = new SendAlertJob(jedisPool, mapper, mailSender, opts);
    }

    @Test
    public void noAlerts_doNothing() throws Exception {
        when(jedis.zrangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Sets.<String>newHashSet());
        job.execute(ctx);

        verify(mailSender, never()).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }

    @Test
    public void someAlerts_sendMailsAndRemoveThem() throws Exception {
        Set<String> proxyMails = Sets.newHashSet("first@forward.cat", "second@forward.cat");
        when(jedis.zrangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(proxyMails);
        job.execute(ctx);

        verify(mailSender, times(proxyMails.size())).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }
}

