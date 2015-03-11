package com.forwardcat.james;

import com.forwardcat.common.StringNormalizer;
import com.google.common.collect.Lists;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import java.util.List;

public class MailUtils {

    private static final List<String> USER_IGNORE_KEYWORDS = Lists.newArrayList("noreply", "notreply", "notrespond", "no-responder");
    private static final List<String> FB_USER_IGNORE_KEYWORDS = Lists.newArrayList("notification", "update");

    public static boolean shouldBounce(Mail mail) {
        MailAddress sender = mail.getSender();

        boolean notRespondSender = false;
        boolean fbNotification = false;
        if (sender != null && sender.getLocalPart() != null) {
            String senderUser = StringNormalizer.onlyLowerCaseWords(sender.getLocalPart());
            notRespondSender = containsAny(senderUser, USER_IGNORE_KEYWORDS);

            if (sender.getDomain() != null) {
                String senderDomain = sender.getDomain().toLowerCase();
                fbNotification = "facebookmail.com".equals(senderDomain) && containsAny(senderUser, FB_USER_IGNORE_KEYWORDS);
            }
        }

        boolean toIgnore = notRespondSender || fbNotification;
        return !toIgnore;
    }

    private static boolean containsAny(String string, List<String> contained) {
        return contained.stream().anyMatch(string::contains);
    }

    private MailUtils() {

    }
}
