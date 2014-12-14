package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.google.inject.Inject;
import models.MailSender;
import org.apache.mailet.MailAddress;
import play.Play;
import play.i18n.Lang;
import play.mvc.Result;
import play.twirl.api.Html;
import redis.clients.jedis.JedisPool;
import views.html.report;
import views.html.user_reported;
import views.html.user_reported_email;

import javax.mail.internet.AddressException;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.*;

public class Report extends AbstractController {

    private final MailSender mailSender;
    private final JedisPool jedisPool;
    private final ObjectMapper mapper;
    private final MailAddress reportAddress;

    @Inject
    public Report(JedisPool jedisPool, ObjectMapper mapper, MailSender mailSender) throws AddressException {
        this.jedisPool = jedisPool;
        this.mapper = mapper;
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
        MailAddress mailAddress = toMailAddress(proxy);
        if (mailAddress != null && isLocal(mailAddress)) {

            String proxyKey = generateProxyKey(mailAddress);
            ProxyMail proxyMail = getProxy(proxyKey, jedisPool, mapper);
            if (proxyMail != null && proxyMail.isActive() && !proxyMail.isBlocked()) {
                Html content = user_reported_email.render(lang(), proxy, message);
                mailSender.sendHtmlMail(reportAddress, "User reported", content.toString());
            }
        }

        return ok("true");
    }
}
