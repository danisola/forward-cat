package controllers;

import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getMailAddress;
import static models.JedisHelper.returnJedisOnException;

public class ValidateProxy extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateProxy.class.getName());
    private final JedisPool jedisPool;

    @Inject
    ValidateProxy(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Result validate(String proxy) {
        // Checking params
        Optional<MailAddress> mailAddress = getMailAddress(proxy);
        if (!mailAddress.isPresent()) {
            return badRequest();
        }

        Boolean valid;

        // Creating the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            // Checking whether the proxy exists
            String proxyKey = generateProxyKey(mailAddress.get());
            valid = !jedis.exists(proxyKey);
        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            return internalServerError();
        }
        jedisPool.returnResource(jedis);

        return ok(valid.toString());
    }
}
