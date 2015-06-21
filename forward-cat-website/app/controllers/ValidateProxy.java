package controllers;

import com.google.inject.Inject;
import models.ProxyRepository;
import org.apache.mailet.MailAddress;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Optional;

import static models.ControllerUtils.getMailAddress;

public class ValidateProxy extends Controller {

    private final ProxyRepository proxyRepo;

    @Inject
    ValidateProxy(ProxyRepository proxyRepo) {
        this.proxyRepo = proxyRepo;
    }

    public Result validate(String proxy) {
        // Checking params
        Optional<MailAddress> mailAddress = getMailAddress(proxy);
        if (!mailAddress.isPresent()) {
            return ok(Boolean.FALSE.toString());
        }

        Boolean valid = !proxyRepo.exists(mailAddress.get());
        return ok(valid.toString());
    }
}
