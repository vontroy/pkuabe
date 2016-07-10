/**
 *
 */
package pku.abe.commons.redis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pku.abe.commons.redis.clients.jedis.BinaryClient.LIST_POSITION;
import pku.abe.commons.redis.clients.jedis.BinaryJedisPubSub;
import pku.abe.commons.redis.clients.jedis.Client;
import pku.abe.commons.redis.clients.jedis.DebugParams;
import pku.abe.commons.redis.clients.jedis.JedisMonitor;
import pku.abe.commons.redis.clients.jedis.JedisPubSub;
import pku.abe.commons.redis.clients.jedis.JedisShardInfo;
import pku.abe.commons.redis.clients.jedis.Pipeline;
import pku.abe.commons.redis.clients.jedis.PipelineBlock;
import pku.abe.commons.redis.clients.jedis.Protocol;
import pku.abe.commons.redis.clients.jedis.SortingParams;
import pku.abe.commons.redis.clients.jedis.Transaction;
import pku.abe.commons.redis.clients.jedis.TransactionBlock;
import pku.abe.commons.redis.clients.jedis.Tuple;
import pku.abe.commons.redis.clients.jedis.ZParams;

import pku.abe.commons.switcher.ResourceSwitcherSupport;
import pku.abe.commons.switcher.ResourceSwitcherSupport.Callback;
import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;

import pku.abe.commons.log.ApiLogger;

public class SwitcherSupportJedis extends WeiboJedis {

    /**
     * redis资源开关统一命名规则 http://redmine.intra.weibo.com/projects/platform/wiki/Switcher
     */
    public static String REDIS_SWITCHER_PREFIX = "resource.redis.";

    static final String OK_REPLY = "OK";

    private ResourceSwitcherSupport switcherSupport;

    private static final String RESOURCE_TYPE = "redis";
    /**
     * the global write switcher of REDIS client. It's purpose is avoiding the touchstone system may
     * set dirty value to online's REDIS resources
     */
    private static final Switcher REDIS_CLIENT_GLOBAL_WRITE_SWITCHER = SwitcherManagerFactoryLoader.getSwitcherManagerFactory()
            .getSwitcherManager().registerSwitcher("feature.global.redis.resource.write", true);

    public SwitcherSupportJedis(JedisShardInfo shardInfo) {
        super(shardInfo);
        this.initSwitcher(shardInfo.getHost(), shardInfo.getPort());
    }

    public SwitcherSupportJedis(String host, int port, int timeout) {
        super(host, port, timeout);
        this.initSwitcher(host, port);
    }

    public SwitcherSupportJedis(String host, int port) {
        super(host, port);
        this.initSwitcher(host, port);
    }

    public SwitcherSupportJedis(String host) {
        super(host);
        this.initSwitcher(host, Protocol.DEFAULT_PORT);
    }

    private void initSwitcher(String host, int port) {
        this.switcherSupport = new ResourceSwitcherSupport(RESOURCE_TYPE, host, port);
    }

    public Switcher getReadSwitcher() {
        return this.switcherSupport.getReadSwitcher();
    }

    public Switcher getWriteSwitcher() {
        return this.switcherSupport.getWriteSwitcher();
    }

    public SwitcherManager getSwitcherMananger() {
        return this.switcherSupport.getSwitcherMananger();
    }

    public void setThrowSwitcherException(boolean throwSwitcherException) {
        this.switcherSupport.setThrowSwitcherException(throwSwitcherException);
    }

    private <T> T throwOrReturnValue(boolean isReadOp, T defaultValue, Callback<T> callback) {
        if (!isReadOp && REDIS_CLIENT_GLOBAL_WRITE_SWITCHER.isClose()) {
            ApiLogger.warn("global resource redis write switcher is close,return the defaultValue.");
            return defaultValue;
        }
        return this.switcherSupport.throwOrReturnValue(isReadOp, defaultValue, callback);
    }


    // ================ write 操作 =========================


    @Override
    public String set(final String key, final String value) {
        // 默认返回 OK_REPLY.
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.set(key, value);
            }
        });
    }

    @Override
    public String set(final byte[] key, final byte[] value) {
        // 默认返回 OK_REPLY.
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.set(key, value);
            }
        });
    }

    @Override
    public String getSet(final String key, final String value) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.getSet(key, value);
            }
        });
    }


    @Override
    public byte[] getSet(final byte[] key, final byte[] value) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.getSet(key, value);
            }
        });
    }

    @Override
    public Long setnx(final String key, final String value) {
        // 默认返回 0 表示set失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.setnx(key, value);
            }
        });
    }

    @Override
    public Long setnx(final byte[] key, final byte[] value) {
        // 默认返回 0 表示set失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.setnx(key, value);
            }
        });
    }

    @Override
    public Long del(final String... keys) {
        // 默认返回 0 表示未被删除
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.del(keys);
            }
        });
    }


    @Override
    public Long del(final byte[]... keys) {
        // 默认返回 0 表示未被删除
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.del(keys);
            }
        });
    }

    @Override
    public String rename(final String oldkey, final String newkey) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.rename(oldkey, newkey);
            }
        });
    }


    @Override
    public String rename(final byte[] oldkey, final byte[] newkey) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.rename(oldkey, newkey);
            }
        });
    }


    @Override
    public Long renamenx(final String oldkey, final String newkey) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.renamenx(oldkey, newkey);
            }
        });
    }

    @Override
    public Long renamenx(final byte[] oldkey, final byte[] newkey) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.renamenx(oldkey, newkey);
            }
        });
    }

    @Override
    public Long move(final String key, final int dbIndex) {
        // 默认返回 0,表示move失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.move(key, dbIndex);
            }
        });
    }


    @Override
    public Long move(final byte[] key, final int dbIndex) {
        // 默认返回 0,表示move失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.move(key, dbIndex);
            }
        });
    }


    @Override
    public String setex(final String key, final int seconds, final String value) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.setex(key, seconds, value);
            }
        });
    }

    @Override
    public String setex(final byte[] key, final int seconds, final byte[] value) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.setex(key, seconds, value);
            }
        });
    }

    @Override
    public String mset(final String... keysvalues) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.mset(keysvalues);
            }
        });
    }


    @Override
    public String mset(final byte[]... keysvalues) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.mset(keysvalues);
            }
        });
    }

    @Override
    public Long msetnx(final String... keysvalues) {
        // 默认返回 0,表示set失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.msetnx(keysvalues);
            }
        });
    }


    @Override
    public Long msetnx(final byte[]... keysvalues) {
        // 默认返回 0,表示set失败
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.msetnx(keysvalues);
            }
        });
    }

    @Override
    public Long decrBy(final String key, final long integer) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.decrBy(key, integer);
            }
        });
    }


    @Override
    public Long decrBy(final byte[] key, final long integer) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.decrBy(key, integer);
            }
        });
    }

    @Override
    public Long decr(final String key) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.decr(key);
            }
        });
    }

    @Override
    public Long decr(final byte[] key) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.decr(key);
            }
        });
    }

    @Override
    public Long incrBy(final String key, final long integer) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.incrBy(key, integer);
            }
        });
    }


    @Override
    public Long incrBy(final byte[] key, final long integer) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.incrBy(key, integer);
            }
        });
    }

    @Override
    public Long incr(final String key) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.incr(key);
            }
        });
    }

    @Override
    public Long incr(final byte[] key) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.incr(key);
            }
        });
    }


    @Override
    public Long hincrBy(final String key, final String field, final long value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hincrBy(key, field, value);
            }
        });
    }


    @Override
    public Long hincrBy(final byte[] key, final byte[] field, final long value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hincrBy(key, field, value);
            }
        });
    }

    @Override
    public Long append(final String key, final String value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.append(key, value);
            }
        });
    }


    @Override
    public Long append(final byte[] key, final byte[] value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.append(key, value);
            }
        });
    }


    @Override
    public Long hset(final String key, final String field, final String value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hset(key, field, value);
            }
        });
    }

    @Override
    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hset(key, field, value);
            }
        });
    }


    @Override
    public Long hsetnx(final String key, final String field, final String value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hsetnx(key, field, value);
            }
        });
    }

    @Override
    public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hsetnx(key, field, value);
            }
        });
    }

    @Override
    public String hmset(final String key, final Map<String, String> hash) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.hmset(key, hash);
            }
        });
    }


    @Override
    public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.hmset(key, hash);
            }
        });
    }

    @Override
    public Long hdel(final String key, final String... field) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hdel(key, field);
            }
        });
    }


    @Override
    public Long hdel(final byte[] key, final byte[]... field) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hdel(key, field);
            }
        });
    }

    @Override
    public Long rpush(final String key, final String... string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.rpush(key, string);
            }
        });
    }

    @Override
    public Long rpush(final byte[] key, final byte[]... string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.rpush(key, string);
            }
        });
    }

    @Override
    public Long lpush(final String key, final String... string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lpush(key, string);
            }
        });
    }


    @Override
    public Long lpush(final byte[] key, final byte[]... string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lpush(key, string);
            }
        });
    }


    @Override
    public Long rpushx(final String key, final String string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.rpushx(key, string);
            }
        });
    }


    @Override
    public Long rpushx(final byte[] key, final byte[] string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.rpushx(key, string);
            }
        });
    }

    @Override
    public Long lpushx(final String key, final String string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lpushx(key, string);
            }
        });
    }

    @Override
    public Long lpushx(final byte[] key, final byte[] string) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lpushx(key, string);
            }
        });
    }

    @Override
    public String ltrim(final String key, final long start, final long end) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.ltrim(key, start, end);
            }
        });
    }

    @Override
    public String ltrim(final byte[] key, final int start, final int end) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.ltrim(key, start, end);
            }
        });
    }

    @Override
    public String lset(final String key, final long index, final String value) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.lset(key, index, value);
            }
        });
    }


    @Override
    public String lset(final byte[] key, final int index, final byte[] value) {
        // 默认返回 OK_REPLY
        return throwOrReturnValue(false, OK_REPLY, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.lset(key, index, value);
            }
        });
    }

    @Override
    public Long lrem(final String key, final long count, final String value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lrem(key, count, value);
            }
        });
    }

    @Override
    public Long lrem(final byte[] key, final int count, final byte[] value) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.lrem(key, count, value);
            }
        });
    }

    @Override
    public String lpop(final String key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.lpop(key);
            }
        });
    }


    @Override
    public byte[] lpop(final byte[] key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.lpop(key);
            }
        });
    }


    @Override
    public String rpop(final String key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.rpop(key);
            }
        });
    }

    @Override
    public byte[] rpop(final byte[] key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.rpop(key);
            }
        });
    }

    @Override
    public String rpoplpush(final String srckey, final String dstkey) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.rpoplpush(srckey, dstkey);
            }
        });
    }

    @Override
    public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.rpoplpush(srckey, dstkey);
            }
        });
    }

    @Override
    public Long sadd(final String key, final String... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sadd(key, member);
            }
        });
    }


    @Override
    public Long sadd(final byte[] key, final byte[]... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sadd(key, member);
            }
        });
    }


    @Override
    public Long srem(final String key, final String... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.srem(key, member);
            }
        });
    }

    @Override
    public Long srem(final byte[] key, final byte[]... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.srem(key, member);
            }
        });
    }


    @Override
    public String spop(final String key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.spop(key);
            }
        });
    }


    @Override
    public byte[] spop(final byte[] key) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.spop(key);
            }
        });
    }

    @Override
    public Long smove(final String srckey, final String dstkey, final String member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.smove(srckey, dstkey, member);
            }
        });

    }


    @Override
    public Long smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.smove(srckey, dstkey, member);
            }
        });
    }

    @Override
    public Long sunionstore(final String dstkey, final String... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sinterstore(dstkey, keys);
            }
        });
    }

    @Override
    public Long sunionstore(final byte[] dstkey, final byte[]... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sinterstore(dstkey, keys);
            }
        });
    }

    @Override
    public Long sinterstore(final String dstkey, final String... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sinterstore(dstkey, keys);
            }
        });
    }


    @Override
    public Long sinterstore(final byte[] dstkey, final byte[]... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sinterstore(dstkey, keys);
            }
        });
    }

    @Override
    public Long sdiffstore(final String dstkey, final String... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sdiffstore(dstkey, keys);
            }
        });
    }


    @Override
    public Long sdiffstore(final byte[] dstkey, final byte[]... keys) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sdiffstore(dstkey, keys);
            }
        });
    }

    @Override
    public Long zadd(final String key, final double score, final String member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zadd(key, score, member);
            }
        });
    }


    @Override
    public Long zadd(final byte[] key, final double score, final byte[] member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zadd(key, score, member);
            }
        });
    }


    @Override
    public Long zadd(final String key, final Map<Double, String> scoreMembers) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zadd(key, scoreMembers);
            }
        });
    }

    @Override
    public Long zadd(final byte[] key, final Map<Double, byte[]> scoreMembers) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zadd(key, scoreMembers);
            }
        });
    }


    @Override
    public Long zrem(final String key, final String... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrem(key, member);
            }
        });
    }


    @Override
    public Long zrem(final byte[] key, final byte[]... member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrem(key, member);
            }
        });
    }

    @Override
    public Double zincrby(final String key, final double score, final String member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0D, new Callback<Double>() {
            @Override
            public Double call() {
                return SwitcherSupportJedis.super.zincrby(key, score, member);
            }
        });
    }


    @Override
    public Double zincrby(final byte[] key, final double score, final byte[] member) {
        // 默认返回 0
        return throwOrReturnValue(false, 0D, new Callback<Double>() {
            @Override
            public Double call() {
                return SwitcherSupportJedis.super.zincrby(key, score, member);
            }
        });
    }


    @Override
    public Long sort(final String key, final SortingParams sortingParameters, final String dstkey) {
        // 默认返回 0, 本sort操作会写数据
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sort(key, sortingParameters, dstkey);
            }
        });
    }

    @Override
    public Long sort(final String key, final String dstkey) {
        // 默认返回 0, 本sort操作会写数据
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sort(key, dstkey);
            }
        });
    }


    @Override
    public Long sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
        // 默认返回 0, 本sort操作会写数据
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sort(key, sortingParameters, dstkey);
            }
        });
    }

    @Override
    public Long sort(final byte[] key, final byte[] dstkey) {
        // 默认返回 0, 本sort操作会写数据
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.sort(key, dstkey);
            }
        });
    }


    @Override
    public List<String> blpop(final int timeout, final String... keys) {
        // 默认返回 null 表示超时
        return throwOrReturnValue(false, null, new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.blpop(timeout, keys);
            }
        });
    }


    @Override
    public List<byte[]> blpop(final int timeout, final byte[]... keys) {
        // 默认返回 null 表示超时
        return throwOrReturnValue(false, null, new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.blpop(timeout, keys);
            }
        });
    }

    @Override
    public String brpoplpush(final String source, final String destination, final int timeout) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.brpoplpush(source, destination, timeout);
            }
        });
    }

    @Override
    public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
        // 默认返回 null
        return throwOrReturnValue(false, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.brpoplpush(source, destination, timeout);
            }
        });
    }


    @Override
    public List<String> brpop(final int timeout, final String... keys) {
        // 默认返回 null 表示超时
        return throwOrReturnValue(false, null, new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.brpop(timeout, keys);
            }
        });
    }


    @Override
    public List<byte[]> brpop(final int timeout, final byte[]... keys) {
        // 默认返回 null 表示超时
        return throwOrReturnValue(false, null, new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.brpop(timeout, keys);
            }
        });
    }


    @Override
    public Long zremrangeByRank(final String key, final long start, final long end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByRank(key, start, end);
            }
        });
    }

    @Override
    public Long zremrangeByRank(final byte[] key, final int start, final int end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByRank(key, start, end);
            }
        });
    }


    @Override
    public Long zremrangeByScore(final String key, final double start, final double end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByScore(key, start, end);
            }
        });
    }

    @Override
    public Long zremrangeByScore(final byte[] key, final double start, final double end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByScore(key, start, end);
            }
        });
    }

    @Override
    public Long zremrangeByScore(final byte[] key, final byte[] start, final byte[] end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByScore(key, start, end);
            }
        });
    }


    @Override
    public Long zremrangeByScore(final String key, final String start, final String end) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zremrangeByScore(key, start, end);
            }
        });
    }

    @Override
    public Long zunionstore(final String dstkey, final String... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zunionstore(dstkey, sets);
            }
        });
    }

    @Override
    public Long zunionstore(final String dstkey, final ZParams params, final String... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zunionstore(dstkey, params, sets);
            }
        });
    }

    @Override
    public Long zunionstore(final byte[] dstkey, final byte[]... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zunionstore(dstkey, sets);
            }
        });
    }

    @Override
    public Long zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zunionstore(dstkey, params, sets);
            }
        });
    }

    @Override
    public Long zinterstore(final String dstkey, final String... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zinterstore(dstkey, sets);
            }
        });
    }


    @Override
    public Long zinterstore(final byte[] dstkey, final byte[]... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zinterstore(dstkey, sets);
            }
        });
    }


    @Override
    public Long zinterstore(final String dstkey, final ZParams params, final String... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zinterstore(dstkey, params, sets);
            }
        });
    }

    @Override
    public Long zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
        // 默认返回 0
        return throwOrReturnValue(false, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zinterstore(dstkey, params, sets);
            }
        });
    }

    @Override
    public Long linsert(final String key, final LIST_POSITION where, final String pivot, final String value) {
        // 默认返回 -1 表示pivot 未找到
        return throwOrReturnValue(false, -1L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.linsert(key, where, pivot, value);
            }
        });
    }


    @Override
    public Long linsert(final byte[] key, final LIST_POSITION where, final byte[] pivot, final byte[] value) {
        // 默认返回 -1 表示pivot 未找到
        return throwOrReturnValue(false, -1L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.linsert(key, where, pivot, value);
            }
        });
    }

    @Override
    public Boolean setbit(final String key, final long offset, final boolean value) {
        // 默认返回 0
        return throwOrReturnValue(false, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.setbit(key, offset, value);
            }
        });
    }

    @Override
    public Boolean setbit(final byte[] key, final long offset, final byte[] value) {
        // 默认返回 0
        return throwOrReturnValue(false, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.setbit(key, offset, value);
            }
        });
    }


    // ================ read 操作 =========================

    @Override
    public String get(final String key) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.get(key);
            }
        });
    }

    @Override
    public byte[] get(final byte[] key) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.get(key);
            }
        });
    }

    @Override
    public String hget(final String key, final String field) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.hget(key, field);
            }
        });
    }

    @Override
    public byte[] hget(final byte[] key, final byte[] field) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.hget(key, field);
            }
        });
    }

    @Override
    public String substr(final String key, final int start, final int end) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.substr(key, start, end);
            }
        });
    }

    @Override
    public byte[] substr(final byte[] key, final int start, final int end) {
        // 默认返回null
        return throwOrReturnValue(true, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.substr(key, start, end);
            }
        });
    }

    @Override
    public List<String> mget(final String... keys) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.mget(keys);
            }
        });
    }

    @Override
    public List<byte[]> mget(final byte[]... keys) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.mget(keys);
            }
        });
    }

    @Override
    public List<String> hmget(final String key, final String... fields) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.hmget(key, fields);
            }
        });
    }

    @Override
    public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.hmget(key, fields);
            }
        });
    }

    @Override
    public Set<String> keys(final String pattern) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.keys(pattern);
            }
        });
    }


    @Override
    public Set<byte[]> keys(final byte[] pattern) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.keys(pattern);
            }
        });
    }


    @Override
    public Set<String> hkeys(final String key) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.hkeys(key);
            }
        });
    }


    @Override
    public Set<byte[]> hkeys(final byte[] key) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.hkeys(key);
            }
        });
    }

    @Override
    public Boolean exists(final String key) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.exists(key);
            }
        });
    }


    @Override
    public Boolean exists(final byte[] key) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.exists(key);
            }
        });
    }

    @Override
    public Boolean hexists(final String key, final String field) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.hexists(key, field);
            }
        });
    }


    @Override
    public Boolean hexists(final byte[] key, final byte[] field) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.hexists(key, field);
            }
        });
    }

    @Override
    public Long hlen(final String key) {
        // 默认返回 0
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hlen(key);
            }
        });
    }


    @Override
    public Long hlen(final byte[] key) {
        // 默认返回 0
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.hlen(key);
            }
        });
    }

    @Override
    public Long llen(final String key) {
        // 默认返回 0
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.llen(key);
            }
        });
    }

    @Override
    public Long llen(final byte[] key) {
        // 默认返回 0
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.llen(key);
            }
        });
    }

    @Override
    public List<String> hvals(final String key) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.hvals(key);
            }
        });
    }

    @Override
    public List<byte[]> hvals(final byte[] key) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.hvals(key);
            }
        });
    }

    @Override
    public Map<String, String> hgetAll(final String key) {
        // 默认返回 empty map
        return throwOrReturnValue(true, Collections.<String, String>emptyMap(), new Callback<Map<String, String>>() {

            @Override
            public Map<String, String> call() {
                return SwitcherSupportJedis.super.hgetAll(key);
            }
        });
    }

    @Override
    public Map<byte[], byte[]> hgetAll(final byte[] key) {
        // 默认返回 empty map
        return throwOrReturnValue(true, Collections.<byte[], byte[]>emptyMap(), new Callback<Map<byte[], byte[]>>() {

            @Override
            public Map<byte[], byte[]> call() {
                return SwitcherSupportJedis.super.hgetAll(key);
            }
        });
    }


    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.lrange(key, start, end);
            }
        });
    }


    @Override
    public List<byte[]> lrange(final byte[] key, final int start, final int end) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.lrange(key, start, end);
            }
        });
    }


    @Override
    public String lindex(final String key, final long index) {
        // 默认返回null,表示未找到
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.lindex(key, index);
            }
        });
    }


    @Override
    public byte[] lindex(final byte[] key, final int index) {
        // 默认返回null,表示未找到
        return throwOrReturnValue(true, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.lindex(key, index);
            }
        });
    }


    @Override
    public Set<String> smembers(final String key) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.smembers(key);
            }
        });
    }


    @Override
    public Set<byte[]> smembers(final byte[] key) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.smembers(key);
            }
        });
    }

    @Override
    public Boolean sismember(final String key, final String member) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.sismember(key, member);
            }
        });
    }


    @Override
    public Boolean sismember(final byte[] key, final byte[] member) {
        // 默认返回false
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.sismember(key, member);
            }
        });
    }


    @Override
    public Set<String> sinter(final String... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.sinter(keys);
            }
        });
    }

    @Override
    public Set<byte[]> sinter(final byte[]... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.sinter(keys);
            }
        });
    }

    @Override
    public Set<String> sunion(final String... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.sunion(keys);
            }
        });
    }


    @Override
    public Set<byte[]> sunion(final byte[]... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.sunion(keys);
            }
        });
    }

    @Override
    public Set<String> sdiff(final String... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.sdiff(keys);
            }
        });
    }

    @Override
    public Set<byte[]> sdiff(final byte[]... keys) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.sdiff(keys);
            }
        });
    }

    @Override
    public String srandmember(final String key) {
        // 默认返回null,表示未找到
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.srandmember(key);
            }
        });
    }


    @Override
    public byte[] srandmember(final byte[] key) {
        // 默认返回null,表示未找到
        return throwOrReturnValue(true, null, new Callback<byte[]>() {

            @Override
            public byte[] call() {
                return SwitcherSupportJedis.super.srandmember(key);
            }
        });
    }

    @Override
    public Long scard(final String key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.scard(key);
            }
        });
    }


    @Override
    public Long scard(final byte[] key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.scard(key);
            }
        });
    }


    @Override
    public Set<String> zrange(final String key, final long start, final long end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrange(key, start, end);
            }
        });
    }


    @Override
    public Set<byte[]> zrange(final byte[] key, final int start, final int end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrange(key, start, end);
            }
        });
    }


    @Override
    public Long zrank(final String key, final String member) {
        // 默认返回 null 表示不存在
        return throwOrReturnValue(true, null, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrank(key, member);
            }
        });
    }


    @Override
    public Long zrank(final byte[] key, final byte[] member) {
        // 默认返回 null 表示不存在
        return throwOrReturnValue(true, null, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrank(key, member);
            }
        });
    }


    @Override
    public Long zrevrank(final String key, final String member) {
        // 默认返回 null 表示不存在
        return throwOrReturnValue(true, null, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrevrank(key, member);
            }
        });
    }


    @Override
    public Long zrevrank(final byte[] key, final byte[] member) {
        // 默认返回 null 表示不存在
        return throwOrReturnValue(true, null, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zrevrank(key, member);
            }
        });
    }


    @Override
    public Set<String> zrevrange(final String key, final long start, final long end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrevrange(key, start, end);
            }
        });
    }

    @Override
    public Set<byte[]> zrevrange(final byte[] key, final int start, final int end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrevrange(key, start, end);
            }
        });
    }


    @Override
    public Set<Tuple> zrangeWithScores(final String key, final long start, final long end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeWithScores(key, start, end);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeWithScores(final byte[] key, final int start, final int end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeWithScores(key, start, end);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeWithScores(key, start, end);
            }
        });
    }


    @Override
    public Set<Tuple> zrevrangeWithScores(final byte[] key, final int start, final int end) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeWithScores(key, start, end);
            }
        });
    }

    @Override
    public Long zcard(final String key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcard(key);
            }
        });
    }

    @Override
    public Long zcard(final byte[] key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcard(key);
            }
        });
    }

    @Override
    public Double zscore(final String key, final String member) {
        // 默认返回 null,表示不存在
        return throwOrReturnValue(true, null, new Callback<Double>() {

            @Override
            public Double call() {
                return SwitcherSupportJedis.super.zscore(key, member);
            }
        });
    }


    @Override
    public Double zscore(final byte[] key, final byte[] member) {
        // 默认返回 null,表示不存在
        return throwOrReturnValue(true, null, new Callback<Double>() {

            @Override
            public Double call() {
                return SwitcherSupportJedis.super.zscore(key, member);
            }
        });
    }


    @Override
    public List<String> sort(final String key) {
        // 默认返回 empty list,sort 不会改变存储在redis中的值，所以是读操作
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.sort(key);
            }
        });
    }


    @Override
    public List<byte[]> sort(final byte[] key) {
        // 默认返回 empty list,sort 不会改变存储在redis中的值，所以是读操作
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {
            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.sort(key);
            }
        });
    }

    @Override
    public List<String> sort(final String key, final SortingParams sortingParameters) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<String>emptyList(), new Callback<List<String>>() {

            @Override
            public List<String> call() {
                return SwitcherSupportJedis.super.sort(key, sortingParameters);
            }
        });
    }

    @Override
    public List<byte[]> sort(final byte[] key, final SortingParams sortingParameters) {
        // 默认返回 empty list
        return throwOrReturnValue(true, Collections.<byte[]>emptyList(), new Callback<List<byte[]>>() {

            @Override
            public List<byte[]> call() {
                return SwitcherSupportJedis.super.sort(key, sortingParameters);
            }
        });
    }

    @Override
    public Long zcount(final String key, final double min, final double max) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcount(key, min, max);
            }
        });
    }

    @Override
    public Long zcount(final byte[] key, final double min, final double max) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcount(key, min, max);
            }
        });
    }

    @Override
    public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcount(key, min, max);
            }
        });
    }

    @Override
    public Long zcount(final String key, final String min, final String max) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.zcount(key, min, max);
            }
        });
    }

    @Override
    public Set<String> zrangeByScore(final String key, final double min, final double max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max);
            }
        });
    }


    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max);
            }
        });
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max);
            }
        });
    }

    @Override
    public Set<String> zrangeByScore(final String key, final String min, final String max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max);
            }
        });
    }

    @Override
    public Set<String> zrangeByScore(final String key, final double min, final double max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<byte[]>emptySet(), new Callback<Set<byte[]>>() {

            @Override
            public Set<byte[]> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<String> zrangeByScore(final String key, final String min, final String max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max, final int offset, final int count) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
        // 默认返回 empty set
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @Override
    public Long strlen(final String key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.strlen(key);
            }
        });
    }

    @Override
    public Long strlen(final byte[] key) {
        // 默认返回 0 表示不存在
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.strlen(key);
            }
        });
    }

    @Override
    public Boolean getbit(final String key, final long offset) {
        // 默认返回 0
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.getbit(key, offset);
            }
        });
    }

    @Override
    public Boolean getbit(final byte[] key, final long offset) {
        // 默认返回 0
        return throwOrReturnValue(true, false, new Callback<Boolean>() {

            @Override
            public Boolean call() {
                return SwitcherSupportJedis.super.getbit(key, offset);
            }
        });
    }

    @Override
    public String ping() {
        // 读写开关都关闭的时候，ping也返回默认值
        if (this.getReadSwitcher().isClose() && this.getWriteSwitcher().isClose()) {
            return "PONG";
        } else {
            return super.ping();
        }
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final double max, final double min) {
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrevrangeByScore(key, max, min);
            }
        });
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final String max, final String min) {
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrevrangeByScore(key, max, min);
            }
        });
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final double max, final double min, final int offset, final int count) {
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final String max, final String min, final int offset, final int count) {
        return throwOrReturnValue(true, Collections.<String>emptySet(), new Callback<Set<String>>() {

            @Override
            public Set<String> call() {
                return SwitcherSupportJedis.super.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min, final int offset, final int count) {
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min, final int offset, final int count) {
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
        return throwOrReturnValue(true, Collections.<Tuple>emptySet(), new Callback<Set<Tuple>>() {

            @Override
            public Set<Tuple> call() {
                return SwitcherSupportJedis.super.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @Override
    public Long setrange(final String key, final long offset, final String value) {
        return throwOrReturnValue(true, 0L, new Callback<Long>() {

            @Override
            public Long call() {
                return SwitcherSupportJedis.super.setrange(key, offset, value);
            }
        });
    }

    @Override
    public String getrange(final String key, final long startOffset, final long endOffset) {
        return throwOrReturnValue(true, null, new Callback<String>() {

            @Override
            public String call() {
                return SwitcherSupportJedis.super.getrange(key, startOffset, endOffset);
            }
        });
    }


    // ================ 其他 操作 开关无影响 =========================


    @Override
    public String type(String key) {
        // 开关对 type操作无影响
        return super.type(key);
    }

    @Override
    public String type(byte[] key) {
        // 开关对 type操作无影响
        return super.type(key);
    }

    @Override
    public String save() {
        // 开关对 save操作无影响
        return super.save();
    }


    @Override
    protected void checkIsInMulti() {
        // 开关对 checkIsInMulti操作无影响
        super.checkIsInMulti();
    }


    @Override
    public List<Object> pipelined(PipelineBlock jedisPipeline) {
        // 开关对 pipelined操作无影响
        return super.pipelined(jedisPipeline);
    }

    @Override
    public Pipeline pipelined() {
        // 开关对 pipelined操作无影响
        return super.pipelined();
    }


    @Override
    public String bgsave() {
        // 开关对 bgsave操作无影响
        return super.bgsave();
    }

    @Override
    public String bgrewriteaof() {
        // 开关对 bgrewriteaof操作无影响
        return super.bgrewriteaof();
    }

    @Override
    public Long lastsave() {
        // 开关对 lastsave操作无影响
        return super.lastsave();
    }

    @Override
    public String shutdown() {
        // 开关对 shutdown操作无影响
        return super.shutdown();
    }

    @Override
    public String info() {
        // 开关对 info操作无影响
        return super.info();
    }

    @Override
    public void monitor(JedisMonitor jedisMonitor) {
        // 开关对 monitor操作无影响
        super.monitor(jedisMonitor);
    }

    @Override
    public String slaveof(String host, int port) {
        // 开关对 slaveof操作无影响
        return super.slaveof(host, port);
    }

    @Override
    public String slaveofNoOne() {
        // 开关对 slaveofNoOne操作无影响
        return super.slaveofNoOne();
    }

    @Override
    public List<String> configGet(String pattern) {
        // 开关对 configGet操作无影响
        return super.configGet(pattern);
    }

    @Override
    public String configResetStat() {
        // 开关对 configResetStat操作无影响
        return super.configResetStat();
    }

    @Override
    public String configSet(String parameter, String value) {
        // 开关对 configSet操作无影响
        return super.configSet(parameter, value);
    }

    @Override
    public boolean isConnected() {
        // 开关对 isConnected操作无影响
        return super.isConnected();
    }

    @Override
    public void sync() {
        // 开关对 sync操作无影响
        super.sync();
    }


    @Override
    public String debug(DebugParams params) {
        // 开关对 debug操作无影响
        return super.debug(params);
    }

    @Override
    public Client getClient() {
        // 开关对 getClient操作无影响
        return super.getClient();
    }


    @Override
    public String quit() {
        // 开关对 quit操作无影响
        return super.quit();
    }

    @Override
    public String flushDB() {
        // 开关对 flushDB 操作无影响
        return super.flushDB();
    }

    @Override
    public String randomKey() {
        // 开关对 randomKey 操作无影响
        return super.randomKey();
    }


    @Override
    public byte[] randomBinaryKey() {
        // 开关对 randomBinaryKey 操作无影响
        return super.randomBinaryKey();
    }


    @Override
    public Long dbSize() {
        // 开关对 dbSize 操作无影响
        return super.dbSize();
    }

    @Override
    public Long expire(String key, int seconds) {
        // 开关对 expire 操作无影响,暂时未使用该功能
        return super.expire(key, seconds);
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        // 开关对 expireAt 操作无影响,暂时未使用该功能
        return super.expireAt(key, unixTime);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        // 开关对 expire 操作无影响,暂时未使用该功能
        return super.expire(key, seconds);
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        // 开关对 expireAt 操作无影响,暂时未使用该功能
        return super.expireAt(key, unixTime);
    }

    @Override
    public Long ttl(String key) {
        // 开关对 ttl 操作无影响,暂时未使用该功能
        return super.ttl(key);
    }

    @Override
    public Long ttl(byte[] key) {
        // 开关对 ttl 操作无影响,暂时未使用该功能
        return super.ttl(key);
    }


    @Override
    public String select(int index) {
        // 开关对 select 操作无影响
        return super.select(index);
    }

    @Override
    public String flushAll() {
        // 开关对 flushAll 操作无影响
        return super.flushAll();
    }

    @Override
    public String auth(String password) {
        // 开关对 auth 操作无影响
        return super.auth(password);
    }

    @Override
    public Transaction multi() {
        // 开关对 multi 操作无影响
        return super.multi();
    }

    @Override
    public List<Object> multi(TransactionBlock jedisTransaction) {
        // 开关对 multi 操作无影响
        return super.multi(jedisTransaction);
    }

    @Override
    public void connect() {
        // 开关对 connect 操作无影响
        super.connect();
    }

    @Override
    public void disconnect() {
        // 开关对 disconnect 操作无影响
        super.disconnect();
    }

    @Override
    public String watch(String... keys) {
        // 开关对 watch 操作无影响
        return super.watch(keys);
    }

    @Override
    public String watch(byte[]... key) {
        // 开关对 watch 操作无影响
        return super.watch(key);
    }

    @Override
    public String unwatch() {
        // 开关对 unwatch 操作无影响
        return super.unwatch();
    }

    @Override
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        // 开关对 subscribe 操作无影响
        super.subscribe(jedisPubSub, channels);
    }


    @Override
    public void subscribe(BinaryJedisPubSub jedisPubSub, byte[]... channels) {
        // 开关对 subscribe 操作无影响
        super.subscribe(jedisPubSub, channels);
    }

    @Override
    public Long publish(String channel, String message) {
        // 开关对 publish 操作无影响
        return super.publish(channel, message);
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        // 开关对 publish 操作无影响
        return super.publish(channel, message);
    }


    @Override
    public void psubscribe(BinaryJedisPubSub jedisPubSub, byte[]... patterns) {
        // 开关对 psubscribe 操作无影响
        super.psubscribe(jedisPubSub, patterns);
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        // 开关对 psubscribe 操作无影响
        super.psubscribe(jedisPubSub, patterns);
    }

    @Override
    public Long persist(String key) {
        // 开关对 persist 操作无影响
        return super.persist(key);
    }

    @Override
    public Long persist(byte[] key) {
        // 开关对 persist 操作无影响
        return super.persist(key);
    }

    @Override
    public String echo(String string) {
        // 开关对 echo 操作无影响
        return super.echo(string);
    }

    @Override
    public byte[] echo(byte[] string) {
        // 开关对 echo 操作无影响
        return super.echo(string);
    }

}
