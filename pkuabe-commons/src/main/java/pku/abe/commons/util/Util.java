package pku.abe.commons.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import pku.abe.commons.exception.LMException;
import pku.abe.commons.exception.LMExceptionFactor;
import pku.abe.commons.log.ApiLogger;
import pku.abe.data.dao.util.DateDuration;

public class Util {

    private static final String IP_SEPARATOR = "\\.";
    private static final long[] MULTIPLIERS = new long[] {16777216L, 65536L, 256L, 1L};

    private static final long NODEIDMIN = 999999999999999L;

    private static final long[] ZERO_LENGTH = new long[] {};

    private static final int fillchar = '=';
    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";
    private static Double ERROR_LATITUDE = Double.MAX_VALUE;
    private static Double ERROR_LONGITUDE = Double.MAX_VALUE;
    private static int MAX_DIGS_LEN = 70;
    private static int[] digs = new int[MAX_DIGS_LEN];

    private static final long minDeepLinkId = 3344763785052162L;
    private static final long maxDeepLinkId = 4503599627370495L;

    static {
        digs['0'] = 0;
        digs['1'] = 1;
        digs['2'] = 2;
        digs['3'] = 3;
        digs['4'] = 4;
        digs['5'] = 5;
        digs['6'] = 6;
        digs['7'] = 7;
        digs['8'] = 8;
        digs['9'] = 9;
    }

    private static final Pattern VALID_URL_PATTERN =
            Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");

    private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>() {
        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (Exception e) {}
            return null;
        }
    };

    public static String md5Digest(byte[] data) {
        MessageDigest md5 = MD5.get();
        md5.reset();
        md5.update(data);
        byte[] digest = md5.digest();
        return encodeHex(digest);
    }

    public static String encodeHex(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length + bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * 判断是否是经度
     *
     * @param src
     * @return
     */
    public static boolean isLatitude(String src) {

        return validGeoNumber(src) && convertDouble(src, ERROR_LATITUDE) != ERROR_LATITUDE;
    }

    // 以00开关的地理位置，都认定为非法的地理位置信息. 紧急修复 @TODO fixme
    private static boolean validGeoNumber(String s) {
        if (s == null) {
            return false;
        }
        if (StringUtils.startsWith(s, "00")) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否是经度
     *
     * @param src
     * @return
     */
    public static boolean isLongitude(String src) {
        return validGeoNumber(src) && convertDouble(src, ERROR_LONGITUDE) != ERROR_LONGITUDE;
    }

    public static String toStr(byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error byte[] to String => " + e);
        }
    }

    public static byte[] toBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error serializing String:" + str + " => " + e);
        }
    }

    /**
     * 将字符串转为double数字，如果格式不正常则为 0.0
     *
     * @param src
     * @return
     */
    public static double convertDouble(String src) {
        return convertDouble(src, 0.0);
    }

    /**
     * 将字符串转为double数字，如果格式不正常则为 defaultValue
     *
     * @param src
     * @param defaultValue
     * @return
     */
    public static double convertDouble(String src, double defaultValue) {
        try {
            return Double.parseDouble(src);
        } catch (Exception e) {}
        return defaultValue;
    }

    public static int convertInt(Object obj) {
        return convertInt(obj, 0);
    }

    public static int convertInt(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {}
        return defaultValue;
    }

    public static long convertLong(String src) {
        return convertLong(src, 0);
    }

    public static long convertLong(String src, long defaultValue) {
        try {
            return parseLong(src, 10);
        } catch (Exception e) {}
        return defaultValue;
    }

    public static long parseLong(String s, int radix) throws NumberFormatException {
        long result = 0;
        boolean negative = false;
        int i = 0, max = s.length();
        int digit;

        if (max > 0) {
            if (s.charAt(0) == '-') {
                negative = true;
                i++;
            }

            if (i < max) {
                digit = digits(s.charAt(i++));
                result = -digit;
            }
            while (i < max) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = digits(s.charAt(i++));
                result *= radix;
                result -= digit;
            }
        } else {
            throw new NumberFormatException(s);
        }

        if (negative) {
            if (i > 1) {
                return result;
            } else { /* Only got "-" */
                throw new NumberFormatException(s);
            }
        } else {
            return -result;
        }
    }

    public static int digits(char c) {
        if (c > MAX_DIGS_LEN - 1) {
            throw new NumberFormatException("");
        }
        return digs[c];
    }

    /**
     * Encodes a String as a base64 String.
     *
     * @param data a String to encode.
     * @return a base64 encoded String.
     */
    public static String encodeBase64(String data) {
        return encodeBase64(data.getBytes());
    }

    /**
     * Encodes a byte array into a base64 String.
     *
     * @param data a byte array to encode.
     * @return a base64 encode String.
     */
    public static String encodeBase64(byte[] data) {
        int c;
        int len = data.length;
        StringBuffer ret = new StringBuffer(((len / 3) + 1) * 4);
        for (int i = 0; i < len; ++i) {
            c = (data[i] >> 2) & 0x3f;
            ret.append(cvt.charAt(c));
            c = (data[i] << 4) & 0x3f;
            if (++i < len) c |= (data[i] >> 4) & 0x0f;

            ret.append(cvt.charAt(c));
            if (i < len) {
                c = (data[i] << 2) & 0x3f;
                if (++i < len) c |= (data[i] >> 6) & 0x03;

                ret.append(cvt.charAt(c));
            } else {
                ++i;
                ret.append((char) fillchar);
            }

            if (i < len) {
                c = data[i] & 0x3f;
                ret.append(cvt.charAt(c));
            } else {
                ret.append((char) fillchar);
            }
        }
        return ret.toString();
    }

    /**
     * Decodes a base64 String.
     *
     * @param data a base64 encoded String to decode.
     * @return the decoded String.
     */
    public static String decodeBase64(String data) {
        return decodeBase64(data.getBytes());
    }

    /**
     * Decodes a base64 aray of bytes.
     *
     * @param data a base64 encode byte array to decode.
     * @return the decoded String.
     */
    public static String decodeBase64(byte[] data) {
        int c, c1;
        int len = data.length;
        StringBuffer ret = new StringBuffer((len * 3) / 4);
        for (int i = 0; i < len; ++i) {
            c = cvt.indexOf(data[i]);
            ++i;
            c1 = cvt.indexOf(data[i]);
            c = ((c << 2) | ((c1 >> 4) & 0x3));
            ret.append((char) c);
            if (++i < len) {
                c = data[i];
                if (fillchar == c) break;

                c = cvt.indexOf((char) c);
                c1 = ((c1 << 4) & 0xf0) | ((c >> 2) & 0xf);
                ret.append((char) c1);
            }

            if (++i < len) {
                c1 = data[i];
                if (fillchar == c1) break;

                c1 = cvt.indexOf((char) c1);
                c = ((c << 6) & 0xc0) | c1;
                ret.append((char) c);
            }
        }
        return ret.toString();
    }

    public static String urlDecoder(String s, String charcoding) {
        if (s == null) return null;
        try {
            return URLDecoder.decode(s, charcoding);
        } catch (Exception e) {}
        return null;
    }

    public static String urlEncoder(String s, String charcoding) {
        if (s == null) return null;
        try {
            return URLEncoder.encode(s, charcoding);
        } catch (Exception e) {}
        return null;
    }

    public static String blankAscii32(String src, boolean isWithoutLR) {
        char[] c = src.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] < 32) {
                if (!(isWithoutLR && c[i] == '\n')) {
                    c[i] = ' ';
                }
            }
        }
        return String.valueOf(c);
    }

    /**
     * 将字符串解析为Map，其中str的格式为：{id}_{feature_value}|{id}_{feature_value}|...
     *
     * @param str 待处理字符串
     * @return 结果Map
     */
    public static Map<Long, Long> parseStr2Map(String str) {
        Map<Long, Long> map = new HashMap<Long, Long>(200);
        if (str == null || str.length() == 0) {
            return map;
        }
        int index = 0;
        int beginIndex = 0;
        long id = 0L;
        long featureValue = 0L;
        String item = null;
        while (beginIndex + 1 < str.length()) {
            index = str.indexOf('|', beginIndex);
            item = str.substring(beginIndex, index);
            beginIndex = index + 1;
            //
            index = item.indexOf('_');
            if (index <= 0) {
                continue;
            }
            try {
                id = parseLong(item.substring(0, index), 10);
                featureValue = parseLong(item.substring(index + 1, item.length()), 10);
                map.put(id, featureValue);
            } catch (Exception e) {
                ApiLogger.error("parseStr2Map error", e);
            }
        }
        return map;
    }

    /**
     * arithmetic crc32
     *
     * @param data
     * @return
     */
    public static long getCrc32(String data) {
        byte[] bytes = data.getBytes();
        java.util.zip.CRC32 x = new java.util.zip.CRC32();
        x.update(bytes);
        long l = x.getValue();
        return l;
    }

    /**
     * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
     *
     * @param s
     * @return
     */
    public static String toEscapeJson(String s) {
        if (s == null || s.length() < 1) return "";
        StringBuffer sb = new StringBuffer();
        escape(s, sb);
        return sb.toString();
    }

    /**
     * @param s - Must not be null.
     * @param sb
     */
    static void escape(String s, StringBuffer sb) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    // Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        } // for
    }

    public enum FillDirection {
        LEFT {
            public String fill(char[] str, String src) {
                StringBuilder sb = new StringBuilder();
                sb.append(str).append(src);
                return sb.toString();
            }
        },
        RIGHT {
            public String fill(char[] str, String src) {
                StringBuilder sb = new StringBuilder();
                sb.append(src).append(str);
                return sb.toString();
            }
        };

        public abstract String fill(char[] str, String src);
    }

    /**
     * Along the specified FillDirection, fill the source string with specified character until its
     * length reaches totalLen.
     *
     * @param c character to fill.
     * @param totalLen total length after filling characters
     * @param src the string to be filled.
     * @param direction filling direction.
     */
    public static String fillChar(char c, int totalLen, String src, FillDirection direction) {
        int srcLen = src.length();
        int fillLen = totalLen - srcLen;
        char[] str = new char[fillLen];
        for (int i = 0; i < fillLen; i++) {
            str[i] = c;
        }
        return direction.fill(str, src);
    }

    public static String addSlashes(String text) {
        final StringBuffer sb = new StringBuffer(text.length() * 2);
        final StringCharacterIterator iterator = new StringCharacterIterator(text);
        char character = iterator.current();
        while (character != StringCharacterIterator.DONE) {
            if (character == '"')
                sb.append("\\\"");
            else if (character == '\'')
                sb.append("\\\'");
            else if (character == '\\')
                sb.append("\\\\");
            else if (character == '\n')
                sb.append("\\n");
            else if (character == '\r')
                sb.append("\\r");
            else
                sb.append(character);
            character = iterator.next();
        }
        return sb.toString();
    }

    /**
     * 字符串ip(v4)转成长整型
     *
     * @param ip
     * @return
     * @author yongkun
     */
    public static long ip2long(String ip) {
        String[] fragments = ip.split(IP_SEPARATOR);
        long value = 0;
        for (int i = 0; i < fragments.length; i++) {
            String fragment = fragments[i];
            // value +=
            // BigInteger.valueOf(Long.valueOf(fragment)).multiply(BigInteger.valueOf(256).pow(3-i)).longValue();
            value += Long.parseLong(fragment) * MULTIPLIERS[i];
        }
        return value;
    }

    /**
     * 转换整型IP为字符串型IP(v4)
     *
     * @param ip 整型IP
     * @return 字符串型IP
     * @author yongkun
     */
    public static String long2ip(long ip) {
        String value = "";
        long last = ip;
        for (int i = 0; i < 4; i++) {
            if (i != 0) {
                value += ".";
            }
            long result = last / MULTIPLIERS[i];
            value += result;
            last -= result * MULTIPLIERS[i];
        }
        return value;
    }

    public static String arrayToString(Object array, String split) {
        if (array == null) {
            return "";
        }
        if (split == null) {
            throw new NullPointerException("split");
        }
        if (!array.getClass().isArray()) {
            return array.toString();
        }
        int len = Array.getLength(array);
        if (len == 0) {
            return "";
        }
        StringBuilder buf = new StringBuilder(len * 10);
        for (int i = 0; i < len; i++) {
            Object v = Array.get(array, i);
            if (v == null) {
                buf.append("null");
            } else {
                buf.append(v.toString());
            }
            if (i < len - 1) {
                buf.append(split);
            }
        }
        return buf.toString();
    }

    /**
     * 目前是根据ID的位数判定，16位以上的为nodeid
     *
     * @param id
     * @return
     */
    public static boolean isNodeId(long id) {
        if (id > NODEIDMIN) {
            return true;
        } else {
            return false;
        }
    }

    public static long[][] getUidAndNids(long[] ids) {
        long[][] uidAndNids = new long[2][];
        if (ArrayUtils.isEmpty(ids)) {
            uidAndNids[0] = ZERO_LENGTH;
            uidAndNids[1] = ZERO_LENGTH;
            return uidAndNids;
        }
        long[] allUid = new long[ids.length];
        int i = 0;
        int j = ids.length - 1;
        for (long id : ids) {
            if (isNodeId(id)) {
                allUid[i++] = id; // 前半部分存nid
            } else {
                allUid[j--] = id; // 后半部分存uid
            }
        }
        if (j + 1 < ids.length) {
            uidAndNids[0] = Arrays.copyOfRange(allUid, j + 1, ids.length); // uids
        } else {
            uidAndNids[0] = ZERO_LENGTH;
        }

        if (i > 0) {
            uidAndNids[1] = Arrays.copyOfRange(allUid, 0, i); // nids
        } else {
            uidAndNids[1] = ZERO_LENGTH;
        }
        return uidAndNids;
    }

    public static final int LONG_BYTES_SIZE = Long.SIZE / Byte.SIZE;

    public static byte[] toBytes(long val) {
        byte[] b = new byte[8];
        for (int i = 7; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    public static long toLong(byte[] bytes) {
        if (bytes.length < LONG_BYTES_SIZE) {
            throw new IllegalArgumentException("convert bytes to long error, bytes:" + Arrays.toString(bytes));
        }
        long l = 0;
        for (int i = 0; i < LONG_BYTES_SIZE; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }

    private static final int[] TABLE = {0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3,
            0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2,
            0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec,
            0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447,
            0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
            0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808,
            0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a,
            0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d,
            0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457,
            0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce,
            0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0,
            0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3,
            0xb966d409, 0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad,
            0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84,
            0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe,
            0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9,
            0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
            0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a,
            0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04,
            0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f,
            0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21,
            0x86d3d2d4, 0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777, 0x88085ae6, 0xff0f6a70,
            0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2,
            0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5,
            0x47b2cf7f, 0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf,
            0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d,};

    public static int crc32(byte[] buf) {
        int crc32 = 0;
        crc32 ^= 0xffffffff;
        for (int i = 0, endIndex = buf.length; i < endIndex; i++) {
            crc32 = crc32 >>> 8 ^ TABLE[(crc32 ^ buf[i]) & 0xff];
        }
        crc32 ^= 0xffffffff;
        if (crc32 < 0) {
            return -crc32;
        }
        return crc32;
    }

    public static long getMergedUuid(long uid, long id) {
        long timeNumber = UuidHelper.getTimeNumberFromId(id);
        long seqNumber = id % (1 << 13);
        long umid = timeNumber;
        umid <<= 33;
        umid += seqNumber << 20;
        umid += uid % (1 << 20);
        return umid;
    }

    private static final Pattern domainPattern = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");

    public static String getDomain(String url) {
        Matcher m = domainPattern.matcher(url);
        if (m.find()) {
            return m.group();
        } else {
            return null;
        }
    }

    public static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        Matcher m = VALID_URL_PATTERN.matcher(url.toLowerCase());
        return m.matches();
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return MD5Utils.md5(uuid.toString());
    }

    /**
     * ASCII convert to Unicode
     *
     * @param src
     * @return
     */
    public static String ascii2Unicode(String src) {
        if (StringUtils.isBlank(src)) {
            return StringUtils.EMPTY;
        }
        StringBuilder text = new StringBuilder();

        try {
            char[] bytes = src.toCharArray();
            for (char i : bytes) {
                String dd = Integer.toHexString(i);
                if (dd != null && dd.length() <= 2) {
                    dd = "00" + dd;
                }
                dd = "\\u" + dd;
                text.append(dd);
            }
        } catch (Exception e) {
            ApiLogger.warn("ascii2Unicode convert fail:" + src + " ;exception:", e);
            return StringUtils.EMPTY;
        }

        return text.toString();
    }

    public static Date timeStrToDate(String timeStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(timeStr);
        } catch (ParseException e) {
            ApiLogger.error("Util.timeStrToDate parse date failed", e);
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.timeStrToDate parse date failed");
        }
    }

    public static Calendar getCalendarWithDate(String date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timeStrToDate(date));
        return calendar;
    }

    public static boolean isSameMonth(String startDate, String endDate) {
        Calendar start = getCalendarWithDate(startDate);
        Calendar end = getCalendarWithDate(endDate);
        return start.get(Calendar.MONTH) == end.get(Calendar.MONTH) && start.get(Calendar.YEAR) == end.get(Calendar.YEAR);
    }

    public static boolean isValidDate(String startDate, String endDate) {
        DateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = null;
        Date end = null;
        try {
            start = dfs.parse(startDate);
            end = dfs.parse(endDate);
        } catch (ParseException e) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            try {
                start = df.parse(startDate);
                end = df.parse(endDate);
            } catch (ParseException e1) {
                ApiLogger.error("Util.isValidDate parse date failed", e1);
                throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.isValidDate parse date failed");
            }
        }
        return start.compareTo(end) < 1;
    }

    public static ArrayList<DateDuration> getBetweenMonths(String minDate, String maxDate) {
        if (!isValidDate(minDate, maxDate)) {
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "minDate is after maxDate!");
        }

        ArrayList<DateDuration> result = new ArrayList<DateDuration>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        Calendar start_date_min = Calendar.getInstance();
        Calendar start_date_max = Calendar.getInstance();
        Calendar end_date_min = Calendar.getInstance();
        Calendar end_date_max = Calendar.getInstance();

        try {
            start_date_min.setTime(sdf.parse(minDate));
            start_date_max.setTime(sdf.parse(minDate));
            start_date_max.set(Calendar.DAY_OF_MONTH, start_date_max.getActualMaximum(Calendar.DAY_OF_MONTH));

            end_date_min.setTime(sdf.parse(maxDate));
            end_date_min.set(Calendar.DAY_OF_MONTH, end_date_min.getActualMinimum(Calendar.DAY_OF_MONTH));
            end_date_max.setTime(sdf.parse(maxDate));

            min.setTime(sdf.parse(minDate));
            min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), 1);

            max.setTime(sdf.parse(maxDate));
            max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), 1);

        } catch (ParseException e) {
            ApiLogger.error("Util.getBetweenMonths parse time failed", e);
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.getBetweenMonths parse date failed");
        }

        if (min.equals(max)) {
            try {
                DateDuration start_date_duration = new DateDuration();
                start_date_duration.setMin_date(sdf.format(sdf.parse(minDate).getTime()));
                start_date_duration.setMax_date(sdf.format(sdf.parse(maxDate).getTime()));
                result.add(start_date_duration);
            } catch (ParseException e) {
                ApiLogger.error("Util.getBetweenMonths parse time failed", e);
                throw new LMException("Util.getBetweenMonths parse date failed");
            }
        } else {
            DateDuration start_date_duration = new DateDuration();
            start_date_duration.setMin_date(sdf.format(start_date_min.getTime()));
            start_date_duration.setMax_date(sdf.format(start_date_max.getTime()));
            result.add(start_date_duration);

            min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH) + 1, 1);
            Calendar curr_min = min;
            Calendar curr_max;

            while (curr_min.before(max)) {
                DateDuration dateDuration_tmp = new DateDuration();

                curr_min.set(Calendar.DAY_OF_MONTH, curr_min.getActualMinimum(Calendar.DAY_OF_MONTH));
                curr_max = curr_min;

                dateDuration_tmp.setMin_date(sdf.format(curr_min.getTime()));

                curr_max.set(Calendar.DAY_OF_MONTH, curr_max.getActualMaximum(Calendar.DAY_OF_MONTH));

                dateDuration_tmp.setMax_date(sdf.format(curr_max.getTime()));

                result.add(dateDuration_tmp);

                curr_min.add(Calendar.MONTH, 1);
            }
            DateDuration end_date_duration = new DateDuration();
            end_date_duration.setMin_date(sdf.format(end_date_min.getTime()));
            end_date_duration.setMax_date(sdf.format(end_date_max.getTime()));
            result.add(end_date_duration);
        }
        return result;
    }

    public boolean validateDeepLinkId(long deepLinkId) {
        return deepLinkId <= maxDeepLinkId && deepLinkId >= minDeepLinkId;
    }


    public static ArrayList<String> getDays(String minDate, String maxDate) {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Calendar start_date = Calendar.getInstance();
        Calendar end_date = Calendar.getInstance();

        try {
            start_date.setTime(sdf.parse(minDate));
            end_date.setTime(sdf.parse(maxDate));

            while (start_date.before(end_date) || start_date.equals(end_date)) {
                result.add(sdf.format(start_date.getTime()));
                start_date.add(start_date.DATE, 1);
            }

        } catch (ParseException e) {
            ApiLogger.error("Util.getBetweenMonths parse time failed", e);
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.getBetweenMonths parse time failed");
        }
        return result;
    }

    public static ArrayList<String> getIntervalDays(String minDate, String maxDate, int duration) {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Calendar start_date = Calendar.getInstance();
        Calendar end_date = Calendar.getInstance();

        try {
            start_date.setTime(sdf.parse(minDate));
            end_date.setTime(sdf.parse(maxDate));

            while (start_date.before(end_date) || start_date.equals(end_date)) {
                result.add(sdf.format(start_date.getTime()));
                start_date.add(start_date.DATE, duration);
            }

        } catch (ParseException e) {
            ApiLogger.error("Util.getBetweenMonths parse time failed", e);
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.getBetweenMonths parse time failed");
        }
        return result;
    }

    public static String formatLinkedmeKey(String linkedme_key) {
        if (!Strings.isNullOrEmpty(linkedme_key)) {
            String[] linkedme_keys = linkedme_key.split("_");
            int length = linkedme_keys.length;
            return linkedme_keys[length - 1];
        } else {
            return linkedme_key;
        }
    }

    public static String getCurrDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(Calendar.getInstance().getTime());
    }

    public static List<String> getMonthBetween(String minDate, String maxDate) {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyMM");

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        try {
            min.setTime(sdf.parse(minDate));
            min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), 1);

            max.setTime(sdf.parse(maxDate));
            max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), 2);
        } catch (ParseException e) {
            ApiLogger.error("Util.getBetweenMonths parse time failed", e);
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Util.getBetweenMonths parse time failed");
        }

        Calendar curr = min;
        while (curr.before(max)) {
            result.add(sdf1.format(curr.getTime()));
            curr.add(Calendar.MONTH, 1);
        }
        return result;
    }

    public static void main(String args[]) {
        String start_month = "2016-06-20";
        String end_month = "2016-08-20";
        int duration = 3;
        List<String> months = getDays(start_month, end_month);
        List<String> interval_days = getIntervalDays(start_month, end_month, duration);

        for (int i = 0; i < interval_days.size(); i++) {
            if (!months.isEmpty()) System.out.println(interval_days.get(i));
        }

        int a = ((true ? 1 : 0) << 3);
        int b = (true ? 1 : 0) << 2;
        int c = (true ? 1 : 0);

        int ios_android_flag = ((true ? 1 : 0) << 3) + ((true ? 1 : 0) << 2) + ((true ? 1 : 0) << 1) + (true ? 1 : 0);

        int d = a + b + c;
        System.out.println(ios_android_flag);
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
    }

}
