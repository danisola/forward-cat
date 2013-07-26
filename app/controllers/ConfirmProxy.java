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
import views.html.proxy_created;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class ConfirmProxy extends AbstractController {

    private final JedisPool jedisPool;
    private final ObjectMapper mapper;

    @Inject
    ConfirmProxy(JedisPool jedisPool, ObjectMapper mapper) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
    }

    public Result confirm() throws Exception {
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
            logger.debug("Hash values are not equals %s - %s", hashParam, hashValue);
            return badRequest();
        }

        // Checking that the proxy is not already active
        if (proxy.isActive()) {
            logger.debug("Proxy {} is already active", proxy);
            return badRequest();
        }
        proxy.activate();

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();

            // Calculating the TTL of the proxy
            DateTime expirationTime = toDateTime(proxy.getExpirationTime());
            DateTime alertTime = getAlertTime(expirationTime);

            pipeline.set(proxyKey, mapper.writeValueAsString(proxy)); // Saving the proxy
            Response<Long> expireResponse = pipeline.expire(proxyKey, secondsTo(expirationTime)); // Setting TTL
            pipeline.zadd(RedisKeys.ALERTS_SET, alertTime.getMillis(), proxyMail.toString()); // Adding an alert
            pipeline.incr(RedisKeys.PROXIES_ACTIVATED_COUNTER); // Incrementing proxies activated
            pipeline.sync();

            if (expireResponse.get() != 1L) {
                // Very unlikely, but something has gone wrong
                logger.error("Error while expiring %s", proxyKey);
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

        // Generating the response
        DateTime expirationTime = toDateTime(proxy.getExpirationTime());
        Lang language = getBestLanguage(request, lang());
        String date = formatInstant(expirationTime, language);
        return ok(proxy_created.render(language, proxyMail.toString(), date));
    }
}
