package models;

import redis.clients.jedis.Protocol;

/**
 * models.Options for the Server
 */
public class Options {

    // Redis models.Options
    private String redisHost = "localhost";

    private int redisPort = Protocol.DEFAULT_PORT;

    private int maxActiveConnections = Runtime.getRuntime().availableProcessors() * 4;

    private int maxIdleConnections = maxActiveConnections / 2;

    private int minIdleConnections = 1;

    private int numTestsPerEvictionRun = maxActiveConnections;

    private boolean testConnectionWhileIdle = true;

    private boolean testConnectionOnBorrow = true;

    private boolean testConnectionOnReturn = false;

    private long timeBetweenEvictionRunsMillis = 60000;

    private int minutesBetweenAlerts = 5;

    private int timeBetweenAlertMailsMillis = 1000;

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public int getMaxActiveConnections() {
        return maxActiveConnections;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public int getMinIdleConnections() {
        return minIdleConnections;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public boolean isTestConnectionWhileIdle() {
        return testConnectionWhileIdle;
    }

    public boolean isTestConnectionOnBorrow() {
        return testConnectionOnBorrow;
    }

    public boolean isTestConnectionOnReturn() {
        return testConnectionOnReturn;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public int getMinutesBetweenAlerts() {
        return minutesBetweenAlerts;
    }

    public int getTimeBetweenAlertMailsMillis() {
        return timeBetweenAlertMailsMillis;
    }

    @Override
    public String toString() {
        return "Options{" +
                "redisHost='" + redisHost + '\'' +
                ", redisPort=" + redisPort +
                ", maxActiveConnections=" + maxActiveConnections +
                ", maxIdleConnections=" + maxIdleConnections +
                ", minIdleConnections=" + minIdleConnections +
                ", numTestsPerEvictionRun=" + numTestsPerEvictionRun +
                ", testConnectionWhileIdle=" + testConnectionWhileIdle +
                ", testConnectionOnBorrow=" + testConnectionOnBorrow +
                ", testConnectionOnReturn=" + testConnectionOnReturn +
                ", timeBetweenEvictionRunsMillis=" + timeBetweenEvictionRunsMillis +
                ", minutesBetweenAlerts=" + minutesBetweenAlerts +
                ", timeBetweenAlertMailsMillis=" + timeBetweenAlertMailsMillis +
                '}';
    }
}
