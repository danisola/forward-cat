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
import views.html.proxy_created;

import java.util.Optional;

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

        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest();
        }

        // Getting the proxy
        MailAddress proxyMailAddress = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = proxyRepo.getProxy(proxyMailAddress);
        if (!maybeProxy.isPresent()) {
            return badRequest();
        }

        // Checking that the hash is correct
        ProxyMail proxy = maybeProxy.get();
        String hashValue = getHash(proxy);
        if (!h.equals(hashValue)) {
            LOGGER.debug("Hash values are not equals %s - %s", h, hashValue);
            return badRequest();
        }

        // Checking that the proxy is not already active
        if (proxy.isActive()) {
            LOGGER.debug("Proxy {} is already active", proxy);
            return badRequest();
        }
        proxy.activate();

        if (spamCatcher.isSpam(proxyMailAddress)) {
            proxy.block();
            statsRepo.incrementCounter(RedisKeys.SPAMMER_PROXIES_BLOCKED_COUNTER);
        }

        proxyRepo.save(proxy);
        statsRepo.incrementCounter(RedisKeys.PROXIES_ACTIVATED_COUNTER);

        // Generating the response
        Lang language = getBestLanguage(request, lang());
        String expirationDate = formatInstant(proxy.getExpirationTime(), language);
        return ok(proxy_created.render(language, proxyMailAddress.toString(), expirationDate));
    }
}
