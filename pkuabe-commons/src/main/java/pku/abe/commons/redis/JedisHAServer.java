package pku.abe.commons.redis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pku.abe.commons.redis.clients.jedis.Tuple;
import pku.abe.commons.redis.clients.jedis.exceptions.JedisException;
import pku.abe.commons.redis.clients.jedis.BinaryClient;

/**
 * @author fishermen use clientBalancer
 * @author tangfulin
 *         <p>
 *         JedisMSServer 与
 *         JedisPort的关系是组装会更好，但是因为上层应用已经是普遍是注入JedisPort，而非JedisClient（JedisPort实现的接口）,
 *         为避免太多变动，所以本次暂时不改这个地方，待上线稳定后，再来一次全面修改 fishermen 2012.9.10
 *         <p>
 *         2 JedisPorts, for HA
 *         <p>
 *         read write first server, read the second when first not alive
 *         <p>
 *         (optional) write the second server
 */
public class JedisHAServer extends JedisPort {

    private List<RedisConfig> serverConfigs;
    private JedisPort first; // read and write
    private JedisPort second; // read this when first is not alive

    // do we need to write second ?
    boolean doubleWrite = false;
    // set return value of first ops to second?
    boolean setSecond = false;

    public JedisHAServer() {
        super();
    }

    public JedisHAServer(final long hashMin, final long hashMax) {
        // super(hashMin, hashMax);
        this();
        this.hashMin = hashMin;
        this.hashMax = hashMax;
    }

    public List<RedisConfig> getServerConfigs() {
        return serverConfigs;
    }

    public void setServerConfigs(List<RedisConfig> serverConfigs) {
        this.serverConfigs = serverConfigs;
    }

    public JedisPort getFirst() {
        return first;
    }

    // public void setFirst(JedisPort first) {
    // this.first = first;
    // }

    public JedisPort getSecond() {
        return second;
    }

    // public void setSecond(JedisPort second) {
    // this.second = second;
    // }

    public List<JedisPort> getClients() {
        List<JedisPort> clients = new ArrayList<JedisPort>();
        clients.add(first);
        clients.add(second);
        return clients;
    }

    public JedisPort getClient(int index) {
        if (index == 0) {
            return first;
        } else if (index == 1) {
            return second;
        } else {
            // TODO support more than 2 clients
            log.warn("not support more than 2 clients now");
            return null;
        }
    }

    public boolean isDoubleWrite() {
        return doubleWrite;
    }

    public boolean isSetSecond() {
        return setSecond;
    }

    public void setDoubleWrite(boolean doubleWrite) {
        this.doubleWrite = doubleWrite;
    }

    public void setSetSecond(boolean setSecond) {
        this.setSecond = setSecond;
    }

    @Override
    public String getResourceInfo() {
        if (first != null && second != null) {
            return first.getResourceInfo() + "/" + second.getResourceInfo();
        }
        return null;
    }

    public boolean isThrowJedisException() {
        return throwJedisException;
    }

    public void setThrowJedisException(boolean throwJedisException) {
        super.setThrowJedisException(throwJedisException);
        this.throwJedisException = throwJedisException;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.readOnly = readOnly;
        if (first != null) {
            first.setReadOnly(readOnly);
        }
        if (second != null) {
            second.setReadOnly(readOnly);
        }
    }

    /**
     * 改成 in spring init-method fishermen 2012.9.10
     */
    public void init() {
        if (this.serverConfigs == null) {
            throw new IllegalStateException("Not set serverConfigs for JedisHAServer ");
        }

        if (this.serverConfigs.size() < 1) {
            log.warn("server list empty!");
            throw new IllegalStateException("JedisHAServer's server list is empty!");
        }

        // this.firstServer = this.servers.get(0);
        this.first = createJedisPort(this.serverConfigs.get(0));
        this.first.throwJedisException = true;
        this.first.setReadOnly(readOnly);

        // just in case
        this.conn = this.first.conn;

        if (this.serverConfigs.size() > 1) {
            this.second = createJedisPort(this.serverConfigs.get(1));
            this.second.setReadOnly(readOnly);
        }

        if (this.serverConfigs.size() > 2) {
            // TODO impl more then 2 servers
            log.warn("not support for more then 2 servers for now: " + this.serverConfigs);
        }
    }

    /**
     * @param server ip:port[:db] db default 0
     * @return
     */
    public JedisPort createJedisPort(RedisConfig redisConfig) {
        log.info("init connection to " + redisConfig.getServerPortDb());

        // redisConfig.setServerPortDb(server);
        final JedisPort p = new JedisPort();
        p.setRedisConfig(redisConfig);
        p.init();
        return p;
    }

    public boolean isAlive() {
        boolean result = false;
        if (first != null) {
            result |= first.isAlive();
        }
        if (second != null) {
            result |= second.isAlive();
        }
        return result;
    }

    @Override
    public Long incr(byte[] key) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.incr(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(key, CodecHandler.encode(result));
                }
            } else {
                result2 = second.incr(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long incr(final String id) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.incr(id);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(id, result);
                }
            } else {
                result2 = second.incr(id);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.incrBy(key, integer);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(key, CodecHandler.encode(result));
                }
            } else {
                result2 = second.incrBy(key, integer);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long incrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return incrBy(bkey, integer);
    }

    @Override
    public Long decr(byte[] key) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.decr(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(key, CodecHandler.encode(result));
                }
            } else {
                result2 = second.decr(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long decr(final String id) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.decr(id);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(id, result);
                }
            } else {
                result2 = second.decr(id);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.decrBy(key, integer);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.set(key, CodecHandler.encode(result));
                }
            } else {
                result2 = second.decrBy(key, integer);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long decrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return decrBy(bkey, integer);
    }

    @Override
    @Deprecated
    public Boolean del(String key) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.del(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.del(key);
                }
            } else {
                result2 = second.del(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long del(byte[]... keys) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.del(keys);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.del(keys);
                }
            } else {
                result2 = second.del(keys);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long del(String... keys) {
        final byte[][] bkeys = new byte[keys.length][];
        for (int i = 0; i < bkeys.length; i++) {
            bkeys[i] = CodecHandler.encode(keys[i]);
        }
        return del(bkeys);
    }

    @Override
    public Boolean exists(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.exists(key);
        }
        if (second != null && second.isAlive()) {
            return second.exists(key);
        }
        log.warn(first + " [redis server all dead] exists , key " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean exists(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return exists(bkey);
    }

    public boolean expire(final String key, final int seconds) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.expire(key, seconds);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.expire(key, seconds);
                }
            } else {
                result2 = second.expire(key, seconds);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public boolean expireAt(final String key, final long unixTime) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.expireAt(key, unixTime);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.expireAt(key, unixTime);
                }
            } else {
                result2 = second.expireAt(key, unixTime);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);

    }

    @Override
    public Long ttl(String key) {
        if (first != null && first.isAlive()) {
            return first.ttl(key);
        }
        if (second != null && second.isAlive()) {
            return second.ttl(key);
        }
        log.warn(first + " [redis server all dead] get, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public boolean persist(final String key) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.persist(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.persist(key);
                }
            } else {
                result2 = second.persist(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public String flushDB() {
        String result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.flushDB();
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.flushDB();
                }
            } else {
                result2 = second.flushDB();
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    // //////////////////////////

    @Override
    public byte[] get(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.get(key);
        }
        if (second != null && second.isAlive()) {
            return second.get(key);
        }
        log.warn(first + " [redis server all dead] get, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    public String get(String key) {
        if (first != null && first.isAlive()) {
            return first.get(key);
        }
        if (second != null && second.isAlive()) {
            return second.get(key);
        }
        log.warn(first + " [redis server all dead] get, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean hdel(byte[] key, byte[] field) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hdel(key, field);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.hdel(key, field);
                }
            } else {
                result2 = second.hdel(key, field);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean hdel(final String key, final String field) {
        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hdel(bkey, bfield);
    }

    @Override
    public Boolean hdelAll(String key) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hdelAll(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.hdelAll(key);
                }
            } else {
                result2 = second.hdelAll(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean hexists(String key, String field) {
        if (first != null && first.isAlive()) {
            return first.hexists(key, field);
        }
        if (second != null && second.isAlive()) {
            return second.hexists(key, field);
        }
        log.warn(first + " [redis server all dead] hmget, keys: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        if (first != null && first.isAlive()) {
            return first.hget(key, field);
        }
        if (second != null && second.isAlive()) {
            return second.hget(key, field);
        }
        log.warn(first + " [redis server all dead] hget, key: " + CodecHandler.toStr(key) + ",field:" + CodecHandler.toStr(field));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public String hget(final String key, final String field) {
        if (first != null && first.isAlive()) {
            return first.hget(key, field);
        }
        if (second != null && second.isAlive()) {
            return second.hget(key, field);
        }
        log.warn(first + " [redis server all dead] hget, key: " + key + " field:" + field);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.hgetAll(key);
        }
        if (second != null && second.isAlive()) {
            return second.hgetAll(key);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        if (first != null && first.isAlive()) {
            return first.hgetAll(key);
        }
        if (second != null && second.isAlive()) {
            return second.hgetAll(key);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hincrBy(key, field, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    second.hset(key, field, CodecHandler.encode(result));
                }
            } else {
                result2 = second.hincrBy(key, field, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hincrBy(bkey, bfield, value);
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.hkeys(key);
        }
        if (second != null && second.isAlive()) {
            return second.hkeys(key);
        }
        log.warn(first + " [redis server all dead] hkeys, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<String> hkeys(final String key) {
        if (first != null && first.isAlive()) {
            return first.hkeys(key);
        }
        if (second != null && second.isAlive()) {
            return second.hkeys(key);
        }
        log.warn(first + " [redis server all dead] hkeys, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long hlen(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.hlen(key);
        }
        if (second != null && second.isAlive()) {
            return second.hlen(key);
        }
        log.warn(first + " [redis server all dead] hlen, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    public Long hlen(String key) {
        if (first != null && first.isAlive()) {
            return first.hlen(key);
        }
        if (second != null && second.isAlive()) {
            return second.hlen(key);
        }
        log.warn(first + " [redis server all dead] hlen, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Long> hmdelete(String key, List<String> fields) {
        List<Long> result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hmdelete(key, fields);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME be careful with this operation, first and second may
                // not be in consistent
                if (result != null) {
                    result2 = second.hmdelete(key, fields);
                }
            } else {
                result2 = second.hmdelete(key, fields);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Boolean> hmexists(String key, List<String> fields) {
        if (first != null && first.isAlive()) {
            return first.hmexists(key, fields);
        }
        if (second != null && second.isAlive()) {
            return second.hmexists(key, fields);
        }
        log.warn(first + " [redis server all dead] hmget, keys: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        if (first != null && first.isAlive()) {
            return first.hmget(key, fields);
        }
        if (second != null && second.isAlive()) {
            return second.hmget(key, fields);
        }
        log.warn(first + " [redis server all dead] mget, key: " + CodecHandler.toStr(key) + ",filed:" + CodecHandler.toString(fields));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> hmget(List<List<String>> keys) {
        if (first != null && first.isAlive()) {
            return first.hmget(keys);
        }
        if (second != null && second.isAlive()) {
            return second.hmget(keys);
        }
        log.warn(first + " [redis server all dead] hmget, keys: " + keys);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    // ////////////////////

    @Override
    public List<String> hmget(String key, List<String> fields) {
        if (first != null && first.isAlive()) {
            return first.hmget(key, fields);
        }
        if (second != null && second.isAlive()) {
            return second.hmget(key, fields);
        }
        log.warn(first + " [redis server all dead] hmget, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        if (first != null && first.isAlive()) {
            return first.hmget(key, fields);
        }
        if (second != null && second.isAlive()) {
            return second.hmget(key, fields);
        }
        log.warn(first + " [redis server all dead] mget, key: " + key + ",filed:" + fields);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> hmgetPipeline(List<List<String>> keys) {
        if (first != null && first.isAlive()) {
            return first.hmgetPipeline(keys);
        }
        if (second != null && second.isAlive()) {
            return second.hmgetPipeline(keys);
        }
        log.warn(first + " [redis server all dead] hmgetPipeline, keys: " + keys);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean hmset(byte[] key, Map<byte[], byte[]> keyValueMap) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hmset(key, keyValueMap);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.hmset(key, keyValueMap);
                }
            } else {
                result2 = second.hmset(key, keyValueMap);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean hmset(String key, Map<byte[], byte[]> keyValueMap) {
        byte[] bkey = CodecHandler.encode(key);
        return hmset(bkey, keyValueMap);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hset(key, field, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.hset(key, field, value);
                }
            } else {
                result2 = second.hset(key, field, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
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

    public Long hset(final String key, final String field, final String value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.hset(key, field, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.hset(key, field, value);
                }
            } else {
                result2 = second.hset(key, field, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public <T extends Serializable> Long hset(String key, String field, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return hset(key, field, bvalue);
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.hvals(key);
        }
        if (second != null && second.isAlive()) {
            return second.hvals(key);
        }
        log.warn(first + " [redis server all dead] hvals, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    public List<String> hvals(final String key) {
        if (first != null && first.isAlive()) {
            return first.hvals(key);
        }
        if (second != null && second.isAlive()) {
            return second.hvals(key);
        }
        log.warn(first + " [redis server all dead] hvals, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean lsset(String key, long[] values) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lsset(key, values);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.lsset(key, values);
                }
            } else {
                result2 = second.lsset(key, values);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    /**
     * returns： 空字串：cache不存在 1：成功 0：不成功
     */
    @Override
    public Boolean lsput(String key, long... values) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lsput(key, values);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.lsput(key, values);
                }
            } else {
                result2 = second.lsput(key, values);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    /**
     * returns： 空字串：cache不存在 1：成功 0：不成功
     */
    @Override
    public Boolean lsdel(String key, long... values) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lsdel(key, values);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.lsdel(key, values);
                }
            } else {
                result2 = second.lsdel(key, values);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    /**
     * returns 和values一一对应；返回null说明cache不存在
     */
    @Override
    public Set<Long> lsmexists(String key, long... values) {
        if (first != null && first.isAlive()) {
            return first.lsmexists(key, values);
        }
        if (second != null && second.isAlive()) {
            return second.lsmexists(key, values);
        }
        log.warn(first + " [redis server all dead] lsmexists, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<Long> lsgetall(String key) {
        if (first != null && first.isAlive()) {
            return first.lsgetall(key);
        }
        if (second != null && second.isAlive()) {
            return second.lsgetall(key);
        }
        log.warn(first + " [redis server all dead] lsgetall, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public int lslen(String key) {
        if (first != null && first.isAlive()) {
            return first.lslen(key);
        }
        if (second != null && second.isAlive()) {
            return second.lslen(key);
        }
        log.warn(first + " [redis server all dead] lslen, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public byte[] lindex(byte[] key, int index) {
        if (first != null && first.isAlive()) {
            return first.lindex(key, index);
        }
        if (second != null && second.isAlive()) {
            return second.lindex(key, index);
        }
        log.warn(first + " [redis server all dead] lindex, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public String lindex(String key, int index) {
        if (first != null && first.isAlive()) {
            return first.lindex(key, index);
        }
        if (second != null && second.isAlive()) {
            return second.lindex(key, index);
        }
        log.warn(first + " [redis server all dead] lrange, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long llen(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.llen(key);
        }
        if (second != null && second.isAlive()) {
            return second.llen(key);
        }
        log.warn(first + " [redis server all dead] llen, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long llen(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return llen(bkey);
    }

    /*
     * @Override public Long lpush(byte[] key, byte[]... values) { Long result = null, result2 =
     * null; if (first != null && first.isAlive()) { result = first.lpush(key,values); } if
     * (doubleWrite && second != null && second.isAlive()) { if (setSecond) { if (result != null) {
     * second.lpush(key,values); } } else { result2 = second.lpush(key,values); } }
     * if(result!=null){ return result; } if(result2 != null){ return result2; } throw new
     * JedisException("redis server all dead: " + first + " " + second); }
     */
    @Override
    public Long lpush(byte[] key, byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lpush(key, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.lpush(key, value);
                }
            } else {
                result2 = second.lpush(key, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long lpush(byte[] key, byte[]... value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lpush(key, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.lpush(key, value);
                }
            } else {
                result2 = second.lpush(key, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long lpush(String key, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lpush(bkey, value);
    }

    @Override
    public Long lpush(String key, byte[]... value) {
        byte[] bkey = CodecHandler.encode(key);
        return lpush(bkey, value);
    }

    @Override
    public Long lpush(String key, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lpush(key, bvalue);
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
    public <T extends Serializable> Long lpush(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lpush(key, bvalue);
    }

    @Override
    public List<byte[]> lrange(byte[] key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.lrange(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.lrange(key, start, end);
        }
        log.warn(first + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> lrange(String key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.lrange(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.lrange(key, start, end);
        }
        log.warn(first + " [redis server all dead] lrange, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long lrem(byte[] key, int count, byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lrem(key, count, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.lrem(key, count, value);
                }
            } else {
                result2 = second.lrem(key, count, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long lrem(String key, int count, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lrem(bkey, count, value);
    }

    @Override
    public Long lrem(String key, int count, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public Long lrem(String key, int count, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public <T extends Serializable> Long lrem(String key, int count, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lrem(key, count, bvalue);
    }

    @Override
    public Boolean lset(byte[] key, int index, byte[] value) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lset(key, index, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.lset(key, index, value);
                }
            } else {
                result2 = second.lset(key, index, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean lset(String key, int index, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return lset(bkey, index, value);
    }

    @Override
    public Boolean lset(String key, int index, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public Boolean lset(String key, int index, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public <T extends Serializable> Boolean lset(String key, int index, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return lset(key, index, bvalue);
    }

    @Override
    public Boolean ltrim(byte[] key, int start, int end) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.ltrim(key, start, end);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.ltrim(key, start, end);
                }
            } else {
                result2 = second.ltrim(key, start, end);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean ltrim(String key, int start, int end) {
        byte[] bkey = CodecHandler.encode(key);
        return ltrim(bkey, start, end);
    }

    @Override
    public List<byte[]> mget(byte[]... keys) {
        if (first != null && first.isAlive()) {
            return first.mget(keys);
        }
        if (second != null && second.isAlive()) {
            return second.mget(keys);
        }
        log.warn(first + " [redis server all dead] mget, key: " + CodecHandler.toString(keys));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> mget(List<String> keys) {
        if (first != null && first.isAlive()) {
            return first.mget(keys);
        }
        if (second != null && second.isAlive()) {
            return second.mget(keys);
        }
        log.warn(first + " [redis server all dead] mget, key: " + keys);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    @Deprecated
    public Map<String, String> mgetMap(String... ids) {
        if (first != null && first.isAlive()) {
            return first.mgetMap(ids);
        }
        if (second != null && second.isAlive()) {
            return second.mgetMap(ids);
        }
        log.warn(first + " [redis server all dead] mget, key: " + ids);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<String> mget(String... ids) {
        if (first != null && first.isAlive()) {
            return first.mget(ids);
        }
        if (second != null && second.isAlive()) {
            return second.mget(ids);
        }
        log.warn(first + " [redis server all dead] mget, key: " + ids);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean mset(byte[]... keysvalues) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.mset(keysvalues);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            result2 = second.mset(keysvalues);
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
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
    public Boolean mset(String... keysvalues) {
        final byte[][] bkeysvalues = new byte[keysvalues.length][];
        for (int i = 0; i < keysvalues.length; i++) {
            bkeysvalues[i] = CodecHandler.encode(keysvalues[i]);
        }
        return mset(bkeysvalues);
    }

    @Override
    public Long rpush(byte[] key, byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.rpush(key, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.rpush(key, value);
                }
            } else {
                result2 = second.rpush(key, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long rpush(byte[] key, byte[]... value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.rpush(key, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.rpush(key, value);
                }
            } else {
                result2 = second.rpush(key, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long rpush(String key, byte[] value) {
        byte[] bkey = CodecHandler.encode(key);
        return rpush(bkey, value);
    }

    @Override
    public Long rpush(String key, byte[]... value) {
        byte[] bkey = CodecHandler.encode(key);
        return rpush(bkey, value);
    }


    @Override
    public Long rpush(String key, Number value) {
        byte[] bvalue = CodecHandler.encode(value);
        return rpush(key, bvalue);
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
    public <T extends Serializable> Long rpush(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return rpush(key, bvalue);
    }

    @Override
    public Boolean set(byte[] key, byte[] value) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.set(key, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.set(key, value);
                }
            } else {
                result2 = second.set(key, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
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
    public Boolean set(String key, String value) {
        byte[] bvalue = CodecHandler.encode(value);
        return set(key, bvalue);
    }

    @Override
    public <T extends Serializable> Boolean set(String key, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return set(key, bvalue);
    }

    @Override
    public Boolean setex(String key, int seconds, String value) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.setex(key, seconds, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.setex(key, seconds, value);
                }
            } else {
                result2 = second.setex(key, seconds, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.zadd(key, score, member);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.zadd(key, score, member);
                }
            } else {
                result2 = second.zadd(key, score, member);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zrem(byte[] key, byte[] member) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.zrem(key, member);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.zrem(key, member);
                }
            } else {
                result2 = second.zrem(key, member);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zremrangeByRank(byte[] key, int start, int end) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.zremrangeByRank(key, start, end);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.zremrangeByRank(key, start, end);
                }
            } else {
                result2 = second.zremrangeByRank(key, start, end);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);

    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.zremrangeByScore(key, start, end);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.zremrangeByScore(key, start, end);
                }
            } else {
                result2 = second.zremrangeByScore(key, start, end);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        Double result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.zincrby(key, score, member);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.zincrby(key, score, member);
                }
            } else {
                result2 = second.zincrby(key, score, member);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        if (first != null && first.isAlive()) {
            return first.zrank(key, member);
        }
        if (second != null && second.isAlive()) {
            return second.zrank(key, member);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.zrange(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.zrange(key, start, end);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.zrangeWithScores(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.zrangeWithScores(key, start, end);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        if (first != null && first.isAlive()) {
            return first.zrangeByScore(key, min, max);
        }
        if (second != null && second.isAlive()) {
            return second.zrangeByScore(key, min, max);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        if (first != null && first.isAlive()) {
            return first.zrangeByScore(key, min, max, offset, count);
        }
        if (second != null && second.isAlive()) {
            return second.zrangeByScore(key, min, max, offset, count);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        if (first != null && first.isAlive()) {
            return first.zrangeByScoreWithScores(key, min, max);
        }
        if (second != null && second.isAlive()) {
            return second.zrangeByScoreWithScores(key, min, max);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        if (first != null && first.isAlive()) {
            return first.zrangeByScoreWithScores(key, min, max, offset, count);
        }
        if (second != null && second.isAlive()) {
            return second.zrangeByScoreWithScores(key, min, max, offset, count);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        if (first != null && first.isAlive()) {
            return first.zrevrank(key, member);
        }
        if (second != null && second.isAlive()) {
            return second.zrevrank(key, member);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.zrevrange(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.zrevrange(key, start, end);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
        if (first != null && first.isAlive()) {
            return first.zrevrangeWithScores(key, start, end);
        }
        if (second != null && second.isAlive()) {
            return second.zrevrangeWithScores(key, start, end);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zcard(byte[] key) {
        if (first != null && first.isAlive()) {
            return first.zcard(key);
        }
        if (second != null && second.isAlive()) {
            return second.zcard(key);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        if (first != null && first.isAlive()) {
            return first.zcount(key, min, max);
        }
        if (second != null && second.isAlive()) {
            return second.zcount(key, min, max);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        if (first != null && first.isAlive()) {
            return first.zscore(key, member);
        }
        if (second != null && second.isAlive()) {
            return second.zscore(key, member);
        }
        log.warn(first + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Object evalsha(final String sha1, final int keyCount, final String... keys) {
        throw new JedisException("JedisHAServer not support evalsha");
    }

    @Override
    public String scriptLoad(final String script) {
        String result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.scriptLoad(script);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.scriptLoad(script);
                }
            } else {
                result2 = second.scriptLoad(script);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Object> pipeline(JedisPipelineReadCallback callback) {
        if (first != null && first.isAlive()) {
            return first.pipeline(callback);
        }
        if (second != null && second.isAlive()) {
            return second.pipeline(callback);
        }
        log.warn(first + " [redis server all dead] pipeline");
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Object> pipeline(JedisPipelineWriteCallback callback) {
        List<Object> result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.pipeline(callback);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.pipeline(callback);
                }
            } else {
                result2 = second.pipeline(callback);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public String lpop(String key) {
        String result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lpop(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.lpop(key);
                }
            } else {
                result2 = second.lpop(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public byte[] lpop(byte[] key) {
        byte[] result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.lpop(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.lpop(key);
                }
            } else {
                result2 = second.lpop(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final String pivot, final String value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.linsert(key, where, pivot, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.linsert(key, where, pivot, value);
                }
            } else {
                result2 = second.linsert(key, where, pivot, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.linsert(key, where, pivot, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.linsert(key, where, pivot, value);
                }
            } else {
                result2 = second.linsert(key, where, pivot, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long linsert(final byte[] key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.linsert(key, where, pivot, value);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                // FIXME get list in first and set to second
                if (result != null) {
                    result2 = second.linsert(key, where, pivot, value);
                }
            } else {
                result2 = second.linsert(key, where, pivot, value);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Boolean rename(String oldkey, String newkey) {
        byte[] boldkey = CodecHandler.encode(oldkey);
        byte[] bnewkey = CodecHandler.encode(newkey);

        return rename(boldkey, bnewkey);
    }

    @Override
    public Boolean rename(final byte[] oldkey, final byte[] newkey) {
        Boolean result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.rename(oldkey, newkey);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.rename(oldkey, newkey);
                }
            } else {
                result2 = second.rename(oldkey, newkey);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long sadd(String key, String... members) {
        Long result = 0l, result2 = 0l;
        if (first != null && first.isAlive()) {
            result = first.sadd(key, members);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.sadd(key, members);
                }
            } else {
                result2 = second.sadd(key, members);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long srem(String key, String... members) {
        Long result = 0l, result2 = 0l;
        if (first != null && first.isAlive()) {
            result = first.srem(key, members);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.srem(key, members);
                }
            } else {
                result2 = second.srem(key, members);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    public Boolean sismember(String key, String member) {
        if (first != null && first.isAlive()) {
            return first.sismember(key, member);
        }
        if (second != null && second.isAlive()) {
            return second.sismember(key, member);
        }
        log.warn(first + " [redis server all dead] get, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long pfadd(String key, String... elements) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.pfadd(key, elements);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.pfadd(key, elements);
                }
            } else {
                result2 = second.pfadd(key, elements);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long pfcount(String... keys) {
        if (first != null && first.isAlive()) {
            return first.pfcount(keys);
        }
        if (second != null && second.isAlive()) {
            return second.pfcount(keys);
        }
        log.warn(first + " [redis server all dead] get, key: " + Arrays.toString(keys));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public boolean pfmerge(final String destkey, final String... sourcekeys) {
        boolean result = false, result2 = false;
        if (first != null && first.isAlive()) {
            result = first.pfmerge(destkey, sourcekeys);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result) {
                    result2 = second.pfmerge(destkey, sourcekeys);
                }
            } else {
                result2 = second.pfmerge(destkey, sourcekeys);
            }
        }
        if (result) {
            return result;
        }
        return result2;
    }

    @Override
    public String getset(final String key, final String newValue) {
        String result = null;
        String result2 = null;
        boolean firstDead = true;
        boolean secondDead = true;
        if (first != null && first.isAlive()) {
            result = first.getset(key, newValue);
            firstDead = false;
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.getset(key, newValue);
                }
            } else {
                result2 = second.getset(key, newValue);
            }
            secondDead = false;
        }
        if (!firstDead) {
            return result;
        }
        if (!secondDead) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public byte[] getset(final byte[] key, final byte[] newValue) {
        byte[] result = null;
        byte[] result2 = null;
        boolean firstDead = true;
        boolean secondDead = true;
        if (first != null && first.isAlive()) {
            result = first.getset(key, newValue);
            firstDead = false;
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.getset(key, newValue);
                }
            } else {
                result2 = second.getset(key, newValue);
            }
            secondDead = false;
        }
        if (!firstDead) {
            return result;
        }
        if (!secondDead) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long bfget(String key) {
        if (first != null && first.isAlive()) {
            return first.bfget(key);
        }
        if (second != null && second.isAlive()) {
            return second.bfget(key);
        }
        log.warn(first + " [redis server all dead] bfget, key: " + key);
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public Long bfset(String key) {
        Long result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.bfset(key);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.bfset(key);
                }
            } else {
                result2 = second.bfset(key);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Long> bfmget(String... keys) {
        if (first != null && first.isAlive()) {
            return first.bfmget(keys);
        }
        if (second != null && second.isAlive()) {
            return second.bfmget(keys);
        }
        log.warn(first + " [redis server all dead] bfmget, key: " + Arrays.toString(keys));
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public List<Long> bfmset(String... keys) {
        List<Long> result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.bfmset(keys);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.bfmset(keys);
                }
            } else {
                result2 = second.bfmset(keys);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }


    @Override
    public <V> V callUpdate(final JedisPortUpdateCallback<V> callback) {
        V result = null, result2 = null;
        if (first != null && first.isAlive()) {
            result = first.callUpdate(callback);
        }
        if (doubleWrite && second != null && second.isAlive()) {
            if (setSecond) {
                if (result != null) {
                    result2 = second.callUpdate(callback);
                }
            } else {
                result2 = second.callUpdate(callback);
            }
        }
        if (result != null) {
            return result;
        }
        if (result2 != null) {
            return result2;
        }
        throw new JedisException("redis server all dead: " + first + " " + second);
    }

    @Override
    public synchronized void close() {
        if (this.first != null) {
            this.first.close();
        }
        if (this.second != null) {
            this.second.close();
        }
        super.close();
    }

    public String toString() {
        if (serverConfigs.size() > 1) {
            return first.toString() + "... servers:" + serverConfigs;
        } else {
            return first.toString();
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        final List<RedisConfig> serverConfigs = new ArrayList<RedisConfig>();
        serverConfigs.add(new RedisConfig());
        serverConfigs.add(new RedisConfig());
        serverConfigs.get(0).setServerPortDb("10.75.0.109:7821:0");
        serverConfigs.get(1).setServerPortDb("10.75.0.109:7821:1");

        final JedisHAServer server = new JedisHAServer();
        server.setServerConfigs(serverConfigs);
        server.setDoubleWrite(true);
        server.init();

        // test get
        System.out.println(server.set("258.cntrm", "10"));
        System.out.println(server.get("258.cntrm"));
        System.out.println(server.incr("258.cntrm"));
        System.out.println(server.get("258.cntrm"));

    }
}
