package pku.abe.commons.redis;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pku.abe.commons.redis.clients.jedis.Tuple;
import pku.abe.commons.redis.clients.jedis.exceptions.JedisException;
import pku.abe.commons.redis.clients.jedis.BinaryClient;


/**
 * @author fishermen
 * @author maijunsheng
 *         <p>
 *         2 JedisPorts, for master slave
 *         <p>
 *         write first server
 *         <p>
 *         read sencod server
 *         <p>
 *         <pre>
 *
 *                                                                                         JedisMSServer 与 JedisPort的关系是组装会更好，但是因为上层应用已经是普遍是注入JedisPort，而非JedisClient（JedisPort实现的接口）,
 *                                                                                         为避免太多变动，所以本次暂时不改这个地方，待上线稳定后，再来一次全面修改 fishermen 2012.9.10
 *
 *                                                                                         (1) master 写，而slave只读。
 *                                                                                         (2) 写的时候只写master，无论什么情况都不写slave，而读的时候，优先读slave，slave不可用的时候读master。
 *                                                                                         (3) 再提供方法 如， hgetFromMaster等，优先读取master（主要解决的事master slave的同步延迟场景无法适应cas的问题，比如有些场景是get,process,set，这些场景直接通过get master 能够拿到比较实时的结果）
 *                                                                                         (4) master是必设的，如果没有，那么init的时候直接报错，不让加载成功。如果想把slave配置成master而不想可写的话，通过setMaster(slaveServer), setReadOnly(true)。而无论什么情况,slave.setReadonly(true)，不受readonly的影响。
 *
 *                                                                                         </pre>
 */
public class JedisMSServer extends JedisPort {

    private RedisConfig masterConfig;
    private JedisPort master; // read and write
    private RedisConfig slaveConfig;
    private JedisPort slave; // read this when first is not alive

    public JedisMSServer() {
        super();
    }

    public JedisMSServer(final long hashMin, final long hashMax) {
        // super(poolConfig, hashMin, hashMax);
        this();
        this.hashMin = hashMin;
        this.hashMax = hashMax;
    }

    public void setPoolConfig(RedisConfig poolConfig) {
        super.setRedisConfig(poolConfig);
    }

    public void setMasterConfig(RedisConfig masterConfig) {
        this.masterConfig = masterConfig;
    }

    public void setSlaveConfig(RedisConfig slaveConfig) {
        this.slaveConfig = slaveConfig;
    }

    public JedisPort getMaster() {
        return master;
    }

    public void setMaster(JedisPort master) {
        this.master = master;
    }

    public JedisPort getSlave() {
        return slave;
    }

    public void setSlave(JedisPort slave) {
        this.slave = slave;
    }

    @Override
    public String getResourceInfo() {
        if (masterConfig != null && slaveConfig != null) {
            return masterConfig.getHostnamePort() + "/" + slaveConfig.getHostnamePort();
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
        if (master != null) {
            master.setReadOnly(readOnly);
        }
        if (slave != null) {
            slave.setReadOnly(true);
        }
    }

    /**
     * init-method
     */
    public void init() {
        if (this.masterConfig == null) {
            throw new RuntimeException("init jedisMSServer error");
        }

        this.throwJedisException = true;

        // master is need
        this.master = createConnect(this.masterConfig);
        this.master.throwJedisException = true;
        this.master.setReadOnly(readOnly);

        // just in case
        this.conn = this.master.conn;

        if (this.slaveConfig != null) {
            this.slave = createConnect(this.slaveConfig);
            this.slave.setReadOnly(true);
        }
    }

    /**
     * @param server ip:port[:db] db default 0
     * @return
     */
    private JedisPort createConnect(RedisConfig redisConfig) {
        log.info(String.format("Create jedis client to {}:{}:{}", redisConfig.getHostname(), redisConfig.getPort(),
                redisConfig.getDbName()));

        final JedisPort p = new JedisPort();
        p.setRedisConfig(redisConfig);
        p.init();
        return p;
    }

    public boolean isAlive() {
        boolean result = false;
        if (master != null) {
            result |= master.isAlive();
        }
        if (slave != null) {
            result |= slave.isAlive();
        }
        return result;
    }

    @Override
    public Long incr(byte[] key) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.incr(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long incr(final String id) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.incr(id);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.incrBy(key, integer);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long incrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return incrBy(bkey, integer);
    }

    @Override
    public Long decr(byte[] key) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.decr(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long decr(final String id) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.decr(id);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.decrBy(key, integer);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long decrBy(String key, long integer) {
        byte[] bkey = CodecHandler.encode(key);
        return decrBy(bkey, integer);
    }

    @Override
    @Deprecated
    public Boolean del(String key) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.del(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long del(byte[]... keys) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.del(keys);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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
        if (slave != null && slave.isAlive()) {
            return slave.exists(key);
        }
        if (master != null && master.isAlive()) {
            return master.exists(key);
        }
        log.warn(master + " [redis server all dead] exists , key " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Boolean existsFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.exists(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.exists(key);
        }
        log.warn(master + " [redis server all dead] existsFromMaster , key " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Boolean exists(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return exists(bkey);
    }

    public Boolean existsFromMaster(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return existsFromMaster(bkey);
    }

    public boolean expire(final String key, final int seconds) {
        if (master != null && master.isAlive()) {
            return master.expire(key, seconds);
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public boolean expireAt(String key, long unixTime) {
        if (master != null && master.isAlive()) {
            return master.expireAt(key, unixTime);
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long ttl(String key) {
        if (slave != null && slave.isAlive()) {
            return slave.ttl(key);
        }

        if (master != null && master.isAlive()) {
            return master.ttl(key);
        }
        log.warn(master + " [redis server all dead] ttl, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public boolean persist(String key) {
        if (master != null && master.isAlive()) {
            return master.persist(key);
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public String flushDB() {
        String result = null;
        if (master != null && master.isAlive()) {
            result = master.flushDB();
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    // //////////////////////////

    @Override
    public byte[] get(byte[] key) {
        if (slave != null && slave.isAlive()) {
            return slave.get(key);
        }

        if (master != null && master.isAlive()) {
            return master.get(key);
        }
        log.warn(master + " [redis server all dead] get, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public byte[] getFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.get(key);
        }

        if (slave != null && slave.isAlive()) {
            return slave.get(key);
        }

        log.warn(master + " [redis server all dead] getFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public String get(String key) {
        if (slave != null && slave.isAlive()) {
            return slave.get(key);
        }

        if (master != null && master.isAlive()) {
            return master.get(key);
        }

        log.warn(master + " [redis server all dead] get, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public String getFromMaster(String key) {
        if (master != null && master.isAlive()) {
            return master.get(key);
        }

        if (slave != null && slave.isAlive()) {
            return slave.get(key);
        }

        log.warn(master + " [redis server all dead] getFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Boolean hdel(byte[] key, byte[] field) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.hdel(key, field);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean hdel(final String key, final String field) {
        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hdel(bkey, bfield);
    }

    @Override
    public Boolean hdelAll(String key) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.hdelAll(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean hexists(String key, String field) {
        if (slave != null && slave.isAlive()) {
            return slave.hexists(key, field);
        }
        if (master != null && master.isAlive()) {
            return master.hexists(key, field);
        }
        log.warn(master + " [redis server all dead] hmget, keys: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    public Boolean hexistsFromMaster(String key, String field) {
        if (master != null && master.isAlive()) {
            return master.hexists(key, field);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hexists(key, field);
        }
        log.warn(master + " [redis server all dead] hexistsFromMaster, keys: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        if (slave != null && slave.isAlive()) {
            return slave.hget(key, field);
        }

        if (master != null && master.isAlive()) {
            return master.hget(key, field);
        }
        log.warn(master + " [redis server all dead] hget, key: " + CodecHandler.toStr(key) + ",field:" + CodecHandler.toStr(field));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public byte[] hgetFromMaster(byte[] key, byte[] field) {
        if (master != null && master.isAlive()) {
            return master.hget(key, field);
        }

        if (slave != null && slave.isAlive()) {
            return slave.hget(key, field);
        }

        log.warn(master + " [redis server all dead] hgetFromMaster, key: " + CodecHandler.toStr(key) + ",field:"
                + CodecHandler.toStr(field));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public String hget(final String key, final String field) {
        if (slave != null && slave.isAlive()) {
            return slave.hget(key, field);
        }

        if (master != null && master.isAlive()) {
            return master.hget(key, field);
        }
        log.warn(master + " [redis server all dead] hget, key: " + key + " field:" + field);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public String hgetFromMaster(final String key, final String field) {
        if (master != null && master.isAlive()) {
            return master.hget(key, field);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hget(key, field);
        }
        log.warn(master + " [redis server all dead] hgetFromMaster, key: " + key + " field:" + field);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        if (slave != null && slave.isAlive()) {
            return slave.hgetAll(key);
        }

        if (master != null && master.isAlive()) {
            return master.hgetAll(key);
        }
        log.warn(master + " [redis server all dead] hgetAll, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Map<byte[], byte[]> hgetAllFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.hgetAll(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hgetAll(key);
        }
        log.warn(master + " [redis server all dead] hgetAllFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    @Override
    public Map<String, String> hgetAll(String key) {
        if (slave != null && slave.isAlive()) {
            return slave.hgetAll(key);
        }
        if (master != null && master.isAlive()) {
            return master.hgetAll(key);
        }
        log.warn(master + " [redis server all dead] hgetAll, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Map<String, String> hgetAllFromMaster(String key) {
        if (master != null && master.isAlive()) {
            return master.hgetAll(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hgetAll(key);
        }
        log.warn(master + " [redis server all dead] hgetAllFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.hincrBy(key, field, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        byte[] bkey = CodecHandler.encode(key);
        byte[] bfield = CodecHandler.encode(field);
        return hincrBy(bkey, bfield, value);
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {

        if (slave != null && slave.isAlive()) {
            return slave.hkeys(key);
        }
        if (master != null && master.isAlive()) {
            return master.hkeys(key);
        }
        log.warn(master + " [redis server all dead] hkeys, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Set<byte[]> hkeysFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.hkeys(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hkeys(key);
        }
        log.warn(master + " [redis server all dead] hkeysFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<String> hkeys(final String key) {
        if (slave != null && slave.isAlive()) {
            return slave.hkeys(key);
        }
        if (master != null && master.isAlive()) {
            return master.hkeys(key);
        }
        log.warn(master + " [redis server all dead] hkeys, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Set<String> hkeysFromMaster(final String key) {
        if (master != null && master.isAlive()) {
            return master.hkeys(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hkeys(key);
        }
        log.warn(master + " [redis server all dead] hkeysFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long hlen(byte[] key) {
        if (slave != null && slave.isAlive()) {
            return slave.hlen(key);
        }
        if (master != null && master.isAlive()) {
            return master.hlen(key);
        }
        log.warn(master + " [redis server all dead] hlen, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Long hlenFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.hlen(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hlen(key);
        }
        log.warn(master + " [redis server all dead] hlenFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Long hlen(String key) {

        if (slave != null && slave.isAlive()) {
            return slave.hlen(key);
        }
        if (master != null && master.isAlive()) {
            return master.hlen(key);
        }
        log.warn(master + " [redis server all dead] hlen, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Long hlenFromMaster(String key) {
        if (master != null && master.isAlive()) {
            return master.hlen(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hlen(key);
        }
        log.warn(master + " [redis server all dead] hlenFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<Long> hmdelete(String key, List<String> fields) {
        List<Long> result = null;
        if (master != null && master.isAlive()) {
            result = master.hmdelete(key, fields);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public List<Boolean> hmexists(String key, List<String> fields) {
        if (slave != null && slave.isAlive()) {
            return slave.hmexists(key, fields);
        }
        if (master != null && master.isAlive()) {
            return master.hmexists(key, fields);
        }
        log.warn(master + " [redis server all dead] hmget, keys: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<Boolean> hmexistsFromMaster(String key, List<String> fields) {
        if (master != null && master.isAlive()) {
            return master.hmexists(key, fields);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmexists(key, fields);
        }
        log.warn(master + " [redis server all dead] hmexistsFromMaster, keys: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] mget, key: " + CodecHandler.toStr(key) + ",filed:" + CodecHandler.toString(fields));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<byte[]> hmgetFromMaster(byte[] key, byte[]... fields) {
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] hmgetFromMaster, key: " + CodecHandler.toStr(key) + ",filed:"
                + CodecHandler.toString(fields));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<String> hmget(List<List<String>> keys) {
        if (slave != null && slave.isAlive()) {
            return slave.hmget(keys);
        }
        if (master != null && master.isAlive()) {
            return master.hmget(keys);
        }
        log.warn(master + " [redis server all dead] hmget, keys: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hmgetFromMaster(List<List<String>> keys) {
        if (master != null && master.isAlive()) {
            return master.hmget(keys);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmget(keys);
        }
        log.warn(master + " [redis server all dead] hmgetFromMaster, keys: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    // ////////////////////

    @Override
    public List<String> hmget(String key, List<String> fields) {

        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] hmget, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hmgetFromMaster(String key, List<String> fields) {
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] hmgetFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<String> hmget(String key, String... fields) {

        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] mget, key: " + key + ",filed:" + fields);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hmgetFromMaster(String key, String... fields) {
        if (master != null && master.isAlive()) {
            return master.hmget(key, fields);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmget(key, fields);
        }
        log.warn(master + " [redis server all dead] hmgetFromMaster, key: " + key + ",filed:" + fields);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    @Override
    public List<String> hmgetPipeline(List<List<String>> keys) {

        if (slave != null && slave.isAlive()) {
            return slave.hmgetPipeline(keys);
        }
        if (master != null && master.isAlive()) {
            return master.hmgetPipeline(keys);
        }
        log.warn(master + " [redis server all dead] hmgetPipeline, keys: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hmgetPipelineFromMaster(List<List<String>> keys) {
        if (master != null && master.isAlive()) {
            return master.hmgetPipeline(keys);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hmgetPipeline(keys);
        }
        log.warn(master + " [redis server all dead] hmgetPipelineFromMaster, keys: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Boolean hmset(byte[] key, Map<byte[], byte[]> keyValueMap) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.hmset(key, keyValueMap);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean hmset(String key, Map<byte[], byte[]> keyValueMap) {
        byte[] bkey = CodecHandler.encode(key);
        return hmset(bkey, keyValueMap);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.hset(key, field, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.hset(key, field, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public <T extends Serializable> Long hset(String key, String field, T value) {
        byte[] bvalue = CodecHandler.encode(value);
        return hset(key, field, bvalue);
    }

    @Override
    public List<byte[]> hvals(byte[] key) {

        if (slave != null && slave.isAlive()) {
            return slave.hvals(key);
        }
        if (master != null && master.isAlive()) {
            return master.hvals(key);
        }
        log.warn(master + " [redis server all dead] hvals, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<byte[]> hvalsFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.hvals(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hvals(key);
        }
        log.warn(master + " [redis server all dead] hvalsFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hvals(final String key) {

        if (slave != null && slave.isAlive()) {
            return slave.hvals(key);
        }
        if (master != null && master.isAlive()) {
            return master.hvals(key);
        }
        log.warn(master + " [redis server all dead] hvals, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> hvalsFromMaster(final String key) {
        if (master != null && master.isAlive()) {
            return master.hvals(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.hvals(key);
        }
        log.warn(master + " [redis server all dead] hvalsFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Boolean lsset(String key, long[] values) {
        if (master != null && master.isAlive()) {
            return master.lsset(key, values);
        }
        throw new JedisException("master server dead: " + master);
    }

    /**
     * returns： 空字串：cache不存在 1：成功 0：不成功
     */
    @Override
    public Boolean lsput(String key, long... values) {
        if (master != null && master.isAlive()) {
            return master.lsput(key, values);
        }
        throw new JedisException("master server dead: " + master);
    }

    /**
     * returns： 空字串：cache不存在 1：成功 0：不成功
     */
    @Override
    public Boolean lsdel(String key, long... values) {
        if (master != null && master.isAlive()) {
            return master.lsdel(key, values);
        }
        throw new JedisException("master server dead: " + master);
    }

    /**
     * returns 和values一一对应；返回null说明cache不存在
     */
    @Override
    public Set<Long> lsmexists(String key, long... values) {
        if (slave != null && slave.isAlive()) {
            return slave.lsmexists(key, values);
        }
        if (master != null && master.isAlive()) {
            return master.lsmexists(key, values);
        }
        log.warn(master + " [redis server all dead] lsmexists, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Set<Long> lsmexistsFromMaster(String key, long... values) {
        if (master != null && master.isAlive()) {
            return master.lsmexists(key, values);
        }
        log.warn(master + " [redis server master dead] lsmexistsFromMaster, key: " + key);
        throw new JedisException("redis server dead: " + master);
    }

    @Override
    public Set<Long> lsgetall(String key) {
        if (slave != null && slave.isAlive()) {
            return slave.lsgetall(key);
        }
        if (master != null && master.isAlive()) {
            return master.lsgetall(key);
        }
        log.warn(master + " [redis server all dead] lsgetall, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Set<Long> lsgetallFromMaster(String key) {
        if (master != null && master.isAlive()) {
            return master.lsgetall(key);
        }
        log.warn(master + " [redis server master dead] lsgetallFromMaster, key: " + key);
        throw new JedisException("redis server dead: " + master);
    }

    @Override
    public int lslen(String key) {
        if (slave != null && slave.isAlive()) {
            return slave.lslen(key);
        }
        if (master != null && master.isAlive()) {
            return master.lslen(key);
        }
        log.warn(master + " [redis server all dead] lslen, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public int lslenFromMaster(String key) {
        if (master != null && master.isAlive()) {
            return master.lslen(key);
        }
        log.warn(master + " [redis server master dead] lslenFromMaster, key: " + key);
        throw new JedisException("redis server dead: " + master);
    }

    @Override
    public byte[] lindex(byte[] key, int index) {
        if (slave != null && slave.isAlive()) {
            return slave.lindex(key, index);
        }

        if (master != null && master.isAlive()) {
            return master.lindex(key, index);
        }
        log.warn(master + " [redis server all dead] lindex, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public byte[] lindexFromMaster(byte[] key, int index) {
        if (master != null && master.isAlive()) {
            return master.lindex(key, index);
        }
        if (slave != null && slave.isAlive()) {
            return slave.lindex(key, index);
        }
        log.warn(master + " [redis server all dead] lindexFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    @Override
    public String lindex(String key, int index) {

        if (slave != null && slave.isAlive()) {
            return slave.lindex(key, index);
        }
        if (master != null && master.isAlive()) {
            return master.lindex(key, index);
        }
        log.warn(master + " [redis server all dead] lindex, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public String lindexFromMaster(String key, int index) {
        if (master != null && master.isAlive()) {
            return master.lindex(key, index);
        }
        if (slave != null && slave.isAlive()) {
            return slave.lindex(key, index);
        }
        log.warn(master + " [redis server all dead] lindexFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long llen(byte[] key) {

        if (slave != null && slave.isAlive()) {
            return slave.llen(key);
        }
        if (master != null && master.isAlive()) {
            return master.llen(key);
        }
        log.warn(master + " [redis server all dead] llen, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public Long llenFromMaster(byte[] key) {
        if (master != null && master.isAlive()) {
            return master.llen(key);
        }
        if (slave != null && slave.isAlive()) {
            return slave.llen(key);
        }
        log.warn(master + " [redis server all dead] llenFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long llen(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return llen(bkey);
    }

    public Long llenFromMaster(String key) {
        byte[] bkey = CodecHandler.encode(key);
        return llenFromMaster(bkey);
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
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.lpush(key, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long lpush(byte[] key, byte[]... value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.lpush(key, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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

        if (slave != null && slave.isAlive()) {
            return slave.lrange(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.lrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<byte[]> lrangeFromMaster(byte[] key, int start, int end) {
        if (master != null && master.isAlive()) {
            return master.lrange(key, start, end);
        }
        if (slave != null && slave.isAlive()) {
            return slave.lrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrangeFromMaster, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<String> lrange(String key, int start, int end) {
        if (slave != null && slave.isAlive()) {
            return slave.lrange(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.lrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> lrangeFromMaster(String key, int start, int end) {
        if (master != null && master.isAlive()) {
            return master.lrange(key, start, end);
        }
        if (slave != null && slave.isAlive()) {
            return slave.lrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrangeFromMaster, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long lrem(byte[] key, int count, byte[] value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.lrem(key, count, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.lset(key, index, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.ltrim(key, start, end);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean ltrim(String key, int start, int end) {
        byte[] bkey = CodecHandler.encode(key);
        return ltrim(bkey, start, end);
    }

    @Override
    public List<byte[]> mget(byte[]... keys) {
        if (slave != null && slave.isAlive()) {
            return slave.mget(keys);
        }
        if (master != null && master.isAlive()) {
            return master.mget(keys);
        }
        log.warn(master + " [redis server all dead] mget, key: " + CodecHandler.toString(keys));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<byte[]> mgetFromMaster(byte[]... keys) {
        if (master != null && master.isAlive()) {
            return master.mget(keys);
        }
        if (slave != null && slave.isAlive()) {
            return slave.mget(keys);
        }
        log.warn(master + " [redis server all dead] mgetFromMaster, key: " + CodecHandler.toString(keys));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<String> mget(List<String> keys) {
        if (slave != null && slave.isAlive()) {
            return slave.mget(keys);
        }
        if (master != null && master.isAlive()) {
            return master.mget(keys);
        }
        log.warn(master + " [redis server all dead] mget, key: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    public List<String> mgetFromMaster(List<String> keys) {
        if (master != null && master.isAlive()) {
            return master.mget(keys);
        }
        if (slave != null && slave.isAlive()) {
            return slave.mget(keys);
        }
        log.warn(master + " [redis server all dead] mgetFromMaster, key: " + keys);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    @Override
    @Deprecated
    public Map<String, String> mgetMap(String... ids) {
        if (slave != null && slave.isAlive()) {
            return slave.mgetMap(ids);
        }
        if (master != null && master.isAlive()) {
            return master.mgetMap(ids);
        }
        log.warn(master + " [redis server all dead] mget, key: " + ids);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Deprecated
    public Map<String, String> mgetMapFromMaster(String... ids) {
        if (master != null && master.isAlive()) {
            return master.mgetMap(ids);
        }
        if (slave != null && slave.isAlive()) {
            return slave.mgetMap(ids);
        }
        log.warn(master + " [redis server all dead] mgetMapFromMaster, key: " + ids);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<String> mget(String... ids) {
        if (slave != null && slave.isAlive()) {
            return slave.mget(ids);
        }
        if (master != null && master.isAlive()) {
            return master.mget(ids);
        }
        log.warn(master + " [redis server all dead] mget, key: " + ids);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }


    public List<String> mgetByMaster(String... ids) {
        if (master != null && master.isAlive()) {
            return master.mget(ids);
        }
        if (slave != null && slave.isAlive()) {
            return slave.mget(ids);
        }
        log.warn(master + " [redis server all dead] mgetByMaster, key: " + ids);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Boolean mset(byte[]... keysvalues) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.mset(keysvalues);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
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
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.rpush(key, value);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long rpush(byte[] key, byte[]... value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.rpush(key, value);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
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
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.set(key, value);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
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
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.setex(key, seconds, value);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    /**
     * ================================================ methods for sorted set zset
     * ================================================
     */
    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zadd(key, score, member);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zadd(String key, Map<Double, String> scoreMidMap) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zadd(key, scoreMidMap);
        }

        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zrem(byte[] key, byte[] member) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zrem(key, member);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zrem(String key, String[] members) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zrem(key, members);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zremrangeByRank(byte[] key, int start, int end) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zremrangeByRank(key, start, end);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.zremrangeByScore(key, start, end);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        Double result = null;
        if (master != null && master.isAlive()) {
            result = master.zincrby(key, score, member);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        if (slave != null && slave.isAlive()) {
            return slave.zrank(key, member);
        }
        if (master != null && master.isAlive()) {
            return master.zrank(key, member);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key) + ", member: " + CodecHandler.toStr(member));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, int start, int end) {
        if (slave != null && slave.isAlive()) {
            return slave.zrange(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.zrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
        if (slave != null && slave.isAlive()) {
            return slave.zrangeWithScores(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.zrangeWithScores(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        if (slave != null && slave.isAlive()) {
            return slave.zrangeByScore(key, min, max);
        }
        if (master != null && master.isAlive()) {
            return master.zrangeByScore(key, min, max);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        if (slave != null && slave.isAlive()) {
            return slave.zrangeByScore(key, min, max, offset, count);
        }
        if (master != null && master.isAlive()) {
            return master.zrangeByScore(key, min, max, offset, count);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        if (slave != null && slave.isAlive()) {
            return slave.zrangeByScoreWithScores(key, min, max);
        }
        if (master != null && master.isAlive()) {
            return master.zrangeByScoreWithScores(key, min, max);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        if (slave != null && slave.isAlive()) {
            return slave.zrangeByScoreWithScores(key, min, max, offset, count);
        }
        if (master != null && master.isAlive()) {
            return master.zrangeByScoreWithScores(key, min, max, offset, count);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        if (slave != null && slave.isAlive()) {
            return slave.zrevrank(key, member);
        }
        if (master != null && master.isAlive()) {
            return master.zrevrank(key, member);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key) + ", member: " + CodecHandler.toStr(member));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, int start, int end) {
        if (slave != null && slave.isAlive()) {
            return slave.zrevrange(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.zrevrange(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
        if (slave != null && slave.isAlive()) {
            return slave.zrevrangeWithScores(key, start, end);
        }
        if (master != null && master.isAlive()) {
            return master.zrevrangeWithScores(key, start, end);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min, final int offset, final int count) {
        if (slave != null && slave.isAlive()) {
            return slave.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
        if (master != null && master.isAlive()) {
            return master.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long zcard(byte[] key) {
        if (slave != null && slave.isAlive()) {
            return slave.zcard(key);
        }
        if (master != null && master.isAlive()) {
            return master.zcard(key);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        if (slave != null && slave.isAlive()) {
            return slave.zcount(key, min, max);
        }
        if (master != null && master.isAlive()) {
            return master.zcount(key, min, max);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        if (slave != null && slave.isAlive()) {
            return slave.zscore(key, member);
        }
        if (master != null && master.isAlive()) {
            return master.zscore(key, member);
        }
        log.warn(master + " [redis server all dead] lrange, key: " + CodecHandler.toStr(key) + ", member: " + CodecHandler.toStr(member));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    /**
     * evalsha at master server
     * <p>
     * <pre>
     *
     * 	已经无法直视JedisPort相关衍生类了。。。
     *
     * </pre>
     *
     * @param sha1
     * @param keyCount
     * @param keys
     * @return
     */
    public Object evalshaAtMaster(final String sha1, final int keyCount, final String... keys) {
        Object result = null;
        if (master != null && master.isAlive()) {
            result = master.evalsha(sha1, keyCount, keys);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    /**
     * 对于update，有好大一只bug，因为该类操作的是slave。请用evalshaAtMaster替换
     */
    @Deprecated
    @Override
    public Object evalsha(final String sha1, int keyCount, final String... keys) {
        if (slave != null && slave.isAlive()) {
            return slave.evalsha(sha1, keyCount, keys);
        }
        if (master != null && master.isAlive()) {
            return master.evalsha(sha1, keyCount, keys);
        }
        log.warn(master + " [redis server all dead] evalsha, sha1: " + sha1);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public String scriptLoad(final String script) {
        String result = null;
        if (master != null && master.isAlive()) {
            result = master.scriptLoad(script);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public List<Object> pipeline(JedisPipelineReadCallback callback) {
        if (slave != null && slave.isAlive()) {
            return slave.pipeline(callback);
        }
        if (master != null && master.isAlive()) {
            return master.pipeline(callback);
        }
        log.warn(master + " [redis server all dead] pipeline");
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<Object> pipeline(JedisPipelineWriteCallback callback) {
        List<Object> result = null;
        if (master != null && master.isAlive()) {
            result = master.pipeline(callback);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public String lpop(String key) {
        String result = null;
        if (master != null && master.isAlive()) {
            result = master.lpop(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public byte[] lpop(byte[] key) {
        byte[] result = null;
        if (master != null && master.isAlive()) {
            result = master.lpop(key);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.linsert(key, where, pivot, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long linsert(final byte[] key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.linsert(key, where, pivot, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long linsert(final String key, final BinaryClient.LIST_POSITION where, final String pivot, final String value) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.linsert(key, where, pivot, value);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean rename(String oldkey, String newkey) {
        byte[] boldkey = CodecHandler.encode(oldkey);
        byte[] bnewkey = CodecHandler.encode(newkey);

        return rename(boldkey, bnewkey);
    }

    @Override
    public Boolean rename(final byte[] oldkey, final byte[] newkey) {
        Boolean result = null;
        if (master != null && master.isAlive()) {
            result = master.rename(oldkey, newkey);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long sadd(final String key, final String... members) {
        Long result = 0l;
        if (master != null && master.isAlive()) {
            result = master.sadd(key, members);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long srem(final String key, final String... members) {
        Long result = 0l;
        if (master != null && master.isAlive()) {
            result = master.srem(key, members);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Boolean sismember(String key, String member) {
        if (slave != null && slave.isAlive()) {
            return slave.sismember(key, member);
        }
        if (master != null && master.isAlive()) {
            return master.sismember(key, member);
        }
        log.warn(master + " [redis server all dead] exists , key " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long pfadd(String key, String... elements) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.pfadd(key, elements);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long pfcount(String... keys) {
        if (slave != null && slave.isAlive()) {
            return slave.pfcount(keys);
        }

        if (master != null && master.isAlive()) {
            return master.pfcount(keys);
        }
        log.warn(master + " [redis server all dead] get, key: " + Arrays.toString(keys));
        throw new JedisException("redis server all dead: " + master + " " + slave);

    }

    @Override
    public boolean pfmerge(String destkey, String... sourcekeys) {
        boolean result = false;
        if (master != null && master.isAlive()) {
            result = master.pfmerge(destkey, sourcekeys);
        }
        return result;
    }

    @Override
    public String getset(String key, String newValue) {
        // String result = null;
        if (master != null && master.isAlive()) {
            return master.getset(key, newValue);
        }
        // if (result != null) {
        // return result;
        // }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public byte[] getset(byte[] key, byte[] newValue) {
        // byte[] result = null;
        if (master != null && master.isAlive()) {
            return master.getset(key, newValue);
        }
        // if (result != null) {
        // return result;
        // }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public Long bfget(final String key) {
        if (slave != null && slave.isAlive()) {
            return slave.bfget(key);
        }

        if (master != null && master.isAlive()) {
            return master.bfget(key);
        }

        log.warn(master + " [redis server all dead] bfget, key: " + key);
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public Long bfset(final String key) {
        Long result = null;
        if (master != null && master.isAlive()) {
            result = master.bfset(key);
        }

        if (result != null) {
            return result;
        }

        throw new JedisException("master server dead: " + master);
    }

    @Override
    public List<Long> bfmget(final String... keys) {
        if (slave != null && slave.isAlive()) {
            return slave.bfmget(keys);
        }
        if (master != null && master.isAlive()) {
            return master.bfmget(keys);
        }
        log.warn(master + " [redis server all dead] bfmget, key: " + Arrays.toString(keys));
        throw new JedisException("redis server all dead: " + master + " " + slave);
    }

    @Override
    public List<Long> bfmset(final String... keys) {
        List<Long> result = null;
        if (master != null && master.isAlive()) {
            result = master.bfmset(keys);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public <V> V callUpdate(final JedisPortUpdateCallback<V> callback) {
        V result = null;
        if (master != null && master.isAlive()) {
            result = master.callUpdate(callback);
        }
        if (result != null) {
            return result;
        }
        throw new JedisException("master server dead: " + master);
    }

    @Override
    public synchronized void close() {
        if (this.master != null) {
            this.master.close();
        }
        if (this.slave != null) {
            this.slave.close();
        }
        super.close();
    }

    public String toString() {
        return "master:" + masterConfig.getServerPortDb() + ",slave=" + slaveConfig.getServerPortDb();
    }
}
