package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import com.google.inject.Inject;
import models.MailSender;
import models.Repository;
import org.apache.mailet.MailAddress;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import views.html.email_sent;
import views.html.error_page;
import views.html.proxy_created_email;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Optional.empty;
import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class AddProxy extends Controller {

    private final Repository repository;
    private final MailSender mailSender;
    private final int INITIAL_PROXY_DURATION = 7;

    @Inject
    AddProxy(Repository repository, MailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    public Result addProxy(String proxy, String email) {
        Http.Request request = request();
        Lang lang = getBestLanguage(request, lang());

        // Checking params
        Optional<MailAddress> maybeUserMail = toMailAddress(email);
        if (proxy == null || !maybeUserMail.isPresent()) {
            return badRequest(error_page.render(lang, empty()));
        }

        MailAddress userMail = maybeUserMail.get();
        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest(error_page.render(lang, empty()));
        }

        Optional<MailAddress> maybeProxyMailAddress = getMailAddress(proxy);
        if (!maybeProxyMailAddress.isPresent()) {
            return badRequest(error_page.render(lang, empty()));
        }

        MailAddress proxyMailAddress = maybeProxyMailAddress.get();

        ZonedDateTime creationTime = now();
        ZonedDateTime expirationTime = creationTime.plusDays(INITIAL_PROXY_DURATION);
        ProxyMail proxyMail = ProxyMail.create(proxyMailAddress, userMail, toDate(creationTime), toDate(expirationTime), lang.code());

        // Creating the proxy
        Optional<User> maybeExistingUser = repository.getUser(userMail);
        if (!repository.proxyExists(proxyMailAddress)) {
            User user = maybeExistingUser.orElseGet(() -> User.create(userMail, toDate(creationTime)));
            user.getProxies().add(proxyMail);
            repository.save(user);
        } else {
            return badRequest(error_page.render(lang, empty())); // Proxy already exists: cannot create a new one
        }

        // Sending the confirmation mail
        String subject = "Forward Cat";
        Html content = proxy_created_email.render(lang, proxyMailAddress, getHash(proxyMail));
        mailSender.sendHtmlMail(userMail, subject, content.toString());

        return ok(email_sent.render(lang));
    }
}
