package pku.abe.commons.memcache;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.util.ResourceInfo;
import pku.abe.commons.util.TimeStatUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * use spring to manage life cycle, singleton design pattern
 *
 * @author Tim
 */
public class VikaCacheClient implements MemcacheClient, DisposableBean, ResourceInfo {
    protected final Logger log = Logger.getLogger(getClass());

    private MemCachedClient client = null;
    /**
     * should be 'server:port'
     */
    private String[] serverPort;
    private int resource;
    private int port;
    private String poolName;
    private int minSpareConnections = 25;
    private int maxSpareConnections = 35;
    private long compressThreshold = 512;
    private boolean compressEnable = true;
    private boolean consistentHashEnable = false;
    private boolean failover = true; // turn off auto-failover in event of server down
    private boolean sanitizeKeys = true; // encode key?

    public static final long DEFAULT_MAX_BUSY_TIME = 1000 * 15;
    public static final int DEFAULT_SOCKET_TIMEOUT = 1000 * 5;
    public static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 1000 * 5;

    private int hashingAlg = SockIOPool.NEW_COMPAT_HASH;

    // seconds to wait for collect all connection data(multiget)
    // change total timeout from 5min to 15 seconds
    // maxBusyTimePerConnection(timeout) is set in MemcachedClient.doMulti() (Ln 2369)
    // move as class field,support config.
    private long maxBusyTime = DEFAULT_MAX_BUSY_TIME;

    // seconds to block on reads
    private int socketTimeOut = DEFAULT_SOCKET_TIMEOUT;
    // seconds to block on initial
    private int socketConnectTimeOut = DEFAULT_SOCKET_CONNECT_TIMEOUT;

    // 默认启用开关
    private boolean enableSwitcher = true;
    // 创建socket时进行二次check，暂时默认不打开，与之前保持一致
    private boolean doubleCheckWhenCreateSocket = false;

    public void init() {
        // String[] serverlist = { serverPort };
        // Integer[] weights = { new Integer(5), new Integer(2) };
        int initialConnections = minSpareConnections;
        long maxIdleTime = 1000 * 60 * 30; // 30 minutes


        long maintThreadSleep = 1000 * 5; // 5 seconds

        // connections. If 0, then will use
        // blocking connect (default)

        // 打开nagle，意味着socket的tcp_nodelay会被设位false, nagle + pingpong mode 是最佳实践 fishermen 2015.12.18
        boolean nagleAlg = true;

        this.client.setCompressEnable(compressEnable);
        this.client.setCompressThreshold(compressThreshold);
        this.client.sanitizeKeys = sanitizeKeys;

        SockIOPool pool = SockIOPool.getInstance(poolName);
        if (serverPort == null || serverPort.length == 0) {
            throw new IllegalArgumentException("mc server port is empty");
        }
        pool.setDoubleCheckWhenCreateSocket(doubleCheckWhenCreateSocket);
        pool.setServers(serverPort);
        // pool.setWeights( weights );
        pool.setInitConn(initialConnections);
        pool.setMinConn(minSpareConnections);
        pool.setMaxConn(maxSpareConnections);
        pool.setMaxIdle(maxIdleTime);
        pool.setMaxBusyTime(maxBusyTime);
        pool.setMaintSleep(maintThreadSleep);
        pool.setSocketTO(socketTimeOut);
        pool.setSocketConnectTO(socketConnectTimeOut);
        pool.setNagle(nagleAlg);
        pool.setHashingAlg(consistentHashEnable ? SockIOPool.CONSISTENT_HASH : hashingAlg);
        pool.setFailover(failover);
        pool.setEnableSwitcher(this.enableSwitcher);
        try {
            pool.initialize();
            try {
                String portStr = serverPort[0].split(":")[1];
                port = Integer.parseInt(portStr);
                resource = TimeStatUtil.MC_TYPE + port;
                TimeStatUtil.register(resource);
            } catch (Exception e) {
                // 防止port取到的是null
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.fatal("memcached init exception, have not started? " + e.getMessage());
        }

    }

    public static final AtomicInteger poolCount = new AtomicInteger();

    public VikaCacheClient() {
        this.poolName = "MCSockPool-" + String.valueOf(poolCount.incrementAndGet());
        this.client = new MemCachedClient(poolName);
        // client.setPoolName(poolName);
    }

    public void setEnableSwitcher(boolean enableSwitcher) {
        this.enableSwitcher = enableSwitcher;
    }

    public boolean isEnableSwitcher() {
        return enableSwitcher;
    }

    @Override
    public String getResourceInfo() {
        return Arrays.toString(serverPort);
    }

    public boolean set(String key, Object value) {
        long start = System.currentTimeMillis();
        try {
            return client.set(key, value);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public boolean set(String key, Object value, Date expdate) {
        long start = System.currentTimeMillis();
        try {
            return client.set(key, value, expdate);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    /**
     * use set commond not cas
     */
    @Deprecated
    public boolean setCas(String key, CasValue<Object> value) {
        long start = System.currentTimeMillis();
        try {
            return client.setCas(key, value);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    @Deprecated
    public boolean setCas(String key, CasValue<Object> value, Date expdate) {
        long start = System.currentTimeMillis();
        try {
            return client.setCas(key, value, expdate);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    // ----------- fix setCas bug -----------------
    public boolean cas(String key, CasValue<Object> value) {
        long start = System.currentTimeMillis();
        try {
            return client.cas(key, value);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public boolean cas(String key, CasValue<Object> value, Date expdate) {
        long start = System.currentTimeMillis();
        try {
            return client.cas(key, value, expdate);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public boolean add(String key, Object value) {
        long start = System.currentTimeMillis();
        try {
            return client.add(key, value);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public boolean add(String key, Object value, Date expdate) {
        long start = System.currentTimeMillis();
        try {
            return client.add(key, value, expdate);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public long incr(String key) {
        long start = System.currentTimeMillis();
        try {
            return client.incr(key);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public long incr(String key, long inc) {
        long start = System.currentTimeMillis();
        try {
            return client.incr(key, inc);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public long decr(String key) {
        long start = System.currentTimeMillis();
        try {
            return client.decr(key);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public long decr(String key, int inc) {
        long start = System.currentTimeMillis();
        try {
            return client.decr(key, inc);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public boolean delete(String key) {
        long start = System.currentTimeMillis();
        try {
            return client.delete(key);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, true, start, -1);
        }
    }

    public Object get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        long start = System.currentTimeMillis();
        try {
            return client.get(key);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, false, start, -1);
        }
    }

    public CasValue<Object> gets(String key) {
        long start = System.currentTimeMillis();
        try {
            return client.gets(key);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, false, start, -1);
        }
    }

    public Map<String, Object> getMulti(String[] keys) {
        long start = System.currentTimeMillis();
        try {
            return client.getMulti(keys);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, false, start, -1);
        }
    }

    public Map<String, Object> getsMulti(String[] keys) {
        long start = System.currentTimeMillis();
        try {
            return client.getsMulti(keys);
        } finally {
            TimeStatUtil.addElapseTimeStat(resource, false, start, -1);
        }
    }

    public String getServerPort() {
        return serverPort[0];
    }

    public void setServerPort(String serverPort) {
        if (serverPort.indexOf(",") == -1)
            this.serverPort = new String[] {serverPort};
        else {
            this.serverPort = serverPort.split(",");
        }
        // init();
        ApiLogger.info("== Init Cache - " + serverPort + "\tcompressEnable:" + compressEnable);
    }

    public void flushAll() {
        client.flushAll();
    }

    public void setPrimitiveAsString(boolean v) {
        this.client.setPrimitiveAsString(v);
    }

    public boolean isPrimitiveAsString() {
        return this.client.isPrimitiveAsString();
    }

    @Override
    public void setServerPortList(String[] serverPort) {
        this.serverPort = serverPort;
        // init();
    }

    @Override
    public boolean append(String key, Object value) {
        return client.append(key, value);
    }

    public MemCachedClient getClient() {
        return client;
    }

    public void setClient(MemCachedClient client) {
        this.client = client;
    }

    public int getMinSpareConnections() {
        return minSpareConnections;
    }

    public void setMinSpareConnections(int minSpareConnections) {
        this.minSpareConnections = minSpareConnections;
    }

    public int getMaxSpareConnections() {
        return maxSpareConnections;
    }

    public void setMaxSpareConnections(int maxSpareConnections) {
        this.maxSpareConnections = maxSpareConnections;
    }

    public long getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(long compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    public boolean isCompressEnable() {
        return compressEnable;
    }

    public void setCompressEnable(boolean compressEnable) {
        this.compressEnable = compressEnable;
    }

    public boolean isConsistentHashEnable() {
        return consistentHashEnable;
    }

    public void setConsistentHashEnable(boolean consistentHashEnable) {
        this.consistentHashEnable = consistentHashEnable;
    }

    public boolean isFailover() {
        return failover;
    }

    public void setFailover(boolean failover) {
        this.failover = failover;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    public void setSocketConnectTimeOut(int socketConnectTimeOut) {
        this.socketConnectTimeOut = socketConnectTimeOut;
    }

    public int getSocketTimeOut() {
        return socketTimeOut;
    }

    public int getSocketConnectTimeOut() {
        return socketConnectTimeOut;
    }

    public long getMaxBusyTime() {
        return maxBusyTime;
    }

    public void setMaxBusyTime(long maxBusyTime) {
        this.maxBusyTime = maxBusyTime;
    }

    public String getPoolName() {
        return poolName;
    }

    public String toString() {
        return "VikaCacheClient@" + Arrays.toString(serverPort);
    }

    public void close() {
        this.client.close();
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.client.setErrorHandler(errorHandler);
    }

    @Override
    public void destroy() throws Exception {
        this.close();
    }

    public void setDoubleCheckWhenCreateSocket(boolean doubleCheckWhenCreateSocket) {
        this.doubleCheckWhenCreateSocket = doubleCheckWhenCreateSocket;
    }

    public boolean isAvailable() {
        return client.isAvailable();
    }

    public void setHashingAlg(int hashingAlg) {
        if (hashingAlg < SockIOPool.NATIVE_HASH || hashingAlg > SockIOPool.CONSISTENT_HASH) {
            ApiLogger.error("VikaCacheClient setHashingAlg error: need 0 ~ 3");
            return;
        }

        this.hashingAlg = hashingAlg;
    }

    public void setSanitizeKeys(boolean sanitizeKeys) {
        this.sanitizeKeys = sanitizeKeys;
    }
}
