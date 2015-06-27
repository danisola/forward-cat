package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.ProxyRepository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.confirm_deletion;
import views.html.error_page;
import views.html.proxy_deleted;

import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.formatInstant;

public class DeleteProxy extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteProxy.class.getName());
    private final ProxyRepository proxyRepo;

    @Inject
    DeleteProxy(ProxyRepository proxyRepo) {
        this.proxyRepo = proxyRepo;
    }

    public Result confirmDeletion(String p, String h) {
        Http.Request request = request();
        Lang language = getBestLanguage(request, lang());

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(language, empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyMail);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(language, empty()));
        }

        // Checking that the proxy is active
        ProxyMail proxy = maybeProxy.get();
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy {} is not active", proxy);
            return badRequest();
        }

        // Generating the answer
        String expirationDate = formatInstant(proxy.getExpirationTime(), language);
        String hashValue = getHash(proxy);
        return ok(confirm_deletion.render(language, proxyMail, expirationDate, hashValue));
    }

    public Result delete(String p, String h) {
        Http.Request request = request();
        Lang lang = getBestLanguage(request, lang());

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(lang, empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyMail);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(lang, empty()));
        }

        // Checking whether the proxy is active or not
        ProxyMail proxy = maybeProxy.get();
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy is not active");
            return badRequest();
        }

        // Removing the proxy
        proxyRepo.delete(proxy);

        // Sending the response
        return ok(proxy_deleted.render(lang));
    }
}
