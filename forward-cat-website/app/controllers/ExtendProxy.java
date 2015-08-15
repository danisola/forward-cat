package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.Repository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.error_page;
import views.html.proxy_extended;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class ExtendProxy extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendProxy.class.getName());
    private final Repository repository;

    @Inject
    ExtendProxy(Repository repository) {
        this.repository = repository;
    }

    public Result extend(String p, String h) {
        Http.Request request = request();
        Lang language = getBestLanguage(request, lang());

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(language, empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyMail = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = repository.getProxy(proxyMail);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(language, empty()));
        }

        // Checking that the proxy is active
        ProxyMail proxy = maybeProxy.get();
        if (!proxy.isActive()) {
            LOGGER.debug("Proxy {} is already active", proxy);
            return badRequest();
        }

        // Checking that the proxy has not been active for more than 15 days

        ZonedDateTime maxExpirationTime = toZonedDateTime(proxy.getCreationTime()).plusDays(getMaxProxyDuration());
        ZonedDateTime newExpirationTime = getNewExpirationTime(toZonedDateTime(proxy.getExpirationTime()).plusDays(getIncrementDaysAdded()), maxExpirationTime);

        Date expirationTimeDate = toDate(newExpirationTime);
        proxy.setExpirationTime(expirationTimeDate);

        repository.update(proxy);

        // Generating the answer
        String date = formatInstant(expirationTimeDate, language);
        return ok(proxy_extended.render(language, proxyMail, date));
    }

    private ZonedDateTime getNewExpirationTime(ZonedDateTime newExpirationTime, ZonedDateTime maxExpirationTime) {
        if (newExpirationTime.isAfter(maxExpirationTime)) {
            return maxExpirationTime;
        }
        return newExpirationTime;
    }
}
