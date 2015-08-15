package controllers;

import com.google.inject.Inject;
import models.Repository;
import org.apache.mailet.MailAddress;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Optional;

import static models.ControllerUtils.getMailAddress;

public class ValidateProxy extends Controller {

    private final Repository repository;

    @Inject
    ValidateProxy(Repository repository) {
        this.repository = repository;
    }

    public Result validate(String proxy) {
        // Checking params
        Optional<MailAddress> mailAddress = getMailAddress(proxy);
        if (!mailAddress.isPresent()) {
            return ok(Boolean.FALSE.toString());
        }

        Boolean valid = !repository.proxyExists(mailAddress.get());
        return ok(valid.toString());
    }
}
