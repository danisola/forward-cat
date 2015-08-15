package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.bean.EntityBean;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.User;
import org.apache.mailet.MailAddress;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static models.ExpirationUtils.now;
import static models.ExpirationUtils.toDate;

public class Repository {

    public void save(EntityBean entity) {
        Ebean.save(entity);
    }

    public void delete(ProxyMail proxy) {
        Ebean.delete(proxy);
    }

    /**
     * Returns the {@link ProxyMail} linked to the given mail address
     */
    public Optional<ProxyMail> getProxy(@Nonnull MailAddress address) {
        return Optional.ofNullable(Ebean.find(ProxyMail.class, toString(address)));
    }

    public Optional<User> getUser(@Nonnull MailAddress address) {
        return Optional.ofNullable(
                Ebean.find(User.class)
                        .where()
                        .eq("email_address", toString(address))
                        .findUnique()
        );
    }

    public boolean proxyExists(@Nonnull MailAddress proxyMailAddress) {
        int count = Ebean.createQuery(ProxyMail.class)
                .where()
                .eq("proxy_address", toString(proxyMailAddress))
                .findRowCount();
        return count >= 1;
    }

    public void update(@Nonnull ProxyMail proxy) {
        Ebean.update(proxy);
    }

    public Set<ProxyMail> getExpiringProxies() {
        return Ebean.find(ProxyMail.class)
                .where()
                .lt("expiration_time", toDate(now().plusDays(1)))
                .eq("expiration_notified", false)
                .findSet();
    }

    public void removeExpiredProxies() {
        List<Object> expiredProxies = Ebean.find(ProxyMail.class)
                .where()
                .lt("expiration_time", new Date())
                .findIds();

        delete(ProxyMail.class, expiredProxies);
    }

    public void removeNonActivatedProxies() {
        List<Object> ids = Ebean.find(ProxyMail.class)
                .where()
                .eq("active", false)
                .lt("creation_time", toDate(now().plusMinutes(-30)))
                .findIds();

        delete(ProxyMail.class, ids);
    }

    public void removeUsersWithoutProxies() {
        String sql = "select u.id from users u where u.id not in " +
                "(select user_id from proxy_mail)";

        RawSql rawSql = RawSqlBuilder
                .parse(sql)
                .columnMapping("u.id", "id")
                .create();

        List<Object> ids = Ebean.find(User.class)
                .setRawSql(rawSql)
                .findIds();

        delete(User.class, ids);
    }

    private <T> void delete(Class<T> clazz, List<Object> ids) {
        if (!ids.isEmpty()) {
            Ebean.delete(clazz, ids);
        }
    }

    private String toString(@Nonnull MailAddress address) {
        return address.toString().toLowerCase();
    }
}
