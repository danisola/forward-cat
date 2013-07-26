package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

abstract class AbstractController extends Controller {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractController.class.getName());

    /**
     * Returns the {@link ProxyMail} linked to the given proxy key or null
     * if it does not exist
     */
    protected ProxyMail getProxy(String proxyKey, JedisPool jedisPool, ObjectMapper mapper) {
        ProxyMail proxy = null;
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            // Getting the proxy as a string
            String proxyString = jedis.get(proxyKey);
            if (proxyString == null) {
                logger.debug("Proxy % doesn't exist", proxyString);
                return null;
            }

            // Checking that the hash is correct
            proxy = mapper.readValue(proxyString, ProxyMail.class);
        } catch (Exception ex) {
            logger.error("Error while connecting to Redis", ex);
            return null;
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
        return proxy;
    }
}
