package controllers;

import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.forward;

import javax.inject.Singleton;

import static models.ControllerUtils.getBestLanguage;

@Singleton
public class Landing extends Controller {

    public Result index() {
        Lang lang = getBestLanguage(request(), lang());
        return ok(forward.render(lang));
    }
}
