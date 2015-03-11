package com.forwardcat.james;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
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
import javax.mail.internet.MimeMultipart;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
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
        assertThatIsBounced(mail);
        assertNoMailHasBeenSent();
    }

    @Test
    public void proxyNotFoundAndSenderIgnorable_shouldIgnoreMail() throws MessagingException {
        Mail mail = newMail("do-not-reply@mail.com");
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertThatIsIgnored(mail);
        assertNoMailHasBeenSent();
    }

    @Test
    public void proxyNotFoundAndSenderIsFbUpdate_shouldIgnoreMail() throws MessagingException {
        Mail mail = newMail("notification+zj4za=zy2==c@facebookmail.com");
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertThatIsIgnored(mail);
        assertNoMailHasBeenSent();
    }

    @Test
    public void proxyNotWellFormed_shouldBounceMail() throws MessagingException {
        when(jedis.get(generateProxyKey(recipient))).thenReturn("not a valid proxy");

        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertThatIsBounced(mail);
        assertNoMailHasBeenSent();
    }

    @Test
    public void proxyNotActive_shouldBounceMail() throws MessagingException {
        String proxy = "{\"ua\":\"someone@gmail.com\",\"ts\":\"2013-02-04T20:55:19.625Z\",\"ex\":\"2013-02-09T20:55:19.625Z\",\"ac\":false}";
        when(jedis.get(generateProxyKey(recipient))).thenReturn(proxy);

        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertThatIsBounced(mail);
        assertNoMailHasBeenSent();
    }

    @Test
    public void proxyBlocked_shouldSwallowMail() throws MessagingException {
        String proxy = "{\"ua\":\"someone@gmail.com\",\"ts\":\"2013-02-04T20:55:19.625Z\",\"ex\":\"2013-02-09T20:55:19.625Z\",\"ac\":true, \"bl\":true}";
        when(jedis.get(generateProxyKey(recipient))).thenReturn(proxy);

        Mail mail = newMail();
        mailet.service(mail);

        verify(pool, atLeastOnce()).returnResource(jedis);
        assertThatIsIgnored(mail);
        assertNoMailHasBeenSent();
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
        return newMail("sender@mail.com");
    }

    private Mail newMail(String sender) throws MessagingException {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(System.getProperties()));
        message.setContent(new MimeMultipart());
        message.setSubject("Some subject");

        return new MailImpl("test email", new MailAddress(sender), Collections.singletonList(recipient), message);
    }

    private void assertThatIsBounced(Mail mail) {
        Serializable attr = mail.getAttribute(ForwardMailet.BOUNCE_ATTRIBUTE);
        assertThat(attr, is("true"));
    }

    private void assertThatIsIgnored(Mail mail) {
        String attr = mail.getState();
        assertThat(attr, is(Mail.GHOST));
    }

    private void assertNoMailHasBeenSent() {
        try {
            verify(context, never()).sendMail(any(Mail.class));
        } catch (MessagingException e) {
            Throwables.propagate(e);
        }
    }
}
