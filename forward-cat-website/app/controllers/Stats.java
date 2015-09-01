package controllers;

import com.google.inject.Inject;
import models.StatsRepository;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.stats;

@With(RedirectAction.class)
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
