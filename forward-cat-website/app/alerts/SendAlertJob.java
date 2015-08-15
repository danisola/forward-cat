package alerts;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import models.Options;
import models.Repository;
import org.apache.mailet.MailAddress;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.twirl.api.Html;
import views.html.proxy_expiring_email;

import java.util.Date;
import java.util.Set;

import static models.ControllerUtils.getHash;
import static models.ControllerUtils.toMailAddress;
import static models.ExpirationUtils.formatInstant;

/**
 * Job that sends alerts to the users of proxies before they expire in order to
 * allow them to extend its life.
 */
public class SendAlertJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendAlertJob.class.getName());
    private final String subject = "Forward Cat";
    private final MailSender mailSender;
    private final Options options;
    private final Repository repository;

    @Inject
    SendAlertJob(Repository repository, MailSender mailSender,
                 Options options) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.options = options;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Getting the proxies that will expire before tomorrow
        Set<ProxyMail> expiringProxies = repository.getExpiringProxies();

        sendAlerts(expiringProxies);
        repository.removeExpiredProxies();
        repository.removeNonActivatedProxies();
        repository.removeUsersWithoutProxies();
    }

    /**
     * Send email alerts to the given proxy users and mark is at notified
     *
     * @param proxies
     */
    private void sendAlerts(Set<ProxyMail> proxies) {
        for (ProxyMail proxy : proxies) {
            try {
                // Sending the mail
                Lang lang = new Lang(Lang.get(proxy.getLang()).get());
                MailAddress address = toMailAddress(proxy.getUser().getEmailAddress()).get();

                Date expirationTime = proxy.getExpirationTime();
                String date = formatInstant(expirationTime, lang);

                Html content = proxy_expiring_email.render(lang, proxy.getProxyAddress(), date, getHash(proxy));
                mailSender.sendHtmlMail(address, subject, content.toString());

                proxy.setExpirationNotified(true);
                repository.update(proxy);
            } catch (Exception ex) {
                LOGGER.error("Unexpected exception sending expiration notification for proxy: " + proxy, ex);
            }

            // Wait before sending another email
            try {
                Thread.sleep(options.getTimeBetweenAlertMailsMillis());
            } catch (InterruptedException e) {
                LOGGER.error("Unexpected error", e);
            }
        }
    }
}
