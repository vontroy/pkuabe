package pku.abe.commons.redis;

import java.util.Map;

public interface JedisWritePipeline {
    void set(final String key, String value);

    void setnx(final String key, String value);

    void hset(final String key, final String field, final String value);

    void hmset(final String key, final Map<String, String> hash);

    void del(final String... keys);

    void hdel(final String key, final String field);

    void incr(final String key);

    void incrBy(String key, long integer);

    void incrBy(byte[] key, long integer);

    void hincrBy(String key, String field, long value);

    void decr(final String key);

    void decrBy(String key, long integer);

    void rpush(String key, String value);

    void rpush(String key, String... values);

    void lpush(String key, String value);

    void ltrim(String key, int start, int end);

    void expire(final String key, final int seconds);

    void getset(String key, String value);

    void bfset(final String key);

    void bfmset(final String... keys);

    void zadd(String key, double score, String member);

    void zremrangeByRank(byte[] key, int start, int end);

    void zcard(byte[] key);
}
