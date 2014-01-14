package alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.Inject;
import models.MailSender;
import models.Options;
import org.apache.mailet.MailAddress;
import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.templates.Html;
import play.i18n.Lang;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import views.html.proxy_expiring_email;

import java.util.Set;

import static models.ControllerUtils.getHash;
import static models.ControllerUtils.toMailAddress;
import static models.ExpirationUtils.formatInstant;
import static models.ExpirationUtils.toDateTime;

/**
 * Job that sends alerts to the users of proxies before they expire in order to
 * allow them to extend its life.
 */
public class SendAlertJob implements Job {

    private final String subject = "Forward Cat";
    private final JedisPool jedisPool;
    private final ObjectMapper mapper;
    private final MailSender mailSender;
    private final Options options;

    @Inject
    SendAlertJob(JedisPool jedisPool, ObjectMapper mapper, MailSender mailSender,
                 Options options) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
        this.mailSender = mailSender;
        this.options = options;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Getting the proxies that will expire before tomorrow
        Jedis scanJedis = jedisPool.getResource();
        Set<String> alerts;
        try {
            // Getting the alerts that should be sent now
            long currentMillis = System.currentTimeMillis();
            alerts = scanJedis.zrangeByScore(RedisKeys.ALERTS_SET, 0L, currentMillis);
        } catch (JedisException ex) {
            logger.error("Unexpected exception", ex);
            return;
        } finally {
            jedisPool.returnResource(scanJedis);
        }

        // Sending alerts to those proxy users and deleting the alerts
        for (String alertProxy : alerts) {
            Jedis jedis = jedisPool.getResource();
            try {
                // Getting the proxy
                String proxyKey = RedisKeys.generateProxyKey(toMailAddress(alertProxy));
                String proxyValue = jedis.get(proxyKey);

                if (proxyValue != null) {
                    ProxyMail proxyMail = mapper.readValue(proxyValue, ProxyMail.class);
                    DateTime expirationTime = toDateTime(proxyMail.getExpirationTime());

                    // Sending the mail
                    Lang lang = new Lang(Lang.get(proxyMail.getLang()).get());
                    MailAddress address = toMailAddress(proxyMail.getUserAddress());

                    String date = formatInstant(expirationTime, lang);

                    Html content = proxy_expiring_email.render(lang, alertProxy, date, getHash(proxyMail));
                    mailSender.sendHtmlMail(address, subject, content.toString());
                } else {
                    logger.warn("Proxy had already expired: " + proxyKey);
                }

                // Removing the alert
                jedis.zrem(RedisKeys.ALERTS_SET, alertProxy);
            } catch (Exception ex) {
                logger.error("Unexpected exception for proxy: " + alertProxy, ex);
            } finally {
                jedisPool.returnResource(jedis);
            }

            // Wait before sending another email
            try {
                Thread.sleep(options.getTimeBetweenAlertMailsMillis());
            } catch (InterruptedException e) {
                logger.error("Unexpected error", e);
            }
        }
    }

    protected static final Logger logger = LoggerFactory.getLogger(SendAlertJob.class.getName());
}
