/**
 *
 */
package pku.abe.commons.spring;

import org.apache.commons.lang.StringUtils;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.memcache.VikaCacheClient;

public class MCClientFactoryBean extends CacheableObjectFactoryBean<VikaCacheClient> {

    /**
     * default hashing algorithmname
     */
    public static final String DEFAULT_HASHING_ALGORITHM_NAME = "new_compat_hash";

    public static enum MCConfigStrategy {
        /**
         * 默认mc配置 /* compressEnable=true，consistentHashEnable=true，primitiveAsString=false
         */
        normal_mc(WfcXmlConstants.MC_DEFAULT_MIN_SPARE_CONNECTIONS, WfcXmlConstants.MC_DEFAULT_MAX_SPARE_CONNECTIONS, true, true, false,
                WfcXmlConstants.MC_DEFAULT_MAX_BUSY_TIME, WfcXmlConstants.MC_DEFAULT_SOCKET_TIMEOUT,
                WfcXmlConstants.MC_DEFAULT_SOCKET_CONNECT_TIMEOUT),

        /**
         * 默认mcq配置 compressEnable=false,consistentHashEnable=false,primitiveAsString=true
         */
        mcq(WfcXmlConstants.MC_DEFAULT_MIN_SPARE_CONNECTIONS, WfcXmlConstants.MC_DEFAULT_MAX_SPARE_CONNECTIONS, false, false, true,
                VikaCacheClient.DEFAULT_MAX_BUSY_TIME, VikaCacheClient.DEFAULT_SOCKET_TIMEOUT,
                VikaCacheClient.DEFAULT_SOCKET_CONNECT_TIMEOUT),

        /**
         * 默认mc counter配置 compressEnable=true，consistentHashEnable=true，primitiveAsString=true
         */
        counter_mc(WfcXmlConstants.MC_DEFAULT_MIN_SPARE_CONNECTIONS, WfcXmlConstants.MC_DEFAULT_MAX_SPARE_CONNECTIONS, true, true, true,
                WfcXmlConstants.MC_DEFAULT_MAX_BUSY_TIME, WfcXmlConstants.MC_DEFAULT_SOCKET_TIMEOUT,
                WfcXmlConstants.MC_DEFAULT_SOCKET_CONNECT_TIMEOUT);

        private int minSpareConnections;
        private int maxSpareConnections;
        private boolean compressEnable;
        private boolean consistentHashEnable;
        private boolean primitiveAsString;
        private long maxBusyTime;
        private int socketTimeOut;
        private int socketConnectTimeOut;

        private MCConfigStrategy(int minSpareConnections, int maxSpareConnections, boolean compressEnable, boolean consistentHashEnable,
                boolean primitiveAsString, long maxBusyTime, int socketTimeOut, int socketConnectTimeOut) {
            this.minSpareConnections = minSpareConnections;
            this.maxSpareConnections = maxSpareConnections;
            this.compressEnable = compressEnable;
            this.consistentHashEnable = consistentHashEnable;
            this.primitiveAsString = primitiveAsString;
            this.maxBusyTime = maxBusyTime;
            this.socketTimeOut = socketTimeOut;
            this.socketConnectTimeOut = socketConnectTimeOut;
        }

        public void setConfig(VikaCacheClient client) {
            client.setCompressEnable(this.compressEnable);
            client.setConsistentHashEnable(this.consistentHashEnable);
            client.setPrimitiveAsString(this.primitiveAsString);
            client.setMinSpareConnections(this.minSpareConnections);
            client.setMaxSpareConnections(this.maxSpareConnections);
            client.setFailover(true);
            // 影响multiget
            client.setMaxBusyTime(this.maxBusyTime);
            client.setSocketTimeOut(this.socketTimeOut);
            client.setSocketConnectTimeOut(this.socketConnectTimeOut);
        }
    }

    private static final String KEY_PREFIX = "MC_";


    private String serverPort;

    private MCConfigStrategy strategy = MCConfigStrategy.normal_mc;

    /**
     * 默认启用开关
     */
    private boolean enableSwitcher = true;


    public void setMinSpareConnections(final int minSpareConnections) {
        this.properties.add(new BeanProperty<VikaCacheClient>("minSpareConnections", minSpareConnections) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setMinSpareConnections(minSpareConnections);
            }
        });
    }


    public void setMaxSpareConnections(final int maxSpareConnections) {
        this.properties.add(new BeanProperty<VikaCacheClient>("maxSpareConnections", maxSpareConnections) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setMaxSpareConnections(maxSpareConnections);
            }
        });
    }

    public void setConsistentHashEnable(final boolean consistentHashEnable) {
        this.properties.add(new BeanProperty<VikaCacheClient>("consistentHashEnable", consistentHashEnable) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setConsistentHashEnable(consistentHashEnable);
            }
        });
    }


    public void setFailover(final boolean failover) {
        this.properties.add(new BeanProperty<VikaCacheClient>("failover", failover) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setFailover(failover);
            }
        });
    }


    public void setServerPort(final String serverPort) {
        this.serverPort = serverPort;
    }


    public void setPrimitiveAsString(final boolean primitiveAsString) {
        this.properties.add(new BeanProperty<VikaCacheClient>("primitiveAsString", primitiveAsString) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setPrimitiveAsString(primitiveAsString);
            }
        });
    }


    public void setCompressEnable(final boolean compressEnable) {
        this.properties.add(new BeanProperty<VikaCacheClient>("compressEnable", compressEnable) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setCompressEnable(compressEnable);
            }
        });
    }


    public void setMaxBusyTime(final long maxBusyTime) {
        this.properties.add(new BeanProperty<VikaCacheClient>("maxBusyTime", maxBusyTime) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setMaxBusyTime(maxBusyTime);
            }
        });
    }

    public void setSocketTimeOut(final int socketTimeOut) {
        this.properties.add(new BeanProperty<VikaCacheClient>("socketTimeOut", socketTimeOut) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setSocketTimeOut(socketTimeOut);
            }
        });
    }

    public void setSocketConnectTimeOut(final int socketConnectTimeOut) {
        this.properties.add(new BeanProperty<VikaCacheClient>("socketConnectTimeOut", socketConnectTimeOut) {
            @Override
            public void apply(VikaCacheClient target) {
                target.setSocketConnectTimeOut(socketConnectTimeOut);
            }
        });

    }

    public void setStrategy(String strategy) {
        this.strategy = MCConfigStrategy.valueOf(strategy.toLowerCase());
    }


    public void setEnableSwitcher(boolean enableSwitcher) {
        this.enableSwitcher = enableSwitcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cn.sina.api.commons.spring.CacheableObjectFactoryBean#getKey()
     */
    @Override
    protected StringBuilder getKey() {
        StringBuilder buf = super.getKey();
        buf.append("-").append(this.strategy.name());
        if (!StringUtils.isBlank(serverPort)) {
            buf.append("-").append(this.serverPort);
        }
        return buf;
    }


    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }


    /*
     * (non-Javadoc)
     * 
     * @see cn.sina.api.commons.spring.CacheableObjectFactoryBean#doCreateInstance()
     */
    @Override
    protected Object doCreateInstance() {
        if (StringUtils.isBlank(serverPort)) {
            ApiLogger.warn("create null MC Client.");
            return null;
        }
        ApiLogger.info("MC-Config create MC Client with server:" + serverPort);
        VikaCacheClient client = new VikaCacheClient();
        // 先使用配置策略
        this.strategy.setConfig(client);
        // 再使用自定义属性覆盖 策略配置
        for (BeanProperty<VikaCacheClient> p : this.properties) {
            if (ApiLogger.isDebugEnabled()) {
                ApiLogger.debug("MC-Config " + this.serverPort + " property:" + p.getName() + " value:" + p.getValue());
            }
            p.apply(client);
        }
        client.setServerPort(serverPort);
        client.setEnableSwitcher(enableSwitcher);

        client.init();
        return client;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class getObjectType() {
        return VikaCacheClient.class;
    }

    @Override
    protected void destroyInstance(Object instance) throws Exception {
        if (instance == null) {
            return;
        }
        // VikaCacheClient client = (VikaCacheClient)instance;
        // client.close();
    }

    /**
     * MemcacheClient's hash algorithm
     *
     * @author tongchuan
     */
    public static enum HashingAlg {
        NATIVE_HASH("native_hash", 0), OLD_COMPAT_HASH("old_compat_hash", 1), NEW_COMPAT_HASH("new_compat_hash",
                2), CONSISTENT_HASH("consistent_hash", 3);

        /**
         * hashing algorithm name
         */
        private String algorithmName;
        /**
         * hashing algorithm value
         */
        private int hashingAlg;

        private HashingAlg(String algorithmName, int hashingAlg) {
            this.algorithmName = algorithmName;
            this.hashingAlg = hashingAlg;
        }

        public String getAlgorithmName() {
            return this.algorithmName;
        }

        public int getHashingAlg() {
            return this.hashingAlg;
        }

        /**
         * parser the hashing algorithm according to the algorithmName
         *
         * @param algorithmName
         * @return
         */
        public static HashingAlg parser(String algorithmName) {
            for (HashingAlg hash : HashingAlg.values()) {
                if (algorithmName.equalsIgnoreCase(hash.getAlgorithmName())) {
                    return hash;
                }
            }
            ApiLogger.error("MemcacheClient does not support the " + algorithmName + " hashing algorithm,please check the property!");
            throw new IllegalArgumentException(" MemcacheClient does not support the " + algorithmName + " hashing algorithm !");
        }
    }

    /**
     * set hashing algorithm
     *
     * @param hashingAlg hashing algorithm name
     */
    public void setHashingAlg(final String hashingAlg) {
        this.properties.add(new BeanProperty<VikaCacheClient>("hashingAlg", hashingAlg) {
            @Override
            public void apply(VikaCacheClient target) {
                setHashingAlg4VikaClient(target, hashingAlg);
            }
        });

    }

    public static void setHashingAlg4VikaClient(VikaCacheClient target, final String hashingAlg) {
        // don't set hashing algorithm if the hashingAlg is empty
        if (StringUtils.isBlank(hashingAlg)) {
            return;
        }
        target.setHashingAlg(HashingAlg.parser(hashingAlg).getHashingAlg());
    }
}
