package models;

import com.forwardcat.common.ProxyMail;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.mailet.MailAddress;
import play.Play;
import play.i18n.Lang;
import play.mvc.Http;

import javax.mail.internet.AddressException;
import java.util.Optional;
import java.util.regex.Pattern;

public class ControllerUtils {
    private static final Pattern HOST_LANG_PATTTERN = Pattern.compile("^[a-z]{2}\\..*");
    private static final String DOMAIN_NAME_PROP = "domainName";

    public static Lang getBestLanguage(Http.Request request, Lang preferred) {
        // Getting the info from the hostname
        String host = request.host();
        if (host != null && HOST_LANG_PATTTERN.matcher(host).matches()) {
            Lang lang = Lang.forCode(host.substring(0, 2));
            if (Lang.availables().contains(lang)) {
                return lang;
            }
        }

        // Returning the preferred language
        return preferred;
    }

    private static HashFunction HASH_FUNCTION = Hashing.murmur3_32(0); // Setting seed for consistent hashing

    /**
     * Given a {@link com.forwardcat.common.ProxyMail}, returns a hash that identifies it uniquely
     */
    public static String getHash(ProxyMail proxy) {
        Hasher hasher = HASH_FUNCTION.newHasher();
        hasher.putString(proxy.getCreationTime().toString(), Charsets.UTF_8);
        return hasher.hash().toString();
    }

    /**
     * Given a username and a domain name, returns a {@link org.apache.mailet.MailAddress}
     */
    public static Optional<MailAddress> getMailAddress(String username) {
        String domainName = Play.application().configuration().getString(DOMAIN_NAME_PROP);
        return getMailAddress(username, domainName);
    }

    /**
     * Given a username and a domain name, returns a {@link org.apache.mailet.MailAddress}
     */
    public static Optional<MailAddress> getMailAddress(String username, String domainName) {
        if (username != null && domainName != null) {
            try {
                return Optional.of(new MailAddress(username, domainName));
            } catch (AddressException e) {
                // Do nothing
            }
        }
        return Optional.empty();
    }

    /**
     * Returns true if the given {@link MailAddress} has the local domain name
     */
    public static boolean isLocal(MailAddress userMail) {
        String domainName = Play.application().configuration().getString(DOMAIN_NAME_PROP);
        return domainName.equals(userMail.getDomain());
    }

    /**
     * Converts the given String to a {@link MailAddress}
     */
    public static Optional<MailAddress> toMailAddress(String emailAddress) {
        if (emailAddress != null) {
            try {
                return Optional.of(new MailAddress(emailAddress));
            } catch (AddressException ex) {
                // Do nothing
            }
        }
        return Optional.empty();
    }

    private ControllerUtils() {
        // Non-instantiable
    }
}
