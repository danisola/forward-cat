package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import org.joda.time.DateTime;
import play.i18n.Lang;
import play.mvc.Http;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import views.html.proxy_extended;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getBestLanguage;
import static models.ControllerUtils.getHash;
import static models.ControllerUtils.toMailAddress;
import static models.ExpirationUtils.*;

public class ExtendProxy extends AbstractController {

    private final JedisPool jedisPool;
    private final ObjectMapper mapper;

    @Inject
    ExtendProxy(JedisPool jedisPool, ObjectMapper mapper) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
    }

    public Result extend() {
        Http.Request request = request();

        // Checking params
        MailAddress proxyMail = toMailAddress(request.getQueryString(PROXY_PARAM));
        String hashParam = request.getQueryString(HASH_PARAM);
        if (proxyMail == null || hashParam == null) {
            logger.debug("Wrong params: {}", request);
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
        if (!hashParam.equals(hashValue)) {
            logger.debug("Hash values are not equals {} - {}", hashParam, hashValue);
            return badRequest();
        }

        // Checking that the proxy is active
        if (!proxy.isActive()) {
            logger.debug("Proxy {} is already active", proxy);
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
            Response<Long> expireResponse = pipeline.expire(proxyKey, secondsTo(newExpirationTime)); // Setting the TTL

            // Adding or overwriting the alert if the time has not passed
            DateTime newAlertTime = getAlertTime(newExpirationTime);
            if (newExpirationTime.isAfterNow()) {
                pipeline.zadd(RedisKeys.ALERTS_SET, newAlertTime.getMillis(), proxyMail.toString());
            }

            pipeline.sync();

            if (expireResponse.get() != 1L) {
                // Very unlikely, but something has gone wrong
                logger.error("Error while expiring {}", proxyKey);
                return badRequest();
            }
        } catch (Exception ex) {
            logger.error("Error while connecting to Redis", ex);
            return internalServerError();
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }

        // Generating the answer
        Lang language = getBestLanguage(request, lang());
        String date = formatInstant(newExpirationTime, language);
        return ok(proxy_extended.render(language, proxyMail.toString(), date));
    }
}
