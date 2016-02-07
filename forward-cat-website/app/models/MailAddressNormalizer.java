package models;

import com.google.common.base.Throwables;
import org.apache.mailet.MailAddress;

import javax.mail.internet.AddressException;
import java.util.Optional;

public class MailAddressNormalizer {

    public static Optional<MailAddress> normalize(Optional<MailAddress> address) {
        return address.map(MailAddressNormalizer::normalize);
    }

    public static MailAddress normalize(MailAddress mailAddress) {
        if (isGmailAddress(mailAddress)) {
            String normalizedLocalPart = mailAddress.getLocalPart()
                    .replace(".", "");

            int plusSign = normalizedLocalPart.indexOf("+");
            if (plusSign >= 0) {
                normalizedLocalPart = normalizedLocalPart.substring(0, plusSign);
            }

            try {
                return new MailAddress(normalizedLocalPart, mailAddress.getDomain());
            } catch (AddressException e) {
                Throwables.propagate(e);
            }
        }
        return mailAddress;
    }

    private static boolean isGmailAddress(MailAddress address) {
        return "gmail.com".equals(address.getDomain());
    }

    private MailAddressNormalizer() {
        // Non-instantiable
    }
}
