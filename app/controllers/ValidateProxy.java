package controllers;

import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getMailAddress;

public class ValidateProxy extends Controller {

    protected static final Logger logger = LoggerFactory.getLogger(ValidateProxy.class.getName());
    private final JedisPool jedisPool;

    @Inject
    public ValidateProxy(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Result validate(String proxy) {
        Http.Request request = request();

        // Checking params
        MailAddress mailAddress = getMailAddress(proxy);
        if (mailAddress == null) {
            logger.debug("Wrong params: {}", request);
            return badRequest();
        }

        Boolean valid = false;

        // Creating the proxy
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            // Checking whether the proxy exists
            String proxyKey = generateProxyKey(mailAddress);
            valid = !jedis.exists(proxyKey);
        } catch (Exception ex) {
            logger.error("Error while connecting to Redis", ex);
            return internalServerError();
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }

        return ok(valid.toString());
    }
}
