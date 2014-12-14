package com.forwardcat.james;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class that initializes and holds the resources used by the Forward Cat
 * mailets
 */
public class ForwardCatResourcesProvider {

    private static final String PROPS_FILE = "forward-cat-jedis.properties";

    private static final String REDIS_HOST = "redis-host";
    private static final String MAX_ACTIVE = "max-active-connections";
    private static final String MIN_IDLE = "min-idle-connections";

    private JedisPool pool;
    private ObjectMapper mapper = new ObjectMapper();

    public ObjectMapper getMapper() {
        return mapper;
    }

    public JedisPool getPool() {
        return pool;
    }

    public ForwardCatResourcesProvider() {
        // Loading properties from file
        Properties props = new Properties();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            props.load(loader.getResourceAsStream(PROPS_FILE));
        } catch (IOException e) {
            // We can't do anything about it
            throw new IllegalStateException("Is " + PROPS_FILE + " in the classpath?", e);
        }

        // Configuring Jedis
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(Integer.parseInt(props.getProperty(MAX_ACTIVE, "10")));
        config.setMinIdle(Integer.parseInt(props.getProperty(MIN_IDLE, "1")));

        String redisHost = props.getProperty(REDIS_HOST, "localhost");
        pool = new JedisPool(config, redisHost);
    }
}
