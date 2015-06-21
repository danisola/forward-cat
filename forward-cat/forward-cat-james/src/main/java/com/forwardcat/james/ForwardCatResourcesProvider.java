package com.forwardcat.james;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class that initializes and holds the resources used by the Forward Cat
 * mailets
 */
public class ForwardCatResourcesProvider {

    private static final String PROPS_FILE = "forward-cat.properties";

    private JedisPool pool;
    private EbeanServer ebeanServer;

    public JedisPool getPool() {
        return pool;
    }

    public EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    @PostConstruct
    public void init() {
        // Loading properties from file
        Properties props = new Properties();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            props.load(loader.getResourceAsStream(PROPS_FILE));
        } catch (IOException e) {
            // We can't do anything about it
            throw new IllegalStateException("Is " + PROPS_FILE + " in the classpath?", e);
        }

        configEbean(props);
        configRedis(props);
    }

    private void configRedis(Properties props) {
        // Configuring Jedis
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(Integer.parseInt(props.getProperty("max-active-connections", "10")));
        config.setMinIdle(Integer.parseInt(props.getProperty("min-idle-connections", "1")));

        String redisHost = props.getProperty("redis-host", "localhost");
        pool = new JedisPool(config, redisHost);
    }

    private void configEbean(Properties props) {
        // http://www.avaje.org/ebean/getstarted_programmatic.html
        ServerConfig config = new ServerConfig();
        config.setName("fwdconnection");

        // Define DataSource parameters
        DataSourceConfig postgresDb = new DataSourceConfig();
        postgresDb.setDriver(props.getProperty("datasource.pg.databaseDriver"));
        postgresDb.setUsername(props.getProperty("datasource.pg.username"));
        postgresDb.setPassword(props.getProperty("datasource.pg.password"));
        postgresDb.setUrl(props.getProperty("datasource.pg.databaseUrl"));
        postgresDb.setHeartbeatSql(props.getProperty("datasource.pg.heartbeatsql"));

        config.setDataSourceConfig(postgresDb);

        // set DDL options...
        config.setDdlGenerate(false);
        config.setDdlRun(false);

//        config.setDefaultServer(false);
//        config.setRegister(false);

        // automatically determine the DatabasePlatform
        // using the jdbc driver
        // config.setDatabasePlatform(new PostgresPlatform());

        config.addClass(ProxyMail.class);
        config.addClass(User.class);

        // specify jars to search for entity beans
        //        config.addJar("someJarThatContainsEntityBeans.jar");

        // create the EbeanServer instance
        ebeanServer = EbeanServerFactory.create(config);
    }

    @PreDestroy
    public void shutdown() {
        pool.destroy();
        ebeanServer.shutdown(true, true);
    }
}
