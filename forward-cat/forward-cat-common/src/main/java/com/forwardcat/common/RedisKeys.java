package com.forwardcat.common;

import org.apache.mailet.MailAddress;

/**
 * Utility class that sets the namespaces for the Redis keys
 */
public class RedisKeys {

  public static final String PROXIES_ACTIVATED_COUNTER = generateCounterKey("proxies_activated");
  public static final String EMAILS_FORWARDED_COUNTER = generateCounterKey("emails_forwarded");
  public static final String EMAILS_BLOCKED_COUNTER = generateCounterKey("emails_blocked");
  public static final String SPAMMER_PROXIES_BLOCKED_COUNTER = generateCounterKey("spammer_proxies_blocked");
  public static final String SPAMMER_EMAILS_BLOCKED_COUNTER = generateCounterKey("spammer_emails_blocked");

  public static final String ALERTS_SET = "z:alerts";
  public static final String USERNAME_STOPWORDS_SET = "s:username_stopwords";

  /**
   * Given a {@link MailAddress}, returns the key that identifies the
   * proxy in Redis.
   */
  public static String generateProxyKey(MailAddress proxyAddress) {
    return "p:" + proxyAddress;
  }

  /**
   * Given a key, returns the Redis key of the counter
   */
  public static String generateCounterKey(String key) {
    return "c:" + key;
  }

  private RedisKeys() {
    // Non-instantiable
  }
}
