package models;

import com.forwardcat.common.RedisKeys;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

import static models.JedisHelper.returnJedisIfNotNull;
import static models.JedisHelper.returnJedisOnException;

public class SpamCatcher {

    private static Logger LOGGER = LoggerFactory.getLogger(SpamCatcher.class.getName());
    private final Set<String> usernameStopwords;

    @Inject
    public SpamCatcher(JedisPool jedisPool) {
        usernameStopwords = getUsernameStopwords(jedisPool);
        LOGGER.info("Username stopwords loaded: " + usernameStopwords.toString());
    }

    public boolean isSpam(MailAddress proxyAddress) {
        String user = normalize(proxyAddress.getLocalPart());
        return usernameStopwords.stream().anyMatch(user::contains);
    }

    private String normalize(String text) {
        return text.toLowerCase()
                   .replaceAll("[^\\w]", "");
    }

    private Set<String> getUsernameStopwords(JedisPool jedisPool) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.smembers(RedisKeys.USERNAME_STOPWORDS_SET);
        } catch (Exception ex) {
            returnJedisOnException(jedisPool, jedis, ex);
            jedis = null;
            throw Throwables.propagate(ex);
        } finally {
            returnJedisIfNotNull(jedisPool, jedis);
        }
    }
}
