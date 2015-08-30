package controllers;

import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import models.Repository;
import org.apache.mailet.MailAddress;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.twirl.api.Html;
import views.html.report;
import views.html.user_reported;
import views.html.user_reported_email;

import javax.mail.internet.AddressException;
import java.util.Optional;

import static models.ControllerUtils.isLocal;
import static models.ControllerUtils.toMailAddress;

@With(RedirectAction.class)
public class Report extends Controller {

    private final Repository repository;
    private final MailSender mailSender;
    private final MailAddress reportAddress;

    @Inject
    Report(Repository repository, MailSender mailSender) throws AddressException {
        this.repository = repository;
        this.mailSender = mailSender;
        this.reportAddress = new MailAddress(Play.application().configuration().getString("reportAddress"));
    }

    public Result reportGet(String langCode) {
        return ok(report.render(lang()));
    }

    public Result reportUser(String langCode, String proxy, String message) {
        Optional<MailAddress> mailAddress = toMailAddress(proxy);
        if (mailAddress.isPresent() && isLocal(mailAddress.get())) {

            Optional<ProxyMail> maybeProxyMail = repository.getProxy(mailAddress.get());
            if (maybeProxyMail.isPresent()) {

                ProxyMail proxyMail = maybeProxyMail.get();
                if (proxyMail.isActive() && !proxyMail.isBlocked()) {
                    Html content = user_reported_email.render(lang(), proxy, message);
                    mailSender.sendHtmlMail(reportAddress, "User reported", content.toString());
                }
            }
        }

        return ok(user_reported.render(lang()));
    }
}
