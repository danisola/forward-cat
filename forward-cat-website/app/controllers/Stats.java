package controllers;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import play.mvc.Result;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import views.html.stats;

import static com.forwardcat.common.RedisKeys.*;

public class Stats extends AbstractController {

    @Inject
    Stats(JedisPool jedisPool) {
        super(jedisPool);
    }

    public Result render() {
        StatsCounters counters = dbFunction(jedis -> {
            Pipeline pipeline = jedis.pipelined();

            Response<Long> activeProxiesRs = pipeline.zcard(ALERTS_SET);
            Response<String> emailsBlockedRs = pipeline.get(EMAILS_BLOCKED_COUNTER);
            Response<String> emailsForwardedRs = pipeline.get(EMAILS_FORWARDED_COUNTER);
            Response<String> proxiesActivatedRs = pipeline.get(PROXIES_ACTIVATED_COUNTER);

            pipeline.sync();

            return new StatsCounters(
                    activeProxiesRs.get().intValue(),
                    toInt(emailsBlockedRs.get()),
                    toInt(emailsForwardedRs.get()),
                    toInt(proxiesActivatedRs.get()));
        }).get();

        return ok(stats.render(lang(), counters));
    }

    public static class StatsCounters {
        public final int activeProxies;
        public final int emailsBlocked;
        public final int emailsForwarded;
        public final int proxiesActivated;

        public StatsCounters(int activeProxies, int emailsBlocked, int emailsForwarded, int proxiesActivated) {
            this.activeProxies = activeProxies;
            this.emailsBlocked = emailsBlocked;
            this.emailsForwarded = emailsForwarded;
            this.proxiesActivated = proxiesActivated;
        }
    }

    private int toInt(String str) {
        if (Strings.isNullOrEmpty(str)) {
            return 0;
        }
        return Integer.parseInt(str);
    }
}
