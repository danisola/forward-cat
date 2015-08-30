package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.inject.Inject;
import models.Repository;
import models.SpamCatcher;
import models.StatsRepository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.error_page;
import views.html.proxy_created;

import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.isAuthenticated;
import static models.ControllerUtils.toMailAddress;
import static models.ExpirationUtils.formatInstant;

@With(RedirectAction.class)
public class ConfirmProxy extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmProxy.class.getName());
    private final StatsRepository statsRepo;
    private final Repository repository;
    private final SpamCatcher spamCatcher;

    @Inject
    ConfirmProxy(StatsRepository statsRepo, Repository repository, SpamCatcher spamCatcher) {
        this.statsRepo = statsRepo;
        this.repository = repository;
        this.spamCatcher = spamCatcher;
    }

    public Result confirm(String langCode, String p, String h) throws Exception {
        // Checking params
        Optional<MailAddress> maybeProxyMail = toMailAddress(p);
        if (!maybeProxyMail.isPresent() || h == null) {
            return badRequest(error_page.render(lang(), empty()));
        }

        // Getting the proxy & checking that the hash is correct
        MailAddress proxyAddress = maybeProxyMail.get();
        Optional<ProxyMail> maybeProxy = repository.getProxy(proxyAddress);
        if (!isAuthenticated(maybeProxy, h)) {
            return badRequest(error_page.render(lang(), empty()));
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

        repository.save(proxy);
        statsRepo.incrementCounter(RedisKeys.PROXIES_ACTIVATED_COUNTER);

        // Generating the response
        String expirationDate = formatInstant(proxy.getExpirationTime(), lang());
        return ok(proxy_created.render(lang(), proxyAddress, expirationDate));
    }
}
