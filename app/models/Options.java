package models;

import com.beust.jcommander.Parameter;
import redis.clients.jedis.Protocol;

/**
 * models.Options for the Server
 */
public class Options {

    @Parameter(names = {"-help", "-h"}, description = "Print this help")
    private boolean help = false;

    @Parameter(names = {"-port", "-p"}, description = "Port that the server will listen to")
    private Integer port = 8080;

    @Parameter(names = {"-compressionLevel", "-c"}, description = "Compression level. 1 yields the fastest compressionLevel and 9 yields the best compressionLevel. 0 means no compressionLevel. The default is 1.")
    private Integer compressionLevel = 1;

    @Parameter(names = {"-workerThreads", "-wt"}, description = "Number of threads that will treat the incoming connections")
    private Integer numWorkerThreads = Runtime.getRuntime().availableProcessors() * 2;

    @Parameter(names = {"-executorThreads", "-et"}, description = "Number of threads that will do the actual process of the requests")
    private Integer numExecutorThreads = Runtime.getRuntime().availableProcessors() * 4;

    // Redis models.Options
    @Parameter(names = "-redisHost", description = "Redis host")
    private String redisHost = "localhost";

    @Parameter(names = "-redisPort", description = "Redis port")
    private int redisPort = Protocol.DEFAULT_PORT;

    @Parameter(names = "-maxCon", description = "Maximum active connections to Redis instance")
    private int maxActiveConnections = numExecutorThreads;

    @Parameter(names = "-maxIdle", description = "Number of connections to Redis that just sit there and do nothing")
    private int maxIdleConnections = maxActiveConnections / 2;

    @Parameter(names = "-minIdle", description = "Minimum number of idle connections to Redis (always open and ready to serve)")
    private int minIdleConnections = 1;

    @Parameter(names = "-numTests", description = "Maximum number of connections to test in each idle check")
    private int numTestsPerEvictionRun = maxActiveConnections;

    @Parameter(names = "-testWhileIdle", description = "Tests whether connections are dead during idle periods")
    private boolean testConnectionWhileIdle = true;

    @Parameter(names = "-testOnBorrow", description = "Tests whether connection is dead when connection retrieval method is called")
    private boolean testConnectionOnBorrow = true;

    @Parameter(names = "-testOnReturn", description = "Tests whether connection is dead when returning a connection to the pool")
    private boolean testConnectionOnReturn = false;

    @Parameter(names = "-timeBetweenEvictions", description = "Idle connection checking period")
    private long timeBetweenEvictionRunsMillis = 60000;

    @Parameter(names = "-minutesAlerts", description = "Minutes between executions of the alert sender job")
    private int minutesBetweenAlerts = 5;

    @Parameter(names = "-timeBetweenAlertMailsMillis", description = "Milliseconds between alert mails")
    private int timeBetweenAlertMailsMillis = 1000;

    public boolean isHelp() {
        return help;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getCompressionLevel() {
        return compressionLevel;
    }

    public Integer getNumWorkerThreads() {
        return numWorkerThreads;
    }

    public Integer getNumExecutorThreads() {
        return numExecutorThreads;
    }

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
                "help=" + help +
                ", port=" + port +
                ", compressionLevel=" + compressionLevel +
                ", numWorkerThreads=" + numWorkerThreads +
                ", numExecutorThreads=" + numExecutorThreads +
                ", redisHost='" + redisHost + '\'' +
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
                '}';
    }
}
