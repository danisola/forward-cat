package com.forwardcat.common;

import com.google.common.collect.Sets;
import org.apache.mailet.MailAddress;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Entity(name = "users")
@Table(name = "users")
public class User {

    @Id @Column(nullable = false)
    private UUID id;
    @Column(nullable = false)
    private String emailAddress;
    @Column(nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    private Set<ProxyMail> proxies;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Set<ProxyMail> getProxies() {
        return proxies;
    }

    public void setProxies(Set<ProxyMail> proxies) {
        this.proxies = proxies;
    }

    public static User create(MailAddress userAddress, Date creationTime, ProxyMail ... proxies) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailAddress(userAddress.toString().toLowerCase());
        user.setCreationTime(creationTime);
        user.setProxies(Sets.newHashSet(proxies));
        return user;
    }
}
