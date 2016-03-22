package controllers;

import models.DomainManager;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

public class RedirectAction extends play.mvc.Action.Simple {

    @Override
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        return DomainManager.redirect(ctx, delegate);
    }
}
