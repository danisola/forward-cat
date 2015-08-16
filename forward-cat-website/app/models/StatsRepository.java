package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.FutureRowCount;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.inject.Inject;

import static com.forwardcat.common.RedisKeys.*;
import static models.JedisHelper.returnJedisIfNotNull;
import static models.JedisHelper.returnJedisOnException;

public class StatsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsRepository.class.getName());
    private final JedisPool jedisPool;

    @Inject
    public StatsRepository(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void incrementCounter(String counter) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.incr(counter);
        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            jedis = null;
        } finally {
            returnJedisIfNotNull(jedisPool, jedis);
        }
    }

    public StatsCounters getStats(){
        Jedis jedis = null;
        StatsCounters counters = null;
        try {
            jedis = jedisPool.getResource();

            Pipeline pipeline = jedis.pipelined();

            FutureRowCount<User> activeUsersRs = Ebean.find(User.class).findFutureRowCount();
            FutureRowCount<ProxyMail> activeProxiesRs = Ebean.find(ProxyMail.class).findFutureRowCount();
            Response<String> emailsBlockedRs = pipeline.get(EMAILS_BLOCKED_COUNTER);
            Response<String> emailsForwardedRs = pipeline.get(EMAILS_FORWARDED_COUNTER);
            Response<String> proxiesActivatedRs = pipeline.get(PROXIES_ACTIVATED_COUNTER);
            Response<String> spammerProxiesBlockedRs = pipeline.get(SPAMMER_PROXIES_BLOCKED_COUNTER);

            pipeline.sync();

            counters = new StatsCounters(
                    activeUsersRs.get(),
                    activeProxiesRs.get(),
                    toInt(emailsBlockedRs.get()),
                    toInt(emailsForwardedRs.get()),
                    toInt(proxiesActivatedRs.get()),
                    toInt(spammerProxiesBlockedRs.get()));

        } catch (Exception ex) {
            LOGGER.error("Error while connecting to Redis", ex);
            returnJedisOnException(jedisPool, jedis, ex);
            jedis = null;
        } finally {
            returnJedisIfNotNull(jedisPool, jedis);
        }
        return counters;
    }

    private int toInt(String str) {
        if (Strings.isNullOrEmpty(str)) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    public static class StatsCounters {
        public final int activeUsers;
        public final int activeProxies;
        public final int emailsBlocked;
        public final int emailsForwarded;
        public final int proxiesActivated;
        public final int spammerProxiesBlocked;

        public StatsCounters(int activeUsers, int activeProxies, int emailsBlocked, int emailsForwarded, int
                proxiesActivated, int spammerProxiesBlocked) {
            this.activeUsers = activeUsers;
            this.activeProxies = activeProxies;
            this.emailsBlocked = emailsBlocked;
            this.emailsForwarded = emailsForwarded;
            this.proxiesActivated = proxiesActivated;
            this.spammerProxiesBlocked = spammerProxiesBlocked;
        }
    }
}
