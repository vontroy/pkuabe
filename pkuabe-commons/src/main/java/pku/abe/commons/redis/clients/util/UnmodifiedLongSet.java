package pku.abe.commons.redis.clients.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import pku.abe.commons.redis.clients.jedis.Jedis;

public class UnmodifiedLongSet {
    private static final int SPREAD_NUM = 1;
    private final ByteBuffer data;
    private final int dataBucketCount;
    private int length;

    public UnmodifiedLongSet(long[] elements) {
        assert elements != null;
        int capacity = 4;
        int length = elements.length >> 2;
        while (length > 0) {
            length = length >> 1;
            capacity = capacity << 1;
        }
        capacity = capacity << SPREAD_NUM;
        data = ByteBuffer.allocate((capacity << 3)).order(ByteOrder.LITTLE_ENDIAN);
        dataBucketCount = capacity - 1;
        for (long v : elements) {
            put(v);
        }
        // System.out.println(buff.capacity());
    }

    public boolean put(long v) {
        if (v == 0) {
            return false;
        }
        // 如果填充率大于50%，则不再进行填充
        if (dataBucketCount < (length << 1)) {
            return false;
        }
        int index = (int) ((jenkinsHash(v) & dataBucketCount) << 3);
        // System.out.println(v+":"+index);
        long k = data.getLong(index);
        if (k == 0 || k == v) {
            data.putLong(index, v);
            length++;
            return true;
        }
        int i = -1;
        while ((i++) <= dataBucketCount) {
            index += 8;
            if (index >= data.capacity()) {
                index = 0;
            }
            long old = data.getLong(index);
            if (old == 0) {
                data.putLong(index, v);
                length++;
                return true;
            } else if (old == v) {
                return true;
            }
        }
        return false;
    }

    public boolean get(long v) {
        if (v == 0) {
            return true;
        }
        int index = (int) ((jenkinsHash(v) & dataBucketCount) << 3);
        long k = data.getLong(index);
        if (k == 0) {
            return false;
        } else if (k == v) {
            return true;
        }
        int i = -1;
        while ((i++) <= dataBucketCount) {
            index += 8;
            if (index >= data.capacity()) {
                index = 0;
            }
            k = data.getLong(index);
            if (k == 0) {
                return false;
            } else if (k == v) {
                return true;
            }
        }
        return false;
    }

    private final static long jenkinsHash(long key) {
        key = (~key) + (key << 21); // key = (key << 21) - key - 1;
        key = key ^ (key >> 24);
        key = (key + (key << 3)) + (key << 8); // key * 265
        key = key ^ (key >> 14);
        key = (key + (key << 2)) + (key << 4); // key * 21
        key = key ^ (key >> 28);
        key = key + (key << 31);
        return key & 0xffffffffL;
    }

    public byte[] toBytes() {
        return data.array();
    }

    public int getLength() {
        return this.length;
    }

    public static void main(String[] args) {
        String[] s =
                "2615417307, 1618051664, 1197369013, 2524694901, 1763962553, 1981468201, 2091189990, 2042814503, 1876787097, 1816835210, 3824253084, 2548476863, 2846890147, 2306722374, 1959057834, 2707338415, 2188239015, 2492278617, 3306210431, 3229125510, 1723981287, 1790702521, 2615278891, 1747913543, 2649075371, 2936031252, 2092092367, 2236374653, 3803065668, 1764594317, 1962310741, 1934183965, 1660174083, 1642909335, 2671109275, 1892744317, 2027967862, 2196165737, 2735238641, 2008657363, 2660192857, 1828175687, 2963811593, 1261788454, 1262931363, 2656274875, 1843147045, 3158071460, 1932977875, 2266899550, 3612930387, 2720089595, 2496066774, 1904907955, 1856136454, 1681825884, 3043496084, 1666680885, 1644461042, 1719232542, 1665736437, 1234552257, 1762331644, 1402602034, 1777924625, 2644586462, 1583900764, 1195230310, 1764523734, 1656809190, 1198920804, 1212812142, 1494848464, 2562810123, 1608574203, 1854283601, 1781379945, 1658688240, 3125046087, 1676082433, 1752839275, 1263498570, 1660587693, 1337970873, 1249207211"
                        .split(", ");
        long[] ll = new long[s.length];
        for (int i = 0; i < ll.length; i++) {
            // System.out.println(i+" : " + s[i]);
            ll[i] = Long.parseLong(s[i]);
        }
        System.out.println(ll.length + ":" + Arrays.toString(ll));
        UnmodifiedLongSet h = new UnmodifiedLongSet(ll);
        for (int i = 0; i <= h.dataBucketCount; i++) {
            System.out.println("pos-" + i + ":" + h.data.getLong());
        }
        System.out.println(h.dataBucketCount + " -- " + h.length);
        // System.exit(0);
        Jedis j = new Jedis("10.210.226.129", 6379);
        j.connect();
        j.lsset("abcd", ll);
        System.out.println(j.lsmexists("abcd", new long[] {1763962553, 1234}));
        j.disconnect();

    }
}
