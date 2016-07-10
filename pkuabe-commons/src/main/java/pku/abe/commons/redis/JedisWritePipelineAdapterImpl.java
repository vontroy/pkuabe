package pku.abe.commons.redis;

import pku.abe.commons.redis.clients.jedis.Pipeline;

import java.util.Map;

public class JedisWritePipelineAdapterImpl implements JedisWritePipeline {
    private Pipeline pipeline;

    public JedisWritePipelineAdapterImpl(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void set(final String key, String value) {
        pipeline.set(key, value);
    }

    public void setnx(final String key, String value) {
        pipeline.setnx(key, value);
    }

    public void hmset(final String key, final Map<String, String> hash) {
        pipeline.hmset(key, hash);
    }

    public void expire(final String key, final int seconds) {
        pipeline.expire(key, seconds);
    }

    public void del(final String... keys) {
        pipeline.del(keys);
    }

    @Override
    public void hset(String key, String field, String value) {
        pipeline.hset(key, field, value);
    }

    @Override
    public void hdel(String key, String field) {
        pipeline.hdel(key, field);
    }

    @Override
    public void incr(String key) {
        pipeline.incr(key);
    }

    @Override
    public void incrBy(String key, long integer) {
        pipeline.incrBy(key, integer);
    }

    @Override
    public void incrBy(byte[] key, long integer) {
        pipeline.incrBy(key, integer);
    }

    @Override
    public void hincrBy(String key, String field, long value) {
        pipeline.hincrBy(key, field, value);
    }

    @Override
    public void decr(String key) {
        pipeline.decr(key);
    }

    @Override
    public void decrBy(String key, long integer) {
        pipeline.decrBy(key, integer);
    }

    @Override
    public void rpush(String key, String value) {
        pipeline.rpush(key, value);
    }

    @Override
    public void rpush(String key, String... values) {
        pipeline.rpush(key, values);
    }

    @Override
    public void lpush(String key, String value) {
        pipeline.lpush(key, value);
    }

    @Override
    public void ltrim(String key, int start, int end) {
        pipeline.ltrim(key, start, end);
    }

    @Override
    public void getset(String key, String value) {
        pipeline.getSet(key, value);
    }

    @Override
    public void bfset(final String key) {
        pipeline.bfset(key);
    }

    @Override
    public void bfmset(final String... keys) {
        pipeline.bfmset(keys);
    }

    @Override
    public void zadd(final String key, final double score, final String member) {
        pipeline.zadd(key, score, member);
    }

    @Override
    public void zremrangeByRank(byte[] key, int start, int end) {
        pipeline.zremrangeByRank(key, start, end);
    }

    @Override
    public void zcard(byte[] key) {
        pipeline.zcard(key);
    }
}
