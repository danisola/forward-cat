package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.mvc.Http;
import play.mvc.Result;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import views.html.proxy_extended;

import java.util.Optional;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class ExtendProxy extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendProxy.class.getName());
    private final ObjectMapper mapper;

    @Inject
    ExtendProxy(JedisPool jedisPool, ObjectMapper mapper) {
        super(jedisPool);
        this.mapper = mapper;
    }

    public Result extend(String p, String h) {
        Http.Request request = request();

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest();
        }

        // Getting the proxy
        MailAddress proxyMail = maybeProxyMail.get();
        String proxyKey = generateProxyKey(proxyMail);
        Optional<ProxyMail> maybeProxy = getProxy(proxyKey, mapper);
        if (!maybeProxy.isPresent()) {
            return badRequest();
        }

        // Checking that the hash is correct
        ProxyMail proxy = maybeProxy.get();
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            LOGGER.debug("Hash values are not equals {} - {}", h, hashValue);
            return badRequest();
        }

        // Checking that the proxy is active
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy {} is already active", proxy);
            return badRequest();
        }

        DateTime creationTime = toDateTime(proxy.getCreationTime());
        DateTime expirationTime = toDateTime(proxy.getExpirationTime());

        // Checking that the proxy has not been active for more than 15 days
        DateTime maxExpirationTime = creationTime.plusDays(getMaxProxyDuration());
        DateTime newExpirationTime = getNewExpirationTime(expirationTime.plusDays(getIncrementDaysAdded()), maxExpirationTime);
        proxy.setExpirationTime(toStringValue(newExpirationTime));

        dbStatement(jedis -> {
            Pipeline pipeline = jedis.pipelined();

            pipeline.set(proxyKey, mapper.writeValueAsString(proxy)); // Saving the proxy
            pipeline.expire(proxyKey, secondsTo(newExpirationTime)); // Setting the TTL

            // Adding or overwriting the alert if the time has not passed
            DateTime newAlertTime = getAlertTime(newExpirationTime);
            if (newExpirationTime.isAfterNow()) {
                pipeline.zadd(RedisKeys.ALERTS_SET, newAlertTime.getMillis(), proxyMail.toString());
            }

            pipeline.sync();
        });

        // Generating the answer
        Lang language = getBestLanguage(request, lang());
        String date = formatInstant(newExpirationTime, language);
        return ok(proxy_extended.render(language, proxyMail.toString(), date));
    }

    private DateTime getNewExpirationTime(DateTime newExpirationTime, DateTime maxExpirationTime) {
        if (newExpirationTime.isAfter(maxExpirationTime)) {
            return maxExpirationTime;
        }
        return newExpirationTime;
    }
}
