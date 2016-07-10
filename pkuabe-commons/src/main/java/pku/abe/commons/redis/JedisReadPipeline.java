package pku.abe.commons.redis;

public interface JedisReadPipeline {

    void get(final String key);

    void mget(final String... keys);

    void hget(final String key, final String field);

    void hgetByteArray(byte[] key, byte[] field);

    void hmget(final String key, final String... fields);

    void hmgetBytes(final byte[] key, final byte[]... fields);

    void hgetAll(final String key);

    void hgetAllBytes(final byte[] key);

    void hkeys(final String key);

    void hlen(final String key);

    void exists(final String key);

    void hexists(final String key, final String field);

    void type(String key);

    void bfget(final String key);

    void bfmget(final String... keys);

    void sismember(String key, String member);

    void smembers(String key);

    void scard(String key);

    void zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

    void zrevrangeByScoreX(byte[] key, byte[] max, byte[] min, int offset, int count);

    void zcard(String key);

    void zrangeWithScores(String key, int start, int end);

    void zrevrangeWithScores(String key, int start, int end);

    void zscore(String key, String member);
}
