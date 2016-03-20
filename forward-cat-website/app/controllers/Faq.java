package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.faq;

@With(RedirectAction.class)
public class Faq extends Controller {

    public Result get(String langCode) {
        return ok(faq.render(lang()));
    }
}
