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
import views.html.confirm_deletion;
import views.html.proxy_deleted;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getBestLanguage;
import static models.ControllerUtils.getHash;
import static models.ControllerUtils.toMailAddress;
import static models.ExpirationUtils.formatInstant;
import static models.ExpirationUtils.toDateTime;

public class DeleteProxy extends AbstractController {

    private final JedisPool jedisPool;
    private final ObjectMapper mapper;

    @Inject
    public DeleteProxy(JedisPool jedisPool, ObjectMapper mapper) {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
    }

    public Result confirmDeletion(String p, String h) {
        Http.Request request = request();

        // Checking params
        MailAddress proxyMail = toMailAddress(p);
        if (proxyMail == null || h == null) {
            logger.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        ProxyMail proxy = getProxy(generateProxyKey(proxyMail), jedisPool, mapper);
        if (proxy == null) {
            logger.debug("Proxy {} doesn't exist", proxy);
            return badRequest();
        }

        // Checking that the hash is correct
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            logger.debug("Hash values are not equals {} - {}", h, hashValue);
            return badRequest();
        }

        // Checking that the proxy is active
        if (!proxy.isActive()) {
            logger.debug("Proxy {} is not active", proxy);
            return badRequest();
        }

        // Generating the answer
        DateTime expirationTime = toDateTime(proxy.getExpirationTime());
        Lang lang = getBestLanguage(request, lang());
        String date = formatInstant(expirationTime, lang);
        return ok(confirm_deletion.render(lang, proxyMail.toString(), date, hashValue));
    }

    public Result delete(String p, String h) {
        Http.Request request = request();

        // Checking params
        MailAddress proxyMail = toMailAddress(p);
        if (proxyMail == null || h == null) {
            logger.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        String proxyKey = generateProxyKey(proxyMail);
        ProxyMail proxy = getProxy(proxyKey, jedisPool, mapper);
        if (proxy == null) {
            logger.debug("Proxy % doesn't exist", proxyKey);
            return badRequest();
        }

        // Checking that the hash is correct
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            logger.debug("Hash values are not equals %s - %s", h, hashValue);
            return badRequest();
        }

        // Checking whether the proxy is active or not
        if (!proxy.isActive()) {
            logger.debug("Proxy is not active");
            return badRequest();
        }

        // Removing the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();

            Response<Long> del = pipeline.del(proxyKey);
            Response<Long> alertDel = pipeline.zrem(RedisKeys.ALERTS_SET, proxyMail.toString());

            pipeline.sync();

            if (del.get() != 1L || alertDel.get() != 1L) {
                logger.error("Proxy couldn't be removed");
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

        // Sending the response
        Lang lang = getBestLanguage(request, lang());
        return ok(proxy_deleted.render(lang));
    }
}
