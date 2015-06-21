package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import models.ProxyRepository;
import org.apache.mailet.MailAddress;
import play.Play;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;
import views.html.report;
import views.html.user_reported;
import views.html.user_reported_email;

import javax.mail.internet.AddressException;
import java.util.Optional;

import static models.ControllerUtils.*;

public class Report extends Controller {

    private final ProxyRepository proxyRepo;
    private final MailSender mailSender;
    private final MailAddress reportAddress;

    @Inject
    Report(ProxyRepository proxyRepo, MailSender mailSender) throws AddressException {
        this.proxyRepo = proxyRepo;
        this.mailSender = mailSender;
        this.reportAddress = new MailAddress(Play.application().configuration().getString("reportAddress"));
    }

    public Result userReported() {
        Lang language = getBestLanguage(request(), lang());
        return ok(user_reported.render(language));
    }

    public Result reportGet() {
        Lang lang = getBestLanguage(request(), lang());
        return ok(report.render(lang));
    }

    public Result reportUser(String proxy, String message) {
        Optional<MailAddress> mailAddress = toMailAddress(proxy);
        if (mailAddress.isPresent() && isLocal(mailAddress.get())) {

            Optional<ProxyMail> maybeProxyMail = proxyRepo.getProxy(mailAddress.get());
            if (maybeProxyMail.isPresent()) {

                ProxyMail proxyMail = maybeProxyMail.get();
                if (proxyMail.isActive() && !proxyMail.isBlocked()) {
                    Html content = user_reported_email.render(lang(), proxy, message);
                    mailSender.sendHtmlMail(reportAddress, "User reported", content.toString());
                }
            }
        }

        return ok("true");
    }
}
