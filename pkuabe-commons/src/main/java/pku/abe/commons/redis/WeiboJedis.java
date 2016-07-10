package pku.abe.commons.redis;

import java.util.ArrayList;
import java.util.List;

import pku.abe.commons.redis.clients.jedis.Jedis;
import pku.abe.commons.redis.clients.jedis.JedisShardInfo;
import pku.abe.commons.redis.clients.util.SafeEncoder;

public class WeiboJedis extends Jedis {
    public WeiboJedis(final String host) {
        super(host);
    }

    public WeiboJedis(final String host, final int port) {
        super(host, port);
    }

    public WeiboJedis(final String host, final int port, final int timeout) {
        super(host, port, timeout);
    }

    public WeiboJedis(JedisShardInfo shardInfo) {
        super(shardInfo);
    }

    public Object evalsha(String sha1, int keyCount, String... params) {
        checkIsInMulti();
        client.evalsha(sha1, keyCount, params);

        return getEvalResult();
    }

    /**
     * just for fix getEvalResult bug
     *
     * @return
     */
    private Object getEvalResult() {
        Object result = client.getOne();

        if (result instanceof byte[]) return SafeEncoder.encode((byte[]) result);

        if (result instanceof List<?>) {
            List<?> list = (List<?>) result;
            List<Object> listResult = new ArrayList<Object>(list.size());
            for (Object bin : list) {
                // Jedis at this just call listResult.add(SafeEncoder.encode((byte[]) bin)); that
                // may be throw ClassCastException
                if (bin instanceof byte[]) {
                    listResult.add(SafeEncoder.encode((byte[]) bin));
                } else {
                    listResult.add(bin);
                }
            }

            return listResult;
        }

        return result;
    }

}
