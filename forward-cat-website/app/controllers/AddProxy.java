package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import models.ProxyRepository;
import org.apache.mailet.MailAddress;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import views.html.proxy_created_email;

import java.time.ZonedDateTime;
import java.util.Optional;

import static models.ControllerUtils.*;
import static models.ExpirationUtils.*;

public class AddProxy extends Controller {

    private final ProxyRepository proxyRepo;
    private final MailSender mailSender;

    @Inject
    AddProxy(ProxyRepository proxyRepo, MailSender mailSender) {
        this.proxyRepo = proxyRepo;
        this.mailSender = mailSender;
    }

    public Result addProxy(String proxy, String email, int duration) {
        Http.Request request = request();

        // Checking params
        Optional<MailAddress> maybeUserMail = toMailAddress(email);
        if (proxy == null || !maybeUserMail.isPresent() || !isValidDuration(duration)) {
            return badRequest();
        }

        MailAddress userMail = maybeUserMail.get();
        // Don't allow chained proxies
        if (isLocal(userMail)) {
            return badRequest();
        }

        Optional<MailAddress> maybeProxyMailAddress = getMailAddress(proxy);
        if (!maybeProxyMailAddress.isPresent()) {
            return badRequest();
        }

        MailAddress proxyMailAddress = maybeProxyMailAddress.get();

        ZonedDateTime creationTime = now();
        ZonedDateTime expirationTime = creationTime.plusDays(duration);
        Lang lang = getBestLanguage(request, lang());
        ProxyMail proxyMail = ProxyMail.create(proxyMailAddress, userMail, toDate(creationTime), toDate(expirationTime), lang.code());

        // Creating the proxy
        if (!proxyRepo.exists(proxyMailAddress)) {
            proxyRepo.save(proxyMail);
        } else {
            return badRequest(); // Proxy already exists: cannot create a new one
        }

        // Sending the confirmation mail
        String subject = "Forward Cat";
        Html content = proxy_created_email.render(lang, proxyMailAddress.toString(), getHash(proxyMail));
        mailSender.sendHtmlMail(userMail, subject, content.toString());

        return ok("true");
    }
}
