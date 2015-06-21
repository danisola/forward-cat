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
import views.html.proxy_deleted;

import java.util.Optional;

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

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            LOGGER.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyMail);
        if (!maybeProxy.isPresent()) {
            LOGGER.debug("Proxy {} doesn't exist", proxyMail);
            return badRequest();
        }

        // Checking that the hash is correct
        ProxyMail proxy = maybeProxy.get();
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            LOGGER.debug("Hash values are not equals {} - {}", h, hashValue);
            return badRequest();
        }

        // Checking that the proxy is active
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy {} is not active", proxy);
            return badRequest();
        }

        // Generating the answer
        Lang lang = getBestLanguage(request, lang());
        String expiratonDate = formatInstant(proxy.getExpirationTime(), lang);
        return ok(confirm_deletion.render(lang, proxyMail.toString(), expiratonDate, hashValue));
    }

    public Result delete(String p, String h) {
        Http.Request request = request();

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            LOGGER.debug("Wrong params: {}", request);
            return badRequest();
        }

        // Validating the proxy
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyMail);
        if (!maybeProxy.isPresent()) {
            LOGGER.debug("Proxy % doesn't exist", proxyMail);
            return badRequest();
        }

        // Checking that the hash is correct
        ProxyMail proxy = maybeProxy.get();
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            LOGGER.debug("Hash values are not equals %s - %s", h, hashValue);
            return badRequest();
        }

        // Checking whether the proxy is active or not
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy is not active");
            return badRequest();
        }

        // Removing the proxy
        proxyRepo.delete(proxy);

        // Sending the response
        Lang lang = getBestLanguage(request, lang());
        return ok(proxy_deleted.render(lang));
    }
}
