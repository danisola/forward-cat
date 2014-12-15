package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import org.apache.mailet.MailAddress;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import views.html.proxy_created_email;

import java.util.Optional;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;
import static models.JedisHelper.returnJedisOnException;

public class AddProxy extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddProxy.class.getName());
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
        Optional<MailAddress> maybeUserMail = toMailAddress(email);
        if (proxy == null || !maybeUserMail.isPresent() || !isValidDuration(duration)) {
            return badRequest();
        }

        MailAddress userMail = maybeUserMail.get();
        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest();
        }

        Optional<MailAddress> proxyMailAddress = getMailAddress(proxy);
        if (!proxyMailAddress.isPresent()) {
            return badRequest();
        }

        DateTime creationTime = new DateTime();
        String creationTimeStr = toStringValue(creationTime);
        String expirationTimeStr = toStringValue(creationTime.plusDays(duration));
        Lang lang = getBestLanguage(request, lang());
        ProxyMail proxyMail = new ProxyMail(userMail.toString(), creationTimeStr, expirationTimeStr, lang.code());
        Boolean proxyAlreadyExists;

        // Creating the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String proxyKey = generateProxyKey(proxyMailAddress.get());
            proxyAlreadyExists = jedis.exists(proxyKey);
            if (!proxyAlreadyExists) {
                String jsonValue = mapper.writeValueAsString(proxyMail);

                Pipeline pipeline = jedis.pipelined();
                pipeline.set(proxyKey, jsonValue);
                pipeline.expire(proxyKey, getUnconfirmedProxyDuration());
                pipeline.sync();
            }
        } catch (Exception ex) {
            returnJedisOnException(jedisPool, jedis, ex);
            LOGGER.error("Error while connecting to Redis", ex);
            return internalServerError();
        }
        jedisPool.returnResource(jedis);

        if (proxyAlreadyExists) {
            return badRequest(); // Proxy already exists: cannot create a new one
        }

        // Sending the confirmation mail
        String subject = "Forward Cat";
        Html content = proxy_created_email.render(lang, proxyMailAddress.toString(), getHash(proxyMail));
        mailSender.sendHtmlMail(userMail, subject, content.toString());

        return ok("true");
    }
}
