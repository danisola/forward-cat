package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import org.apache.mailet.MailAddress;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.templates.Html;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import views.html.proxy_created_email;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class AddProxy extends Controller {

    protected static final Logger logger = LoggerFactory.getLogger(AddProxy.class.getName());

    private final JedisPool jedisPool;
    private final ObjectMapper mapper;
    private final MailSender mailSender;

    @Inject
    public AddProxy(JedisPool jedisPool, ObjectMapper mapper, MailSender mailSender) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
        this.mailSender = mailSender;
    }

    public Result addProxy(String proxy, String email, int duration) {
        Http.Request request = request();

        // Checking params
        MailAddress userMail = toMailAddress(email);
        if (proxy == null || userMail == null || !isValidDuration(duration)) {
            return badRequest();
        }

        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest();
        }

        MailAddress proxyMailAddress = getMailAddress(proxy);
        if (proxyMailAddress == null) {
            return badRequest();
        }

        DateTime creationTime = new DateTime();
        String creationTimeStr = toStringValue(creationTime);
        String expirationTimeStr = toStringValue(creationTime.plusDays(duration));
        Lang lang = getBestLanguage(request, lang());
        ProxyMail proxyMail = new ProxyMail(userMail.toString(), creationTimeStr, expirationTimeStr, lang.code());

        // Creating the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String proxyKey = generateProxyKey(proxyMailAddress);
            Boolean exists = jedis.exists(proxyKey);
            if (exists) {
                // Proxy already exists cannot create a new one
                return badRequest();
            }

            String jsonValue = mapper.writeValueAsString(proxyMail);

            Pipeline pipeline = jedis.pipelined();
            pipeline.set(proxyKey, jsonValue);
            Response<Long> expire = pipeline.expire(proxyKey, getUnconfirmedProxyDuration());
            pipeline.sync();

            if (expire.get() != 1L) {
                // Expire couldn't be set, trying to remove
                logger.error("Expire command couldn't be executed");
                jedis.del(proxyKey);
                return internalServerError();
            }
        } catch (Exception ex) {
            logger.error("Error while connecting to Redis", ex);
            return internalServerError();
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }

        // Sending the confirmation mail
        String subject = "Forward Cat";
        Html content = proxy_created_email.render(lang, proxyMailAddress.toString(), getHash(proxyMail));
        mailSender.sendHtmlMail(userMail, subject, content.toString());

        return ok("true");
    }

    /**
     * Code based on org.apache.commons.lang.StringUtils#isNumeric
     */
    private static boolean isNumeric(String str) {
        if (str.length() == 0) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
