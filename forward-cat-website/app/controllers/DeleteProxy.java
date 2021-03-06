package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.Repository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.confirm_deletion;
import views.html.error_page;
import views.html.proxy_deleted;

import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.formatInstant;

@With(RedirectAction.class)
public class DeleteProxy extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteProxy.class.getName());
    private final Repository repository;

    @Inject
    DeleteProxy(Repository repository) {
        this.repository = repository;
    }

    public Result confirmDeletion(String langCode, String p, String h) {
        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(lang(), empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = repository.getProxy(proxyMail);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(lang(), empty()));
        }

        // Checking that the proxy is active
        ProxyMail proxy = maybeProxy.get();
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy {} is not active", proxy);
            return badRequest();
        }

        // Generating the answer
        String expirationDate = formatInstant(proxy.getExpirationTime(), lang());
        String hashValue = getHash(proxy);
        return ok(confirm_deletion.render(lang(), proxyMail, expirationDate, hashValue));
    }

    public Result delete(String langCode, String p, String h) {
        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(lang(), empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = repository.getProxy(proxyMail);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(lang(), empty()));
        }

        // Checking whether the proxy is active or not
        ProxyMail proxy = maybeProxy.get();
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy is not active");
            return badRequest();
        }

        // Removing the proxy
        repository.delete(proxy);

        // Sending the response
        return ok(proxy_deleted.render(lang()));
    }
}
