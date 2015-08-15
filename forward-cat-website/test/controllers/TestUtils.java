package controllers;

import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import com.google.common.base.Throwables;
import models.Repository;
import org.apache.mailet.MailAddress;

import javax.mail.internet.AddressException;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static ProxyMail activeProxy(MailAddress address, boolean blocked) {
        return proxy(address, blocked, true);
    }

    public static ProxyMail inactiveProxy(MailAddress address) {
        return proxy(address, false, false);
    }

    private static ProxyMail proxy(MailAddress address, boolean blocked, boolean active) {
        ProxyMail proxyMail = ProxyMail.create(address, toMailAddress("user@mail.com"), new Date(), new Date(), "en");
        if (blocked) {
            proxyMail.block();
        }
        if (active) {
            proxyMail.activate();
        }
        proxyMail.setUser(User.create(toMailAddress("user@address.com"), new Date()));
        return proxyMail;
    }

    public static MailAddress toMailAddress(String mailAddress) {
        try {
            return new MailAddress(mailAddress);
        } catch (AddressException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void whenAddressReturnProxy(Repository repository, MailAddress address, ProxyMail proxy) {
        when(repository.getProxy(any(MailAddress.class))).thenAnswer(invocationOnMock -> {
            MailAddress passedAddress = (MailAddress) invocationOnMock.getArguments()[0];
            if (address.toString().equals(passedAddress.toString())) {
                return Optional.of(proxy);
            }
            return Optional.empty();
        });
    }
}
