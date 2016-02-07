package models;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.apache.mailet.MailAddress;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.util.Map;

import static models.MailAddressNormalizer.normalize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MailAddressNormalizerTest {

    Map<String, String> addressessPairs = ImmutableMap.<String, String>builder()
            .put("test@mail.com", "test@mail.com")
            .put("dots.test@gmail.com", "dotstest@gmail.com")
            .put("mail+test@gmail.com", "mail@gmail.com")
            .put("mail.dots+test@gmail.com", "maildots@gmail.com")
            .build();

    @Test
    public void testNormalize() {
        for (Map.Entry<String, String> pair : addressessPairs.entrySet()) {
            MailAddress original = toMail(pair.getKey());
            MailAddress expected = toMail(pair.getValue());
            MailAddress normalized = normalize(original);

            assertThat(normalized.toString(), is(expected.toString()));
        }
    }

    private MailAddress toMail(String address) {
        try {
            return new MailAddress(address);
        } catch (AddressException e) {
            throw Throwables.propagate(e);
        }
    }
}
