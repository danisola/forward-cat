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

    private static final String PROXY_PARAM = "proxy";
    private static final String MAIL_PARAM = "email";
    private static final String DURATION_PARAM = "duration";
    private final JedisPool jedisPool;
    private final ObjectMapper mapper;
    private final MailSender mailSender;

    @Inject
    public AddProxy(JedisPool jedisPool, ObjectMapper mapper, MailSender mailSender) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
        this.mailSender = mailSender;
    }

    public Result addProxy() {
        Http.Request request = request();

        String proxyUserName = request.getQueryString(PROXY_PARAM);
        MailAddress userMail = toMailAddress(request.getQueryString(MAIL_PARAM));
        String durationParam = request.getQueryString(DURATION_PARAM);
        if (proxyUserName == null || userMail == null || durationParam == null || !isNumeric(durationParam)) {
            return badRequest();
        }

        int duration = Integer.parseInt(durationParam);
        if (!isValidDuration(duration)) {
            return badRequest();
        }

        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest();
        }

        MailAddress proxyMailAddress = getMailAddress(proxyUserName);
        if (proxyMailAddress == null) {
            return badRequest();
        }

        DateTime creationTime = new DateTime();
        String creationTimeStr = toStringValue(creationTime);
        String expirationTimeStr = toStringValue(creationTime.plusDays(duration));
        Lang lang = getBestLanguage(request, lang());
        ProxyMail proxy = new ProxyMail(userMail.toString(), creationTimeStr, expirationTimeStr, lang.code());

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

            String jsonValue = mapper.writeValueAsString(proxy);

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
        Html content = proxy_created_email.render(lang, proxyMailAddress.toString(), getHash(proxy));
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
