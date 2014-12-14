package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static models.JedisHelper.returnJedisIfNotNull;
import static models.JedisHelper.returnJedisOnException;

abstract class AbstractController extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractController.class.getName());

    /**
     * Returns the {@link ProxyMail} linked to the given proxy key or null
     * if it does not exist
     */
    protected ProxyMail getProxy(String proxyKey, JedisPool jedisPool, ObjectMapper mapper) {
        ProxyMail proxy = null;
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String proxyString = jedis.get(proxyKey);
            if (proxyString != null) {
                proxy = mapper.readValue(proxyString, ProxyMail.class);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            jedis = null;
        } finally {
            returnJedisIfNotNull(jedisPool, jedis);
        }
        return proxy;
    }
}
