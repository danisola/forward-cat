import alerts.GuiceJobFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import models.MailSender;
import models.Options;
import models.SpamCatcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import play.libs.Json;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Module extends AbstractModule {

    private final Options options = new Options();

    @Override
    protected void configure() {
        // Performing bindings
        bind(Options.class).toInstance(options);
        bind(MailSender.class).in(Singleton.class);
        bind(SpamCatcher.class).in(Singleton.class);

        Json.setObjectMapper(new ObjectMapper().registerModule(new AfterburnerModule()));
    }

    @Provides
    @Singleton
    public JedisPool providesJedisPool() {
        // Configuring the connection pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxActive(options.getMaxActiveConnections());
        poolConfig.setTestOnBorrow(options.isTestConnectionOnBorrow());
        poolConfig.setTestOnReturn(options.isTestConnectionOnReturn());
        poolConfig.setMaxIdle(options.getMaxIdleConnections());
        poolConfig.setMinIdle(options.getMinIdleConnections());
        poolConfig.setTestWhileIdle(options.isTestConnectionWhileIdle());
        poolConfig.setNumTestsPerEvictionRun(options.getNumTestsPerEvictionRun());
        poolConfig.setTimeBetweenEvictionRunsMillis(options.getTimeBetweenEvictionRunsMillis());

        return new JedisPool(poolConfig, options.getRedisHost(), options.getRedisPort());
    }

    @Provides
    public Scheduler providesScheduler(GuiceJobFactory factory, StdSchedulerFactory schedulerFactory) throws SchedulerException {
        try {
            schedulerFactory.initialize("quartz-scheduler.properties");
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.setJobFactory(factory);
            scheduler.start();
            return scheduler;
        } catch (SchedulerException e) {
            throw Throwables.propagate(e);
        }
    }
}
