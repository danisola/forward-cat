package alerts;


import com.forwardcat.common.ProxyMail;
import com.google.common.collect.Sets;
import models.MailSender;
import models.Options;
import models.ProxyRepository;
import org.apache.mailet.MailAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

import java.io.IOException;

import static controllers.TestUtils.activeProxy;
import static controllers.TestUtils.toMailAddress;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendAlertJobTest {

    SendAlertJob job;

    MailAddress proxyMail = toMailAddress("test@forward.cat");
    ProxyMail proxy = activeProxy(proxyMail, false);

    @Mock MailSender mailSender;
    @Mock JobExecutionContext ctx;
    @Mock Options opts;
    @Mock ProxyRepository proxyRepo;


    @Before
    public void setUp() throws IOException {
        when(opts.getTimeBetweenAlertMailsMillis()).thenReturn(1);

        job = new SendAlertJob(proxyRepo, mailSender, opts);
    }

    @Test
    public void noAlerts_doNothing() throws Exception {
        job.execute(ctx);

        verify(mailSender, never()).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
        verify(proxyRepo, never()).update(any(ProxyMail.class));
    }

    @Test
    public void someAlerts_sendMailsAndRemoveThem() throws Exception {
        when(proxyRepo.getExpiringProxies()).thenReturn(Sets.newHashSet(proxy));
        job.execute(ctx);

        verify(mailSender, times(1)).sendHtmlMail(any(MailAddress.class), anyString(), anyString());
    }
}

