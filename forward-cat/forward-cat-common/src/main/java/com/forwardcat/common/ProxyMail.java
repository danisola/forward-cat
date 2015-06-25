package com.forwardcat.common;

import org.apache.mailet.MailAddress;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

import static com.google.common.base.Preconditions.checkState;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Temporary mail
 */
@Entity(name = "proxies")
public class ProxyMail {

    @Id @Column(nullable = false)
    private String proxyAddress;
    @Column(nullable = false)
    private String userAddress;
    @Column(nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;
    @Column(nullable = false)
    private String lang;
    @Column(nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date expirationTime;
    private boolean blocked = false;
    private boolean active = false;
    private boolean expirationNotified = false;

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTimeStr(String expirationTime) {
        this.expirationTime = Date.from(Instant.from(ISO_OFFSET_DATE_TIME.parse(expirationTime)));
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void block() {
        checkState(!blocked, "Proxy is already blocked");
        blocked = true;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void activate() {
        checkState(!active, "Proxy is already active");
        active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpirationNotified() {
        return expirationNotified;
    }

    public void setExpirationNotified(boolean expirationNotified) {
        this.expirationNotified = expirationNotified;
    }

    @Override
    public String toString() {
        return "ProxyMail{" +
                "proxyAddress='" + proxyAddress + '\'' +
                ", userAddress='" + userAddress + '\'' +
                ", creationTime='" + creationTime + '\'' +
                ", lang='" + lang + '\'' +
                ", expirationTime='" + expirationTime + '\'' +
                ", blocked=" + blocked +
                ", active=" + active +
                ", expirationNotified=" + expirationNotified +
                '}';
    }

    public static ProxyMail create(MailAddress proxyAddress, MailAddress userAddress, Date creationTime, Date expirationTime, String lang) {
        ProxyMail proxy = new ProxyMail();
        proxy.setProxyAddress(proxyAddress.toString().toLowerCase());
        proxy.setUserAddress(userAddress.toString().toLowerCase());
        proxy.setCreationTime(creationTime);
        proxy.setExpirationTime(expirationTime);
        proxy.setLang(lang);
        return proxy;
    }
}
