package com.forwardcat.common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Set;
import java.util.UUID;

@Entity(name = "users")
public class User {

    @Id @Column(nullable = false)
    private UUID id;
    @Column(nullable = false)
    private String userAddress;
    private Set<ProxyMail> proxyMails;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public Set<ProxyMail> getProxyMails() {
        return proxyMails;
    }

    public void setProxyMails(Set<ProxyMail> proxyMails) {
        this.proxyMails = proxyMails;
    }
}
