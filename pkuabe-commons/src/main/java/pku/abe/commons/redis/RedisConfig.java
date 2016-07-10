package pku.abe.commons.redis;

import pku.abe.commons.client.balancer.impl.EndpointBalancerConfig;

/**
 * redis config
 */

public class RedisConfig extends EndpointBalancerConfig {

    public static final int DEFAUTL_TIMEOUT = 500;
    public static final String DEFAULT_PASSWORD = null;
    public static final int DEFAULT_DBNAME = 0;

    private int timeout = DEFAUTL_TIMEOUT;
    private String password = DEFAULT_PASSWORD;
    private int dbName = DEFAULT_DBNAME;
    private String serverPortDb;

    public RedisConfig() {
        super();
    }

    public int getTimeout() {
        return timeout;
    }

    public String getPassword() {
        return password;
    }

    public int getDbName() {
        return dbName;
    }

    public String getServerPortDb() {
        if (serverPortDb == null) {
            serverPortDb = getHostname() + ":" + getPort() + ":" + this.dbName;
        }
        return serverPortDb;
    }

    public void setServerPortDb(String serverPortDb) {
        this.serverPortDb = serverPortDb;
        String[] parts = serverPortDb.split(":");
        setHostname(parts[0]);
        setPort(Integer.parseInt(parts[1]));
        if (parts.length > 2) {
            this.dbName = Integer.parseInt(parts[2]);
        }
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDbName(int dbName) {
        this.dbName = dbName;
    }


}
