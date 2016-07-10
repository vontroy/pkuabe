package pku.abe.commons.redis;

import pku.abe.commons.redis.clients.jedis.Pipeline;

public class JedisReadPipelineAdapterImpl implements JedisReadPipeline {
    private Pipeline pipeline;

    public JedisReadPipelineAdapterImpl(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void get(final String key) {
        pipeline.get(key);
    }

    public void mget(final String... keys) {
        pipeline.mget(keys);
    }

    public void hget(final String key, final String field) {
        pipeline.hget(key, field);
    }

    public void hgetByteArray(byte[] key, byte[] field) {
        pipeline.hgetByteArray(key, field);
    }

    public void hmget(final String key, final String... fields) {
        pipeline.hmget(key, fields);
    }

    @Override
    public void hmgetBytes(byte[] key, byte[]... fields) {
        pipeline.hmgetBytes(key, fields);
    }

    public void hgetAll(final String key) {
        pipeline.hgetAll(key);
    }

    @Override
    public void hgetAllBytes(byte[] key) {
        pipeline.hgetAllBytes(key);
    }

    @Override
    public void hkeys(String key) {
        pipeline.hkeys(key);
    }

    @Override
    public void hlen(String key) {
        pipeline.hlen(key);
    }

    @Override
    public void exists(String key) {
        pipeline.exists(key);
    }

    @Override
    public void hexists(String key, String field) {
        pipeline.hexists(key, field);
    }

    @Override
    public void type(String key) {
        pipeline.type(key);
    }

    @Override
    public void bfget(final String key) {
        pipeline.bfget(key);
    }

    @Override
    public void bfmget(final String... keys) {
        pipeline.bfmget(keys);
    }

    @Override
    public void sismember(String key, String member) {
        pipeline.sismember(key, member);
    }

    @Override
    public void smembers(String key) {
        pipeline.smembers(key);
    }

    @Override
    public void scard(String key) {
        pipeline.scard(key);
    }

    @Override
    public void zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public void zrevrangeByScoreX(byte[] key, byte[] max, byte[] min, int offset, int count) {
        pipeline.zrevrangeByScoreX(key, max, min, offset, count);
    }

    @Override
    public void zcard(String key) {
        pipeline.zcard(key);
    }

    @Override
    public void zrangeWithScores(String key, int start, int end) {
        pipeline.zrangeWithScores(key, start, end);
    }

    @Override
    public void zrevrangeWithScores(String key, int start, int end) {
        pipeline.zrevrangeWithScores(key, start, end);
    }

    @Override
    public void zscore(String key, String member) {
        pipeline.zscore(key, member);
    }
}
