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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import views.html.proxy_extended;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class ExtendProxy extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendProxy.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper mapper;

    @Inject
    ExtendProxy(JedisPool jedisPool, ObjectMapper mapper) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
    }

    public Result extend(String p, String h) {
        Http.Request request = request();

        // Checking params
        MailAddress proxyMail = toMailAddress(p);
        if (proxyMail == null || h == null) {
            LOGGER.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Getting the proxy
        String proxyKey = generateProxyKey(proxyMail);
        ProxyMail proxy = getProxy(proxyKey, jedisPool, mapper);
        if (proxy == null) {
            return badRequest();
        }

        // Checking that the hash is correct
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
        DateTime newExpirationTime = expirationTime.plusDays(getIncrementDaysAdded());
        DateTime maxExpirationTime = creationTime.plusDays(getMaxProxyDuration());
        if (newExpirationTime.isAfter(maxExpirationTime)) {
            newExpirationTime = maxExpirationTime;
        }
        proxy.setExpirationTime(toStringValue(newExpirationTime));

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();

            pipeline.set(proxyKey, mapper.writeValueAsString(proxy)); // Saving the proxy
            pipeline.expire(proxyKey, secondsTo(newExpirationTime)); // Setting the TTL

            // Adding or overwriting the alert if the time has not passed
            DateTime newAlertTime = getAlertTime(newExpirationTime);
            if (newExpirationTime.isAfterNow()) {
                pipeline.zadd(RedisKeys.ALERTS_SET, newAlertTime.getMillis(), proxyMail.toString());
            }

            pipeline.sync();
        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            return internalServerError();
        }
        jedisPool.returnResource(jedis);

        // Generating the answer
        Lang language = getBestLanguage(request, lang());
        String date = formatInstant(newExpirationTime, language);
        return ok(proxy_extended.render(language, proxyMail.toString(), date));
    }
}
