package pku.abe.commons.redis;

import pku.abe.commons.client.balancer.Endpoint;
import pku.abe.commons.client.balancer.impl.EndpointBalancerConfig;
import pku.abe.commons.client.balancer.impl.EndpointFactory;
import pku.abe.commons.client.balancer.util.ClientBalancerLog;
import pku.abe.commons.redis.clients.jedis.Jedis;

/**
 * Used to create, destroy, validate redis endpoint .
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-10
 */

public class JedisEndpointFactory extends EndpointFactory<Jedis> {

    /**
     * redis的开关默认为true
     */
    private boolean enableSwitcher = true;

    public JedisEndpointFactory(EndpointBalancerConfig config) {
        super(config);
    }

    public boolean isEnableSwitcher() {
        return enableSwitcher;
    }

    public void setEnableSwitcher(boolean enableSwitcher) {
        this.enableSwitcher = enableSwitcher;
    }

    @Override
    public Endpoint<Jedis> doCreateEndpoint(String ip, EndpointBalancerConfig config) throws Exception {

        RedisConfig redisConfig = (RedisConfig) config;

        Jedis jedis = null;
        if (config.getSoTimeout() > 0) {
            jedis = this.enableSwitcher
                    ? new SwitcherSupportJedis(ip, config.getPort(), config.getSoTimeout())
                    : new WeiboJedis(ip, config.getPort(), config.getSoTimeout());
        } else {
            jedis = this.enableSwitcher ? new SwitcherSupportJedis(ip, config.getPort()) : new WeiboJedis(ip, config.getPort());
        }

        jedis.connect();
        try {
            jedis.select(redisConfig.getDbName());
            if (null != redisConfig.getPassword() && redisConfig.getPassword().length() > 0) {
                jedis.auth(redisConfig.getPassword());
            }
        } catch (Exception e) {
            doDestroyJedis(jedis);
            ClientBalancerLog.log.warn("jedis select or auth failed: " + jedis, e);
            throw e;
        }
        return new Endpoint<Jedis>(jedis, ip, redisConfig.getPort());
    }

    @Override
    public Endpoint<Jedis> doDestroyEndpoint(Endpoint<Jedis> endpoint) {
        doDestroyJedis(endpoint.resourceClient);
        return endpoint;
    }

    private void doDestroyJedis(Jedis jedis) {
        if (jedis != null && jedis.isConnected()) {
            try {
                try {
                    jedis.quit();
                } catch (Exception e) {
                    ClientBalancerLog.log.warn("jedis quit false: " + jedis + " " + e.getMessage());
                }
                jedis.disconnect();
            } catch (Exception e) {
                ClientBalancerLog.log.warn("jedis disconnect false: " + jedis, e);
            }
        }
    }

    @Override
    public boolean doValidateEndpoint(Endpoint<Jedis> endpoint) {
        final Jedis jedis = endpoint.resourceClient;
        try {
            return jedis.isConnected() && jedis.ping().equals("PONG");
        } catch (final Exception e) {
            return false;
        }
    }
}
