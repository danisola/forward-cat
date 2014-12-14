package com.forwardcat.james;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.james.core.MailImpl;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ForwardMailetTest {

    ForwardMailet mailet = new ForwardMailet();

    @Captor ArgumentCaptor<Mail> mailCaptor;
    @Mock ForwardCatResourcesProvider resourcesProvider;
    @Mock JedisPool pool;
    @Mock Jedis jedis;
    @Mock MailetContext context;

    ObjectMapper mapper = new ObjectMapper();
    String prefix = "[Forward.cat] ";
    MailAddress recipient;

    @Before
    public void setUp() throws MessagingException, UnknownHostException {
        when(pool.getResource()).thenReturn(jedis);
        when(resourcesProvider.getPool()).thenReturn(pool);
        when(resourcesProvider.getMapper()).thenReturn(mapper);

        DNSService dns = mock(DNSService.class);
        when(dns.getLocalHost()).thenReturn(InetAddress.getLocalHost());

        mailet.setDNSService(dns);
        mailet.setResourceProvider(resourcesProvider);

        recipient = new MailAddress("recipient@forward.cat");
        MailetConfig config = mock(MailetConfig.class);
        when(config.getInitParameter("sender")).thenReturn("no-reply@forward.cat");
        when(config.getInitParameter("prefix")).thenReturn(prefix);
        when(config.getInitParameter("static")).thenReturn("true");
        when(config.getMailetContext()).thenReturn(context);
        mailet.init(config);
    }

    @Test
    public void proxyNotFound_shouldBounceMail() throws MessagingException {
        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertHasBounceAttr(mail);
    }

    @Test
    public void proxyNotWellFormed_shouldBounceMail() throws MessagingException {
        when(jedis.get(generateProxyKey(recipient))).thenReturn("not a valid proxy");

        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertHasBounceAttr(mail);
    }

    @Test
    public void proxyNotActive_shouldBounceMail() throws MessagingException {
        String proxy = "{\"ua\":\"someone@gmail.com\",\"ts\":\"2013-02-04T20:55:19.625Z\",\"ex\":\"2013-02-09T20:55:19.625Z\",\"ac\":false}";
        when(jedis.get(generateProxyKey(recipient))).thenReturn(proxy);

        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertHasBounceAttr(mail);
    }

    @Test
    public void proxyActive_shouldForwardMail() throws MessagingException {
        String proxy = "{\"ua\":\"someone@gmail.com\",\"ts\":\"2013-02-04T20:55:19.625Z\",\"ex\":\"2013-02-09T20:55:19.625Z\",\"ac\":true}";
        when(jedis.get(generateProxyKey(recipient))).thenReturn(proxy);

        Mail mail = newMail();
        mailet.service(mail);

        assertThat(mail.getState(), is(Mail.GHOST));
        verify(pool, atLeastOnce()).returnResource(jedis);
        verify(context).sendMail(mailCaptor.capture());

        Mail forwardMail = mailCaptor.getValue();
        assertThat(forwardMail, notNullValue());
    }

    private Mail newMail() throws MessagingException {
        MailImpl mail = new MailImpl();
        mail.setName("name");

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(System.getProperties()));
        message.setSubject("Some subject");
        mail.setMessage(message);
        mail.setRecipients(Lists.newArrayList(recipient));
        return mail;
    }

    private void assertHasBounceAttr(Mail mail) {
        Serializable attr = mail.getAttribute(ForwardMailet.BOUNCE_ATTRIBUTE);
        assertNotNull(attr);
        assertThat((String) attr, is("true"));
    }
}
