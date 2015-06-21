package models;

import com.avaje.ebean.Ebean;
import com.forwardcat.common.ProxyMail;
import org.apache.mailet.MailAddress;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static models.ExpirationUtils.now;
import static models.ExpirationUtils.toDate;

public class ProxyRepository {

    public void save(ProxyMail proxy) {
        Ebean.save(proxy);
    }

    public void delete(ProxyMail proxy) {
        Ebean.delete(proxy);
    }

    /**
     * Returns the {@link ProxyMail} linked to the given mail address
     */
    public Optional<ProxyMail> getProxy(MailAddress address) {
        return Optional.ofNullable(Ebean.find(ProxyMail.class, address.toString()));
    }

    public boolean exists(MailAddress proxyMailAddress) {
        int count = Ebean.createQuery(ProxyMail.class)
                .where()
                .eq("proxy_address", proxyMailAddress.toString())
                .findRowCount();
        return count >= 1;
    }

    public void update(ProxyMail proxy) {
        Ebean.update(proxy);
    }

    public Set<ProxyMail> getExpiringProxies() {
        Date upperThreshold = toDate(now().plusDays(1));
        return Ebean.find(ProxyMail.class)
                .where()
                .lt("expiration_time", upperThreshold)
                .eq("expiration_notified", false)
                .findSet();
    }

    public void removeExpiredProxies() {
        Set<ProxyMail> expiredProxies = Ebean.find(ProxyMail.class)
                .where()
                .lt("expiration_time", new Date())
                .findSet();

        Ebean.beginTransaction();
        try {
            expiredProxies.forEach(Ebean::delete);
            Ebean.commitTransaction();
        } finally {
            Ebean.endTransaction();
        }
    }

    public void removeNonActivatedProxies() {
        Date lowThreshold = toDate(now().plusMinutes(-30));
        Set<ProxyMail> nonActivatedProxies = Ebean.find(ProxyMail.class)
                .where()
                .eq("active", false)
                .lt("creation_time", lowThreshold)
                .findSet();

        Ebean.beginTransaction();
        try {
            nonActivatedProxies.forEach(Ebean::delete);
            Ebean.commitTransaction();
        } finally {
            Ebean.endTransaction();
        }
    }
}
