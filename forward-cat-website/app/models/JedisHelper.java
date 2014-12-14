package models;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisHelper {

    public static void returnJedisOnException(JedisPool pool, Jedis jedis, Exception ex) {
        if (ex instanceof JedisConnectionException) {
            pool.returnBrokenResource(jedis);
        } else {
            pool.returnResource(jedis);
        }
    }

    public static void returnJedisIfNotNull(JedisPool pool, Jedis jedis) {
        if (jedis != null) {
            pool.returnResource(jedis);
        }
    }
}
