package controllers;

import com.avaje.ebean.Ebean;
import com.forwardcat.common.ProxyMail;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import models.StatsRepository;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import views.html.stats;

import static com.forwardcat.common.RedisKeys.*;

public class Stats extends Controller {

    private final StatsRepository statsRepo;

    @Inject
    Stats(StatsRepository statsRepo) {
        this.statsRepo = statsRepo;
    }

    public Result render() {
        return ok(stats.render(lang(), statsRepo.getStats()));
    }
}
