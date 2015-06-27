package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.Inject;
import models.ProxyRepository;
import models.SpamCatcher;
import models.StatsRepository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.error_page;
import views.html.proxy_created;

import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.formatInstant;

public class ConfirmProxy extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmProxy.class.getName());
    private final StatsRepository statsRepo;
    private final ProxyRepository proxyRepo;
    private final SpamCatcher spamCatcher;

    @Inject
    ConfirmProxy(StatsRepository statsRepo, ProxyRepository proxyRepo, SpamCatcher spamCatcher) {
        this.statsRepo = statsRepo;
        this.proxyRepo = proxyRepo;
        this.spamCatcher = spamCatcher;
    }

    public Result confirm(String p, String h) throws Exception {
        Http.Request request = request();
        Lang language = getBestLanguage(request, lang());

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(language, empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyAddress = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyAddress);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(language, empty()));
        }

        // Checking that the proxy is not already active
        ProxyMail proxy = maybeProxy.get();
        if (proxy.isActive()) {
            LOGGER.debug("Proxy {} is already active", proxy);
            return badRequest();
        }
        proxy.activate();

        if (spamCatcher.isSpam(proxyAddress)) {
            proxy.block();
            statsRepo.incrementCounter(RedisKeys.SPAMMER_PROXIES_BLOCKED_COUNTER);
        }

        proxyRepo.save(proxy);
        statsRepo.incrementCounter(RedisKeys.PROXIES_ACTIVATED_COUNTER);

        // Generating the response
        String expirationDate = formatInstant(proxy.getExpirationTime(), language);
        return ok(proxy_created.render(language, proxyAddress, expirationDate));
    }
}
