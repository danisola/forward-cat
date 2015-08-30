package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import com.google.inject.Inject;
import models.MailSender;
import models.Repository;
import org.apache.mailet.MailAddress;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.twirl.api.Html;
import views.html.email_sent;
import views.html.error_page;
import views.html.proxy_created_email;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.now;
import static models.ExpirationUtils.toDate;

@With(RedirectAction.class)
public class AddProxy extends Controller {

    private static final int INITIAL_PROXY_DURATION = 7;
    private static final int MAX_PROXIES = 3;
    private final Repository repository;
    private final MailSender mailSender;

    @Inject
    AddProxy(Repository repository, MailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    public Result addProxy(String langCode, String proxy, String email) {
        // Checking params
        Optional<MailAddress> maybeUserMail = toMailAddress(email);
        if (proxy == null || !maybeUserMail.isPresent()) {
            return badRequest(error_page.render(lang(), empty()));
        }

        MailAddress userMail = maybeUserMail.get();
        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest(error_page.render(lang(), empty()));
        }

        Optional<MailAddress> maybeProxyMailAddress = getMailAddress(proxy);
        if (!maybeProxyMailAddress.isPresent()) {
            return badRequest(error_page.render(lang(), empty()));
        }

        MailAddress proxyMailAddress = maybeProxyMailAddress.get();

        ZonedDateTime creationTime = now();
        ZonedDateTime expirationTime = creationTime.plusDays(INITIAL_PROXY_DURATION);
        ProxyMail proxyMail = ProxyMail.create(proxyMailAddress, userMail, toDate(creationTime), toDate(expirationTime), lang().code());

        // Creating the proxy
        if (repository.proxyExists(proxyMailAddress)) {
            return badRequest(error_page.render(lang(), empty())); // Proxy already exists: cannot create a new one
        }

        User user = repository.getUser(userMail).orElseGet(() ->
                User.create(userMail, toDate(creationTime)));

        if (user.getProxies().size() >= MAX_PROXIES) {
            return badRequest(error_page.render(lang(), Optional.of("error.too_many_proxies")));
        }

        user.getProxies().add(proxyMail);
        repository.save(user);

        // Sending the confirmation mail
        String subject = "Forward Cat";
        Html content = proxy_created_email.render(lang(), proxyMailAddress, getHash(proxyMail));
        mailSender.sendHtmlMail(userMail, subject, content.toString());

        return ok(email_sent.render(lang()));
    }
}
