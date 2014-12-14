package com.forwardcat.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkState;

/**
 * Temporary mail
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyMail {

  private final String userAddress;
  private final String creationTime;
  private final String lang;
  private String expirationTime;
  private boolean blocked = false;
  private boolean active = false;

  @JsonCreator
  public ProxyMail(@JsonProperty("ua") String userAddress, @JsonProperty("ts") String creationTime,
                   @JsonProperty("ex") String expirationTime, @JsonProperty("l") String lang) {
    this.userAddress = userAddress;
    this.creationTime = creationTime;
    this.expirationTime = expirationTime;
    this.lang = lang;
  }

  @JsonProperty("ua")
  public String getUserAddress() {
    return userAddress;
  }

  @JsonProperty("ts")
  public String getCreationTime() {
    return creationTime;
  }

  @JsonProperty("ex")
  public String getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(String expirationTime) {
    this.expirationTime = expirationTime;
  }

  @JsonProperty("l")
  public String getLang() {
    return lang;
  }

  public void block() {
    checkState(!blocked, "Proxy is already blocked");
    blocked = true;
  }

  @JsonProperty("bl")
  public boolean isBlocked() {
    return blocked;
  }

  public void activate() {
    checkState(!active, "Proxy is already active");
    active = true;
  }

  @JsonProperty("ac")
  public boolean isActive() {
    return active;
  }
}
