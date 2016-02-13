package controllers;

import com.google.inject.Inject;
import models.MailSender;
import org.apache.mailet.MailAddress;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.twirl.api.Html;
import views.html.contact;
import views.html.contact_email;
import views.html.contact_sent;

import javax.mail.internet.AddressException;

@With(RedirectAction.class)
public class Contact extends Controller {

    private final MailSender mailSender;
    private final MailAddress contactAddress;

    @Inject
    Contact(MailSender mailSender) throws AddressException {
        this.mailSender = mailSender;
        this.contactAddress = new MailAddress(Play.application().configuration().getString("contactAddress"));
    }

    public Result contactGet(String langCode) {
        return ok(contact.render(lang()));
    }

    public Result contactSent(String langCode, String email, String message) {
        Html content = contact_email.render(lang(), email, message);
        mailSender.sendHtmlMail(contactAddress, "Contact", content.toString());
        return ok(contact_sent.render(lang()));
    }
}
