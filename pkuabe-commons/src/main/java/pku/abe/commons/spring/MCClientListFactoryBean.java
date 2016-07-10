/**
 *
 */
package pku.abe.commons.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.memcache.VikaCacheClient;

/**
 * 用于将mc端口列表直接转换成mc实例List<br/>
 * 多组mc之间用 | 号分割。<br/>
 * 如:<br/>
 * <code>
 * testmc1:11211,testmc2:11211|testmc3:11211
 * </code> 表示两组mc
 */
@SuppressWarnings("rawtypes")
public class MCClientListFactoryBean extends CacheableObjectFactoryBean<VikaCacheClient> {

    private String serverPorts;

    private MCClientFactoryBean.MCConfigStrategy strategy = MCClientFactoryBean.MCConfigStrategy.normal_mc;


    private Class targetListClass;

    private static final String KEY_PREFIX = "MCLIST_";

    /**
     * 默认启用开关
     */
    private boolean enableSwitcher = true;

    /**
     * Set the class to use for the target List. Can be populated with a fully qualified class name
     * when defined in a Spring application context.
     * <p>
     * Default is a <code>java.util.ArrayList</code>.
     *
     * @see java.util.ArrayList
     */
    public void setTargetListClass(Class targetListClass) {
        if (targetListClass == null) {
            throw new IllegalArgumentException("'targetListClass' must not be null");
        }
        if (!List.class.isAssignableFrom(targetListClass)) {
            throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
        }
        this.targetListClass = targetListClass;
    }


    public String getServerPorts() {
        return serverPorts;
    }

    public void setServerPorts(String serverPorts) {
        this.serverPorts = serverPorts;
    }

    public void setStrategy(String strategy) {
        this.strategy = MCClientFactoryBean.MCConfigStrategy.valueOf(strategy.toLowerCase());
    }

    @Override
    public Class getObjectType() {
        return List.class;
    }

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
                target.setSocketTimeOut(socketConnectTimeOut);
            }
        });

    }


    public void setEnableSwitcher(boolean enableSwitcher) {
        this.enableSwitcher = enableSwitcher;
    }

    @Override
    protected StringBuilder getKey() {
        StringBuilder buf = super.getKey();
        buf.append("-");
        buf.append(this.strategy.name());
        if (!StringUtils.isBlank(serverPorts)) {
            buf.append("-").append(this.serverPorts);
        }
        if (this.targetListClass != null) {
            buf.append("-").append(this.targetListClass.getName());
        }
        return buf;
    }


    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object doCreateInstance() throws Exception {
        if (StringUtils.isBlank(serverPorts)) {
            ApiLogger.info("MC-Config create empty MC Client list");
            return Collections.emptyList();
        }
        ApiLogger.info("MC-Config create MC Client list with servers:" + serverPorts);
        List result = null;
        if (this.targetListClass != null) {
            result = (List) BeanUtils.instantiateClass(this.targetListClass);
        } else {
            result = new ArrayList();
        }
        String[] serverPortsArray = this.serverPorts.split("\\|");
        for (String serverPort : serverPortsArray) {
            if (!StringUtils.isBlank(serverPort)) {
                VikaCacheClient client = new VikaCacheClient();
                this.strategy.setConfig(client);
                // 再使用自定义属性覆盖 策略配置
                for (BeanProperty<VikaCacheClient> p : this.properties) {
                    if (ApiLogger.isDebugEnabled()) {
                        ApiLogger.debug("MC-Config " + serverPort + " property:" + p.getName() + " value:" + p.getValue());
                    }
                    p.apply(client);
                }
                client.setServerPort(serverPort);
                client.setEnableSwitcher(this.enableSwitcher);
                client.init();
                result.add(client);
            }
        }
        return result;
    }

    @Override
    protected void destroyInstance(Object instance) throws Exception {
        if (instance == null) {
            return;
        }
        // List list = (List)instance;
        // for(Object obj:list){
        // VikaCacheClient client = (VikaCacheClient)obj;
        // client.close();
        // }
    }

    /**
     * set hashing algorithm for MCClientListFactory Bean
     *
     * @param hashingAlg
     */
    public void setHashingAlg(final String hashingAlg) {
        this.properties.add(new BeanProperty<VikaCacheClient>("hashingAlg", hashingAlg) {
            @Override
            public void apply(VikaCacheClient target) {
                MCClientFactoryBean.setHashingAlg4VikaClient(target, hashingAlg);
            }
        });
    }

}
