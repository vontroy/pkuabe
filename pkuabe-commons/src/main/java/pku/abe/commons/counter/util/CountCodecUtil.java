package pku.abe.commons.counter.util;

import pku.abe.commons.util.Util;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class CountCodecUtil {
    public static final char comma = ',';
    public static final char colon = ':';

    /**
     * result format is "repost:9,comment:8,like:1" TODO string.spilt performance
     *
     * @param str
     * @return null if not exist
     */
    public static Map<String, Integer> toMap(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }

        Map<String, Integer> counts = new HashMap<String, Integer>();
        String[] datas = StringUtils.split(str, comma);
        for (String data : datas) {
            String[] keyValue = StringUtils.split(data, colon);
            counts.put(keyValue[0], Util.convertInt(keyValue[1]));
        }
        return counts;
    }
}
