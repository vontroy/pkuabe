/**
 *
 */
package pku.abe.commons.redis;

import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import pku.abe.commons.redis.clients.jedis.Jedis;
import pku.abe.commons.redis.clients.jedis.Pipeline;
import pku.abe.commons.redis.clients.jedis.Tuple;
import pku.abe.commons.redis.clients.jedis.exceptions.JedisConnectionException;
import pku.abe.commons.redis.clients.jedis.exceptions.JedisDataException;
import pku.abe.commons.redis.clients.jedis.exceptions.JedisException;
import pku.abe.commons.client.balancer.Endpoint;
import pku.abe.commons.client.balancer.EndpointPool;
import pku.abe.commons.client.balancer.impl.EndpointPoolImpl;
import pku.abe.commons.client.balancer.util.ClientBalancerStatLog;
import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.util.ApiUtil;
import pku.abe.commons.util.TimeStatUtil;
import pku.abe.commons.util.ResourceInfo;

import pku.abe.commons.profile.ProfileType;
import pku.abe.commons.profile.ProfileUtil;
import pku.abe.commons.redis.clients.jedis.BinaryClient;

/**
 * <pre>
 *
 *
 * 	实现一个AbstractJedisPort，然后继承就可以了，对于callback()变为abstract method，在这两个类继承各自实现。
 *
 *  不过需要进行详细的测试，同理对于JedisHAServer, JedisMSServer。
 *
 *  当然做了这一步的话，那么更建议再往前做一下，重构和优化JedisPort。
 *
 * </pre>
 */
public class JedisPort implements JedisClient, ResourceInfo {

    protected static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("jedis");

    private static final String REDIS_RET_OK = "OK";
    private static final int REDIS_SLOW_TIME = 50;
    private static int max_try_time = 2;

    private static final int DEFAULT_WRITE_RETRY = 1;

    int resource;
    int port;

    protected RedisConfig redisConfig;
    protected EndpointPool<Jedis> conn;

    boolean throwJedisException = false;
    boolean readOnly = false;

    public static final Long readOnlyDefaultLongValue = -1L;
    public static final Boolean readOnlyDefaultBooleanValue = false;
    public static final Double readOnlyDefaultDoubleValue = -1.0;

    protected long hashMin = -1L;
    protected long hashMax = -1L;

    public JedisPort() {}

    public JedisPort(final long hashMin, final long hashMax) {
        this();
        this.hashMin = hashMin;
        this.hashMax = hashMax;
        // must call setServer later
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public void init() {
        if (redisConfig == null) {
            throw new UnsupportedOperationException("Should set redisConfig before invoke jedisPort.init");
        }
        this.conn = new EndpointPoolImpl<Jedis>(new JedisEndpointFactory(redisConfig));
        try {
            port = redisConfig.getPort();
            resource = TimeStatUtil.REDIS_TYPE + port;
            TimeStatUtil.register(resource);
        } catch (Exception e) {
            // 防止port取到的是null
        }

    }

    public void setHashMin(final long hashMin) {
        this.hashMin = hashMin;
    }

    public long getHashMin() {
        return hashMin;
    }

    public EndpointPool<Jedis> getEndpointPool() {
        return conn;
    }

    public void setHashMax(final long hashMax) {
        this.hashMax = hashMax;
    }

    public long getHashMax() {
        return hashMax;
    }

    @Override
    public String getResourceInfo() {
        if (redisConfig != null) {
            return redisConfig.getHostnamePort();
        }
        return null;
    }

    public boolean isThrowJedisException() {
        return throwJedisException;
    }

    public void setThrowJedisException(boolean throwJedisException) {
        this.throwJedisException = throwJedisException;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void close() {
        if (this.conn == null) {
            return;
        }

        try {
            this.conn.close();
        } catch (Exception e) {
            log.warn("Error: when close the jedisport", e);
        }
    }

    @Override
    public boolean isAlive() {
        return this.conn.isAlive();
    }

    /**
     * check if id in [hashMin, hashMax)
     *
     * @param id
     * @return
     */
    public boolean contains(final long id) {
        if (hashMin >= 0 && id < hashMin) {
            return false;
        }
        if (hashMax >= 0 && id >= hashMax) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.conn.toString();
    }

    // ////////////

    @Override
    public boolean expire(final String key, final int seconds) {
        if (readOnly) {
            return false;
        }
        Long result = callable(new JedisPortCallback<Long>("expire", "", true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.expire(key, seconds);
            }
        });
        if (result == null || result != 1) {
            return false;
        }
        return true;
    }

    @Override
    public boolean expireAt(final String key, final long unixTime) {
        if (readOnly) {
            return false;
        }
        Long result = callable(new JedisPortCallback<Long>("expireat", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.expireAt(key, unixTime);
            }
        });
        if (result == null || result != 1) {
            return false;
        }
        return true;
    }

    @Override
    public Long ttl(final String key) {
        return callable(new JedisPortCallback<Long>("ttl", key, false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.ttl(key);
            }
        });
    }

    @Override
    public boolean persist(final String key) {
        if (readOnly) {
            return false;
        }
        Long result = callable(new JedisPortCallback<Long>("persist", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.persist(key);
            }
        });
        if (result == null || result != 1) {
            return false;
        }
        return true;
    }

    /**
     * please be careful to call this
     */
    public boolean flush() {
        String result = callable(new JedisPortCallback<String>("flush", "", false) {
            @Override
            public String call(Jedis jedis) {
                return jedis.flushDB();
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    public List<String> mget(final List<String> keys) {
        return callable(new JedisPortCallback<List<String>>("mget", keys.toString(), false) {
            @Override
            public List<String> call(Jedis jedis) {
                return jedis.mget(keys.toArray(new String[keys.size()]));
            }
        });
    }

    public List<String> hmget(final String key, final List<String> fields) {
        return callable(new JedisPortCallback<List<String>>("hmget", key, false) {
            @Override
            public List<String> call(Jedis jedis) {
                return jedis.hmget(key, fields.toArray(new String[fields.size()]));
            }
        });
    }

    /**
     * pipeline
     *
     * @param keys List of List size 2 : id, field
     * @return
     */
    public List<String> hmget(final List<List<String>> keys) {

        return hmgetPipeline(keys);
    }

    /**
     * pipeline
     *
     * @param keys List of List size 2 : id, field
     * @return
     */
    public List<String> hmgetPipeline(final List<List<String>> keys) {
        return callable(new JedisPortCallback<List<String>>("hmgetPipeline", keys.toString(), false) {
            @Override
            public List<String> call(Jedis jedis) {
                List<String> values = new ArrayList<String>();
                Pipeline pipeline = jedis.pipelined();

                for (List<String> pair : keys) {
                    final String id = pair.get(0);
                    final String field = pair.get(1);
                    pipeline.hget(id, field);
                }

                List<Object> result = pipeline.syncAndReturnAll();
                for (Object o : result) {
                    if (o != null) {
                        values.add((String) o);
                    } else {
                        values.add(null);
                    }
                }
                return values;
            }
        }, max_try_time, true);
    }

    @Override
    public String get(final String key) {
        return callable(new JedisPortCallback<String>("get", key, false) {
            @Override
            public String call(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    @Override
    public String hget(final String key, final String field) {
        return callable(new JedisPortCallback<String>("hget", key, false) {
            @Override
            public String call(Jedis jedis) {
                return jedis.hget(key, field);
            }
        });
    }

    @Override
    public Boolean set(final String key, final String value) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("set", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.set(key, value);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Long hset(final String key, final String field, final String value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("hset", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hset(key, field, value);
            }
        });
    }

    /**
     * delete multi fields in a hash
     *
     * @param
     * @return
     */
    public List<Long> hmdelete(final String key, final List<String> fields) {
        if (readOnly) {
            return new ArrayList<Long>();
        }
        return callable(new JedisPortCallback<List<Long>>("hmdelete", key, true) {
            @Override
            public List<Long> call(Jedis jedis) {
                List<Long> values = new ArrayList<Long>();
                Pipeline pipeline = jedis.pipelined();

                for (String field : fields) {
                    pipeline.hdel(key, field);
                }
                List<Object> result = pipeline.syncAndReturnAll();
                for (Object o : result) {
                    if (o != null) {
                        if (o instanceof Long) {
                            values.add((Long) o);
                        } else {
                            values.add(Long.parseLong((String) o));
                        }
                    } else {
                        values.add(null);
                    }
                }
                return values;
            }
        }, max_try_time, true);
    }

    @Deprecated
    public Boolean del(final String key) {
        if (readOnly) {
            return false;
        }
        return callable(new JedisPortCallback<Boolean>("del", key, true) {
            @Override
            public Boolean call(Jedis jedis) {
                jedis.del(key);
                return true;
            }
        });
    }

    @Override
    public Boolean hdel(final String key, final String field) {
        if (readOnly) {
            return false;
        }

        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hdel(bkey, bfield);
    }

    @Override
    public Long incr(final String key) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }

        if (key == null) {
            return null;
        }
        return callable(new JedisPortCallback<Long>("incr", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.incr(key);
            }
        }, 1);
    }

    @Override
    public Long hincrBy(final String key, final String field, final long value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("hincrBy", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hincrBy(key, field, value);
            }
        }, 1);
    }

    @Override
    public Long decr(final String key) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }

        if (key == null) {
            return null;
        }
        return callable(new JedisPortCallback<Long>("decr", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.decr(key);
            }
        }, 1);
    }

    @Override
    public Set<String> hkeys(final String key) {
        return callable(new JedisPortCallback<Set<String>>("hkeys", key, false) {
            @Override
            public Set<String> call(Jedis jedis) {
                return jedis.hkeys(key);
            }
        });
    }

    @Override
    public List<String> hvals(final String key) {
        return callable(new JedisPortCallback<List<String>>("hvals", key, false) {
            @Override
            public List<String> call(Jedis jedis) {
                return new ArrayList<String>(jedis.hvals(key));
            }
        });
    }

    @Override
    public Map<String, String> hgetAll(final String key) {
        return callable(new JedisPortCallback<Map<String, String>>("hgetAll", key, false) {
            @Override
            public Map<String, String> call(Jedis jedis) {
                return jedis.hgetAll(key);
            }
        }, max_try_time, true);
    }


    public Boolean hdelAll(final String key) {
        if (readOnly) {
            return false;
        }

        return del(key);
    }

    @Override
    public Long hlen(final String key) {
        return callable(new JedisPortCallback<Long>("hlen", key, false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hlen(key);
            }
        });
    }

    public Boolean hexists(final String key, final String field) {
        return callable(new JedisPortCallback<Boolean>("hexists", key, false) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.hexists(key, field);
            }
        });
    }

    /**
     * delete multi fields in a hash
     *
     * @param
     * @return
     */
    public List<Boolean> hmexists(final String key, final List<String> fields) {
        return callable(new JedisPortCallback<List<Boolean>>("hmexists", key, false) {
            @Override
            public List<Boolean> call(Jedis jedis) {
                List<Boolean> values = new ArrayList<Boolean>();
                Pipeline pipeline = jedis.pipelined();

                for (String field : fields) {
                    pipeline.hexists(key, field);
                }

                List<Object> result = pipeline.syncAndReturnAll();
                for (Object o : result) {
                    if (o != null) {
                        values.add((Boolean) o);
                    } else {
                        values.add(null);
                    }
                }
                return values;
            }
        }, max_try_time, true);
    }

    @Override
    public Boolean lsset(final String key, final long[] values) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("lsset", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.lsset(key, values);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean lsput(final String key, final long... values) {
        if (readOnly) {
            return false;
        }
        return callable(new JedisPortCallback<Boolean>("lsput", key, true) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.lsput(key, values);
            }
        });
    }

    @Override
    public Boolean lsdel(final String key, final long... values) {
        if (readOnly) {
            return false;
        }
        return callable(new JedisPortCallback<Boolean>("lsdel", key, true) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.lsdel(key, values);
            }
        });
    }

    @Override
    public Set<Long> lsmexists(final String key, final long... values) {
        return callable(new JedisPortCallback<Set<Long>>("lsmexists", key, false) {
            @Override
            public Set<Long> call(Jedis jedis) {
                return jedis.lsmexists(key, values);
            }
        });
    }

    @Override
    public Set<Long> lsgetall(final String key) {
        return callable(new JedisPortCallback<Set<Long>>("lsgetall", key, false) {
            @Override
            public Set<Long> call(Jedis jedis) {
                return jedis.lsgetall(key);
            }
        });
    }

    @Override
    public int lslen(final String key) {
        return callable(new JedisPortCallback<Integer>("lslen", key, false) {
            @Override
            public Integer call(Jedis jedis) {
                return jedis.lslen(key);
            }
        });
    }

    @Override
    public Boolean set(final byte[] key, final byte[] value) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("set", CodecHandler.toStr(key), true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.set(key, value);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean set(String key, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return set(bkey, value);
    }

    @Override
    public Boolean set(String key, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return set(key, bvalue);
    }

    @Override
    public <T extends Serializable> Boolean set(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return set(key, bvalue);
    }

    @Override
    public Boolean setex(final String key, final int seconds, final String value) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("setex", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.setex(key, seconds, value);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public byte[] get(final byte[] key) {
        return callable(new JedisPortCallback<byte[]>("get", CodecHandler.toStr(key), false) {
            @Override
            public byte[] call(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    @Override
    public Boolean exists(final byte[] key) {
        return callable(new JedisPortCallback<Boolean>("exists", CodecHandler.toStr(key), false) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.exists(key);
            }
        });
    }

    @Override
    public Boolean exists(final String key) {
        byte[] bkey = CodecHandler.encode(key);
        return exists(bkey);
    }

    @Override
    public Long del(final byte[]... keys) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("del", CodecHandler.toString(keys).toString(), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.del(keys);
            }
        });
    }

    @Override
    public Long del(String... keys) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }

        final byte[][] bkeys = new byte[keys.length][];
        for (int i = 0; i < bkeys.length; i++) {
            bkeys[i] = CodecHandler.encode(keys[i]);
        }
        return del(bkeys);
    }

    @Override
    public List<byte[]> mget(final byte[]... keys) {
        return callable(new JedisPortCallback<List<byte[]>>("mget", CodecHandler.toString(keys).toString(), false) {
            @Override
            public List<byte[]> call(Jedis jedis) {
                return jedis.mget(keys);
            }
        });
    }

    @Override
    @Deprecated
    public Map<String, String> mgetMap(String... keys) {
        List<String> ret = mget(keys);
        Map<String, String> map = null;
        if (ret != null) {
            map = new HashMap<String, String>();
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], ret.get(i));
            }
        }
        return map;
    }

    @Override
    public Boolean mset(final byte[]... keysvalues) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("mset", CodecHandler.toString(keysvalues).toString(), true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.mset(keysvalues);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean mset(String... keysvalues) {
        final byte[][] bkeysvalues = new byte[keysvalues.length][];
        for (int i = 0; i < keysvalues.length; i++) {
            bkeysvalues[i] = CodecHandler.encode(keysvalues[i]);
        }
        return mset(bkeysvalues);
    }

    @Override
    public Boolean mset(Map<byte[], byte[]> keyValueMap) {
        byte[][] bkeysvalues = new byte[keyValueMap.size() * 2][];
        int i = 0;
        for (Entry<byte[], byte[]> e : keyValueMap.entrySet()) {
            bkeysvalues[i++] = e.getKey();
            bkeysvalues[i++] = e.getValue();
        }
        return mset(bkeysvalues);
    }

    @Override
    public Long decrBy(final byte[] key, final long integer) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("decrBy", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.decrBy(key, integer);
            }
        }, 1);
    }

    @Override
    public Long decrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return decrBy(bkey, integer);
    }

    @Override
    public Long decr(final byte[] key) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("decr", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.decr(key);
            }
        }, 1);
    }

    @Override
    public Long incrBy(final byte[] key, final long integer) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }

        return callable(new JedisPortCallback<Long>("incrBy", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.incrBy(key, integer);
            }
        }, 1);
    }

    @Override
    public Long incrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return incrBy(bkey, integer);
    }

    @Override
    public Long incr(final byte[] key) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }

        return callable(new JedisPortCallback<Long>("incr", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.incr(key);
            }
        }, 1);
    }

    @Override
    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("hset", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hset(key, field, value);
            }
        });
    }

    @Override
    public Long hset(String key, String field, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hset(bkey, bfield, value);
    }

    @Override
    public Long hset(String key, String field, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return hset(key, field, bvalue);
    }

    @Override
    public <T extends Serializable> Long hset(String key, String field, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return hset(key, field, bvalue);
    }

    @Override
    public byte[] hget(final byte[] key, final byte[] field) {
        return callable(new JedisPortCallback<byte[]>("hget", CodecHandler.toStr(key), false) {
            @Override
            public byte[] call(Jedis jedis) {
                return jedis.hget(key, field);
            }
        });
    }

    @Override
    public Boolean hmset(final byte[] key, final Map<byte[], byte[]> keyValueMap) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("hmset", CodecHandler.toStr(key), true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.hmset(key, keyValueMap);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean hmset(String key, Map<byte[], byte[]> keyValueMap) {
        byte[] bkey = CodecHandler.encode(key);
        return hmset(bkey, keyValueMap);
    }

    @Override
    public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
        return callable(new JedisPortCallback<List<byte[]>>("hmget", CodecHandler.toStr(key), false) {
            @Override
            public List<byte[]> call(Jedis jedis) {
                return jedis.hmget(key, fields);
            }
        }, max_try_time, true);
    }

    @Override
    public List<String> hmget(final String key, final String... fields) {
        return callable(new JedisPortCallback<List<String>>("hmget", key, false) {
            @Override
            public List<String> call(Jedis jedis) {
                return jedis.hmget(key, fields);
            }
        }, max_try_time, true);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        String skey = CodecHandler.toStr(key);
        String sfield = CodecHandler.toStr(field);
        return hincrBy(skey, sfield, value);
    }

    @Override
    public Boolean hdel(final byte[] bkey, final byte[] bfield) {
        if (readOnly) {
            return false;
        }
        callable(new JedisPortCallback<Long>("hdel", CodecHandler.toStr(bkey), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hdel(bkey, bfield);
            }
        });
        return true;

    }

    @Override
    public Long hlen(final byte[] key) {
        return callable(new JedisPortCallback<Long>("hlen", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.hlen(key);
            }
        });
    }

    @Override
    public Set<byte[]> hkeys(final byte[] key) {
        return callable(new JedisPortCallback<Set<byte[]>>("hkeys", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.hkeys(key);
            }
        }, max_try_time, true);
    }

    @Override
    public List<byte[]> hvals(final byte[] key) {
        return callable(new JedisPortCallback<List<byte[]>>("hvals", CodecHandler.toStr(key), false) {
            @Override
            public List<byte[]> call(Jedis jedis) {
                return jedis.hvals(key);
            }
        });
    }

    @Override
    public Map<byte[], byte[]> hgetAll(final byte[] key) {
        return callable(new JedisPortCallback<Map<byte[], byte[]>>("hgetAll", CodecHandler.toStr(key), false) {
            @Override
            public Map<byte[], byte[]> call(Jedis jedis) {
                return jedis.hgetAll(key);
            }
        }, max_try_time, true);
    }

    @Override
    public Long rpush(final byte[] key, final byte[] value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("rpush", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.rpush(key, value);
            }
        });
    }

    @Override
    public Long rpush(final byte[] key, final byte[]... values) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("rpush", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.rpush(key, values);
            }
        });
    }

    @Override
    public Long rpush(String key, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return rpush(bkey, value);
    }

    @Override
    public Long rpush(String key, byte[]... values) {
        byte[] bkey = CodecHandler.encode(key);
        return rpush(bkey, values);
    }

    @Override
    public Long rpush(String key, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return rpush(key, bvalue);
    }

    @Override
    public Long rpush(String key, String... values) {
        byte[][] bvalues = new byte[values.length][];

        for (int i = 0; i < values.length; i++) {
            bvalues[i] = CodecHandler.encode(values[i]);
        }
        return rpush(key, bvalues);
    }

    @Override
    public Long rpush(String key, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return rpush(key, bvalue);
    }

    @Override
    public <T extends Serializable> Long rpush(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return rpush(key, bvalue);
    }

    @Override
    public Long lpush(final byte[] key, final byte[] value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("lpush", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.lpush(key, value);
            }
        });
    }

    @Override
    public Long lpush(final byte[] key, final byte[]... values) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("lpush", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.lpush(key, values);
            }
        });
    }

    @Override
    public Long lpush(String key, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lpush(bkey, value);
    }

    @Override
    public Long lpush(String key, byte[]... values) {
        byte[] bkey = CodecHandler.encode(key);
        return lpush(bkey, values);
    }

    @Override
    public Long lpush(String key, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lpush(key, bvalue);
    }

    @Override
    public Long lpush(String key, String... values) {
        byte[][] bvalues = new byte[values.length][];

        for (int i = 0; i < values.length; i++) {
            bvalues[i] = CodecHandler.encode(values[i]);
        }
        return lpush(key, bvalues);
    }

    @Override
    public Long lpush(String key, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lpush(key, bvalue);
    }

    @Override
    public <T extends Serializable> Long lpush(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lpush(key, bvalue);
    }

    @Override
    public Long llen(final byte[] key) {
        return callable(new JedisPortCallback<Long>("llen", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.llen(key);
            }
        });
    }

    @Override
    public Long llen(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return llen(bkey);
    }

    @Override
    public List<byte[]> lrange(final byte[] key, final int start, final int end) {
        return callable(new JedisPortCallback<List<byte[]>>("lrange", CodecHandler.toStr(key), false) {
            @Override
            public List<byte[]> call(Jedis jedis) {
                return jedis.lrange(key, start, end);
            }
        });
    }

    @Override
    public List<String> lrange(final String key, final int start, final int end) {
        return callable(new JedisPortCallback<List<String>>("lrange", key, false) {
            @Override
            public List<String> call(Jedis jedis) {
                return jedis.lrange(key, start, end);
            }
        });
    }

    @Override
    public Boolean ltrim(final byte[] key, final int start, final int end) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("ltrim", CodecHandler.toStr(key), true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.ltrim(key, start, end);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean ltrim(final String key, final int start, final int end) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("ltrim", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.ltrim(key, start, end);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public byte[] lindex(final byte[] key, final int index) {
        return callable(new JedisPortCallback<byte[]>("lindex", CodecHandler.toStr(key), false) {
            @Override
            public byte[] call(Jedis jedis) {
                return jedis.lindex(key, index);
            }
        });
    }

    @Override
    public String lindex(final String key, final int index) {
        return callable(new JedisPortCallback<String>("lindex", key, false) {
            @Override
            public String call(Jedis jedis) {
                return jedis.lindex(key, index);
            }
        });
    }

    @Override
    public Boolean lset(final byte[] key, final int index, final byte[] value) {
        if (readOnly) {
            return false;
        }
        String result = callable(new JedisPortCallback<String>("lset", CodecHandler.toStr(key), true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.lset(key, index, value);
            }
        });
        return REDIS_RET_OK.equalsIgnoreCase(result);
    }

    @Override
    public Boolean lset(String key, int index, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lset(bkey, index, value);
    }

    @Override
    public Boolean lset(String key, int index, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public Boolean lset(String key, int index, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public <T extends Serializable> Boolean lset(String key, int index, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public Long lrem(final byte[] key, final int count, final byte[] value) {
        return callable(new JedisPortCallback<Long>("lrem", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.lrem(key, count, value);
            }
        });
    }

    @Override
    public Long lrem(String key, int count, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lrem(bkey, count, value);
    }

    @Override
    public Long lrem(String key, int count, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public Long lrem(String key, int count, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public <T extends Serializable> Long lrem(String key, int count, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public String flushDB() {
        if (readOnly) {
            return null;
        }
        return callable(new JedisPortCallback<String>("flushDB", "", false) {
            @Override
            public String call(Jedis jedis) {
                return jedis.flushDB();
            }
        });
    }

    @Override
    public List<String> mget(final String... keys) {
        return callable(new JedisPortCallback<List<String>>("mget", Arrays.toString(keys), false) {
            @Override
            public List<String> call(Jedis jedis) {
                return jedis.mget(keys);
            }
        });
    }

    /**
     * ================================================ methods for sorted set zset
     * ================================================
     */
    @Override
    public Long zadd(final byte[] key, final double score, final byte[] member) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zadd", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zadd(key, score, member);
            }
        });
    }

    public Long zadd(final String key, final double score, final String member) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zadd", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zadd(key, score, member);
            }
        });
    }

    @Override
    public Long zadd(final String key, final Map<Double, String> scoreMembers) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zadd", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zadd(key, scoreMembers);
            }
        });
    }

    @Override
    public Long zrem(final byte[] key, final byte[] member) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zrem", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zrem(key, member);
            }
        });
    }

    @Override
    public Long zrem(final String key, final String[] members) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zrem", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zrem(key, members);
            }
        });
    }

    @Override
    public Long zremrangeByRank(final byte[] key, final int start, final int end) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zremrangeByRank", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zremrangeByRank(key, start, end);
            }
        });
    }

    @Override
    public Long zremrangeByScore(final byte[] key, final double start, final double end) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("zremrangeByScore", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zremrangeByScore(key, start, end);
            }
        });
    }

    @Override
    public Double zincrby(final byte[] key, final double score, final byte[] member) {
        if (readOnly) {
            return readOnlyDefaultDoubleValue;
        }
        return callable(new JedisPortCallback<Double>("zincrby", CodecHandler.toStr(key), true) {
            @Override
            public Double call(Jedis jedis) {
                return jedis.zincrby(key, score, member);
            }
        }, 1);
    }

    @Override
    public Set<byte[]> zrange(final byte[] key, final int start, final int end) {
        return callable(new JedisPortCallback<Set<byte[]>>("zrange", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.zrange(key, start, end);
            }
        });
    }

    @Override
    public Long zrank(final byte[] key, final byte[] member) {
        return callable(new JedisPortCallback<Long>("zrank", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zrank(key, member);
            }
        });
    }

    @Override
    public Long zrevrank(final byte[] key, final byte[] member) {
        return callable(new JedisPortCallback<Long>("zrevrank", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zrevrank(key, member);
            }
        });
    }

    @Override
    public Set<byte[]> zrevrange(final byte[] key, final int start, final int end) {
        return callable(new JedisPortCallback<Set<byte[]>>("zrevrange", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.zrevrange(key, start, end);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeWithScores(final byte[] key, final int start, final int end) {
        return callable(new JedisPortCallback<Set<Tuple>>("zrangeWithScores", CodecHandler.toStr(key), false) {
            @Override
            public Set<Tuple> call(Jedis jedis) {
                return jedis.zrangeWithScores(key, start, end);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(final byte[] key, final int start, final int end) {
        return callable(new JedisPortCallback<Set<Tuple>>("zrevrangeWithScores", CodecHandler.toStr(key), false) {
            @Override
            public Set<Tuple> call(Jedis jedis) {
                return jedis.zrevrangeWithScores(key, start, end);
            }
        });
    }

    @Override
    public Long zcard(final byte[] key) {
        return callable(new JedisPortCallback<Long>("zcard", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zcard(key);
            }
        });
    }

    @Override
    public Long scard(final byte[] key) {
        return callable(new JedisPortCallback<Long>("scard", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.scard(key);
            }
        });
    }

    @Override
    public Double zscore(final byte[] key, final byte[] member) {
        return callable(new JedisPortCallback<Double>("zscore", CodecHandler.toStr(key), false) {
            @Override
            public Double call(Jedis jedis) {
                return jedis.zscore(key, member);
            }
        });
    }

    @Override
    public Long zcount(final byte[] key, final double min, final double max) {
        return callable(new JedisPortCallback<Long>("zcount", CodecHandler.toStr(key), false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.zcount(key, min, max);
            }
        });
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
        return callable(new JedisPortCallback<Set<byte[]>>("zrangeByScore", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.zrangeByScore(key, min, max);
            }
        });
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max, final int offset, final int count) {
        return callable(new JedisPortCallback<Set<byte[]>>("zrangeByScore", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
        return callable(new JedisPortCallback<Set<Tuple>>("zrangeByScoreWithScores", CodecHandler.toStr(key), false) {
            @Override
            public Set<Tuple> call(Jedis jedis) {
                return jedis.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max, final int offset, final int count) {
        return callable(new JedisPortCallback<Set<Tuple>>("zrangeByScoreWithScores", CodecHandler.toStr(key), false) {
            @Override
            public Set<Tuple> call(Jedis jedis) {
                return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Object evalsha(final String sha1, final int keyCount, final String... keys) {
        return callable(new JedisPortCallback<Object>("evalsha", sha1, false) {
            @Override
            public Object call(Jedis jedis) {
                return jedis.evalsha(sha1, keyCount, keys);
            }
        }, 1);
    }

    @Override
    public String scriptLoad(final String script) {
        return callable(new JedisPortCallback<String>("scriptLoad", "", true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.scriptLoad(script);
            }
        }, DEFAULT_WRITE_RETRY);
    }

    /**
     * read request pipeline
     *
     * @param keys List of List size 2 : id, field
     * @return
     */
    @Override
    public List<Object> pipeline(JedisPipelineReadCallback callback) {
        return pipeline(callback, false);
    }

    /**
     * write request pipeline
     *
     * @param keys List of List size 2 : id, field
     * @return
     */
    @Override
    public List<Object> pipeline(JedisPipelineWriteCallback callback) {
        return pipeline(callback, true);
    }

    private List<Object> pipeline(final Object callback, final boolean write) {
        return callable(new JedisPortCallback<List<Object>>("pipeline", "", write) {
            @Override
            public List<Object> call(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                if (write) {
                    ((JedisPipelineWriteCallback) callback).call(new JedisWritePipelineAdapterImpl(pipeline));
                } else {
                    ((JedisPipelineReadCallback) callback).call(new JedisReadPipelineAdapterImpl(pipeline));
                }
                return pipeline.syncAndReturnAll();
            }
        });
    }

    @Override
    public String lpop(final String key) {
        return callable(new JedisPortCallback<String>("lpop", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.lpop(key);
            }
        });
    }

    @Override
    public byte[] lpop(final byte[] key) {
        return callable(new JedisPortCallback<byte[]>("lpop", CodecHandler.toStr(key), true) {
            @Override
            public byte[] call(Jedis jedis) {
                return jedis.lpop(key);
            }
        });
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return linsert(bkey, where, pivot, value);
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final String pivot, final String value) {
        byte[] bvalue = CodecHandler.encode(value);
        byte[] bpivot = CodecHandler.encode(pivot);

        return linsert(key, where, bpivot, bvalue);
    }

    @Override
    public Long linsert(final byte[] key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("linsert", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.linsert(key, where, pivot, value);
            }
        });
    }

    @Override
    public Boolean rename(final String oldkey, final String newkey) {
        byte[] boldkey = CodecHandler.encode(oldkey);
        byte[] bnewkey = CodecHandler.encode(newkey);

        return rename(boldkey, bnewkey);
    }

    @Override
    public Boolean rename(final byte[] oldkey, final byte[] newkey) {
        if (readOnly) {
            return false;
        }

        return callable(new JedisPortCallback<Boolean>("rename", CodecHandler.toStr(oldkey), true) {
            @Override
            public Boolean call(Jedis jedis) {
                String result = jedis.rename(oldkey, newkey);

                return REDIS_RET_OK.equalsIgnoreCase(result);
            }
        });
    }

    @Override
    public Long sadd(final String key, final String... members) {
        if (readOnly) {
            return 0l;
        }

        return callable(new JedisPortCallback<Long>("sadd", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.sadd(key, members);
            }
        });
    }

    @Override
    public Long sadd(final byte[] key, final byte[]... member) {
        if (readOnly) {
            return 0l;
        }
        return callable(new JedisPortCallback<Long>("sadd", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.sadd(key, member);
            }
        });
    }

    @Override
    public Long srem(final String key, final String... members) {
        if (readOnly) {
            return 0l;
        }

        return callable(new JedisPortCallback<Long>("srem", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.srem(key, members);
            }
        });
    }

    @Override
    public Long srem(final byte[] key, final byte[]... member) {
        if (readOnly) {
            return 0l;
        }
        return callable(new JedisPortCallback<Long>("srem", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.srem(key, member);
            }
        });
    }

    @Override
    public Boolean sismember(final String key, final String member) {
        return callable(new JedisPortCallback<Boolean>("sismember", key, false) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.sismember(key, member);
            }
        });
    }

    @Override
    public Set<byte[]> smember(final byte[] key) {
        return callable(new JedisPortCallback<Set<byte[]>>("smembers", CodecHandler.toStr(key), false) {
            @Override
            public Set<byte[]> call(Jedis jedis) {
                return jedis.smembers(key);
            }
        });
    }

    @Override
    public Boolean setbit(final String key, final long offset, final boolean value) {
        return callable(new JedisPortCallback<Boolean>("setbit", key, true) {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.setbit(key, offset, value);
            }
        });
    }

    @Override
    public Set<String> keys(final String pattern) {
        return callable(new JedisPortCallback<Set<String>>("keys", null, false) {
            @Override
            public Set<String> call(Jedis jedis) {
                return jedis.keys(pattern);
            }
        });
    }


    @Override
    public Long pfadd(final String key, final String... elements) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("pfadd", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.pfadd(key, elements);
            }
        });
    }

    @Override
    public Long pfcount(final String... keys) {
        return callable(new JedisPortCallback<Long>("pfcount", Arrays.toString(keys), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.pfcount(keys);
            }
        });
    }

    @Override
    public boolean pfmerge(final String destkey, final String... sourcekeys) {
        if (readOnly) {
            return readOnlyDefaultBooleanValue;
        }
        return callable(new JedisPortCallback<Boolean>("pfmerge", destkey + "_" + Arrays.toString(sourcekeys), true) {
            @Override
            public Boolean call(Jedis jedis) {
                return REDIS_RET_OK.equalsIgnoreCase(jedis.pfmerge(destkey, sourcekeys));
            }
        });
    }

    @Override
    public <V> V callUpdate(final JedisPortUpdateCallback<V> callback) {
        if (readOnly) {
            // 这里不考虑原生类型了，比如别人指定了 V => Integer, 然后 int a = batchUpdate(); null pointer，你懂的
            return null;
        }

        return callable(callback, DEFAULT_WRITE_RETRY);
    }

    // 将retry设置成可配置可调整的
    public static void setMaxTryTime(int retry) {
        max_try_time = retry;
    }

    // 默认重试，非批量，大部分都是非批量接口，区分开是为了做日志统计使用
    public <K> K callable(JedisPortCallback<K> callback) {
        return callable(callback, max_try_time);
    }

    // 非批量，主要用于incr decr等
    public <K> K callable(JedisPortCallback<K> callback, int tryTime) {
        return callable(callback, tryTime, false);
    }

    public <K> K callable(JedisPortCallback<K> callback, int tryTime, boolean isMulti) {
        K value = null;
        Endpoint<Jedis> jedis = null;
        long start = System.currentTimeMillis();
        long cost = -1;
        long end = start;
        int count = 0;
        try {
            while (count++ < tryTime) {
                try {
                    jedis = conn.borrowEndpoint();
                    value = callback.call(jedis.resourceClient);
                    cost = System.currentTimeMillis() - start;
                    ClientBalancerStatLog.incProcessTime("jedis." + callback.getName(), 1, cost);

                    if (log.isDebugEnabled() && cost < REDIS_SLOW_TIME) {
                        log.debug(this.toString() + " " + callback.getName() + ", key: " + callback.getKey() + " result:" + value);
                    } else if (cost >= REDIS_SLOW_TIME) {
                        ClientBalancerStatLog.inc("jedis.slowget");
                        log.warn(this.toString() + "_" + jedis.ipAddress + " " + callback.getName() + ", key: " + callback.getKey()
                                + " result:" + ApiUtil.truncateString(value, 500));
                    }
                    RedisLog.slowLog(this.toString(), cost);
                    conn.returnEndpoint(jedis);
                    break;
                } catch (JedisConnectionException jce) {
                    conn.invalidateEndpoint(jedis);
                    log.error(this + " " + callback.getName() + " fail:" + jce);
                } catch (JedisException je) {
                    if (isSpecialDataException(je)) {
                        // 连接还可用
                        conn.returnEndpoint(jedis);
                    } else {
                        conn.invalidateEndpoint(jedis);
                    }

                    log.error(this + " " + callback.getName() + " fail:" + je);
                    if (je.getCause() instanceof SocketTimeoutException) {
                        continue;
                    }
                    if (throwJedisException) {
                        throw je;
                    }
                    break;
                } catch (final Exception e) {
                    log.error(this + " " + callback.getName() + " error:", e);
                    conn.invalidateEndpoint(jedis);
                    break;
                }
            }
        } finally {
            int realPort = resource;
            if (isMulti) {
                realPort = resource + TimeStatUtil.MULTI_TYPE;
            }
            end = System.currentTimeMillis();
            // 从开始到finally中结束的时间，包括异常和重试时间
            cost = end - start;
            TimeStatUtil.addElapseTimeStat(realPort, callback.isWriter(), start, cost);
            String[] redisName = this.toString().split(" ")[0].split("-");
            if (redisName != null && redisName.length > 2) {
                String name = redisName[2];
                ProfileUtil.accessStatistic(ProfileType.REDIS.value(), name, end, cost, ApiLogger.REDIS_FIRE_TIME);
            }
        }
        return value;
    }

    /**
     * 某些异常是由于server回来的数据异常，这种场景下，server端的连接还可用，如果盲目close连接，会导致server被摘除, 从而导致其他问题，还有其他一些异常返回需要进行判断
     *
     * @throws Exception
     */
    public boolean isSpecialDataException(Exception e) {
        if (!(e instanceof JedisDataException)) {
            return false;
        }

        if (StringUtils.equals(e.getMessage(), "NOSCRIPT No matching script. Please use EVAL.")) {
            return true;
        }

        return false;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String getset(final String key, final String newValue) {
        return callable(new JedisPortCallback<String>("getset", key, true) {
            @Override
            public String call(Jedis jedis) {
                return jedis.getSet(key, newValue);
            }
        });
    }

    @Override
    public byte[] getset(final byte[] key, final byte[] newValue) {
        return callable(new JedisPortCallback<byte[]>("getset", CodecHandler.toStr(key), true) {
            @Override
            public byte[] call(Jedis jedis) {
                return jedis.getSet(key, newValue);
            }
        });
    }

    @Override
    public Long bfget(final String key) {
        return callable(new JedisPortCallback<Long>("bfget", key, false) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.bfget(key);
            }
        });
    }

    @Override
    public Long bfset(final String key) {
        if (readOnly) {
            return readOnlyDefaultLongValue;
        }
        return callable(new JedisPortCallback<Long>("bfset", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.bfset(key);
            }
        });
    }

    @Override
    public List<Long> bfmget(final String... keys) {
        return callable(new JedisPortCallback<List<Long>>("bfmget", Arrays.toString(keys), true) {
            @Override
            public List<Long> call(Jedis jedis) {
                return jedis.bfmget(keys);
            }
        });
    }

    @Override
    public List<Long> bfmset(final String... keys) {
        if (readOnly) {
            return null;
        }
        return callable(new JedisPortCallback<List<Long>>("bfmset", Arrays.toString(keys), true) {
            @Override
            public List<Long> call(Jedis jedis) {
                return jedis.bfmset(keys);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min, final int offset, final int count) {
        return callable(new JedisPortCallback<Set<Tuple>>("zrevrangeByScoreWithScores", CodecHandler.toStr(key), false) {
            @Override
            public Set<Tuple> call(Jedis jedis) {
                return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        });
    }

    @Override
    public Long setrange(final byte[] key, final long offset, final byte[] value) {
        return callable(new JedisPortCallback<Long>("setrange", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.setrange(key, offset, value);
            }
        });
    }

    @Override
    public Long setrange(final String key, final long offset, final String value) {
        return callable(new JedisPortCallback<Long>("setrange", key, true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.setrange(key, offset, value);
            }
        });
    }

    @Override
    public Long append(final byte[] key, final byte[] value) {
        return callable(new JedisPortCallback<Long>("append", CodecHandler.toStr(key), true) {
            @Override
            public Long call(Jedis jedis) {
                return jedis.append(key, value);
            }
        });
    }
}
