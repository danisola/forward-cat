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
import views.html.confirm_deletion;
import views.html.proxy_deleted;

import java.util.Optional;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.formatInstant;
import static models.ExpirationUtils.toDateTime;
import static models.JedisHelper.returnJedisOnException;

public class DeleteProxy extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteProxy.class.getName());
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
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            LOGGER.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        MailAddress proxyMail = maybeProxyMail.get();
        ProxyMail proxy = getProxy(generateProxyKey(proxyMail), jedisPool, mapper);
        if (proxy == null) {
            LOGGER.debug("Proxy {} doesn't exist", proxy);
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
            LOGGER.debug("Proxy {} is not active", proxy);
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
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            LOGGER.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        MailAddress proxyMail = maybeProxyMail.get();
        String proxyKey = generateProxyKey(proxyMail);
        ProxyMail proxy = getProxy(proxyKey, jedisPool, mapper);
        if (proxy == null) {
            LOGGER.debug("Proxy % doesn't exist", proxyKey);
            return badRequest();
        }

        // Checking that the hash is correct
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            LOGGER.debug("Hash values are not equals %s - %s", h, hashValue);
            return badRequest();
        }

        // Checking whether the proxy is active or not
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy is not active");
            return badRequest();
        }

        // Removing the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();

            pipeline.del(proxyKey);
            pipeline.zrem(RedisKeys.ALERTS_SET, proxyMail.toString());
            pipeline.sync();
        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            return internalServerError();
        }
        jedisPool.returnResource(jedis);

        // Sending the response
        Lang lang = getBestLanguage(request, lang());
        return ok(proxy_deleted.render(lang));
    }
}
