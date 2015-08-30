package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.forward;

import javax.inject.Singleton;

@Singleton
@With(RedirectAction.class)
public class Landing extends Controller {

    public Result index(String langCode) {
        return ok(forward.render(lang()));
    }
}
