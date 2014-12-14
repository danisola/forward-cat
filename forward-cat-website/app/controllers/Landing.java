package controllers;

import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.email_sent;
import views.html.forward;

import static models.ControllerUtils.getBestLanguage;

public class Landing extends Controller {

    public static Result index() {
        Lang lang = getBestLanguage(request(), lang());
        return ok(forward.render(lang));
    }

    public static Result emailSent() {
        Lang lang = getBestLanguage(request(), lang());
        return ok(email_sent.render(lang));
    }
}
