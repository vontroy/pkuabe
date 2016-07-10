package pku.abe.commons.util;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;

import pku.abe.commons.log.ApiLogger;

public class ApiUtil {
    private static Map<Byte, String> genders = new HashMap<Byte, String>();

    private static final String GENDER_MALE = "m";
    private static final String GENDER_FEMALE = "f";
    private static final String GENDER_NONE = "n";
    private static Random random = new Random();

    /**
     * 消息本身格式不正确，不需要重试
     */
    public static final int MQ_PROCESS_ABORT = -1;

    /**
     * 消息处理失败，需要重试
     */
    public static final int MQ_PROCESS_RETRY = 0;

    /**
     * system degradation, need repair.
     */
    public static final int MQ_PROCESS_DEGRADATION = -2;

    /**
     * 消息处理成功
     */
    public static final int MQ_PROCESS_SUCCESS = 1;

    private static ThreadLocal<CRC32> crc32Provider = new ThreadLocal<CRC32>() {
        @Override
        protected CRC32 initialValue() {
            return new CRC32();
        }
    };

    static {
        genders.put(Byte.valueOf((byte) 1), GENDER_MALE);
        genders.put(Byte.valueOf((byte) 2), GENDER_FEMALE);
    }

    //////////////////////////////////////////////////////
    // FIXME 确定所有调用在相同线程中，否则会判断错误.
    //////////////////////////////////////////////////////
    // 过滤微博时，传递是否有不能看的微博
    public static ThreadLocal<Boolean> hasVisible = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static String getGender(byte genderCode) {
        String gender = genders.get(genderCode);
        if (gender != null) {
            return gender;
        } else {
            return GENDER_NONE;
        }
    }

    public static long getCrc32(byte[] b) {
        CRC32 crc = crc32Provider.get();
        crc.reset();
        crc.update(b);
        return crc.getValue();
    }

    public static long getCrc32(String str) {
        try {
            return getCrc32(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            ApiLogger.warn(new StringBuilder(64).append("Error: getCrc32, str=").append(str), e);
            return -1;
        }
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static Long[] toLongArr(String[] strArr) {
        return ArrayUtil.toLongArr(strArr);
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static Long[] toLongArr(long[] longArr) {
        return ArrayUtil.toLongArr(longArr);
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static long[] toRawLongArr(String[] strArr) {
        return ArrayUtil.toRawLongArr(strArr);
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static long[] toLongArr(Collection<Long> ids) {
        return ArrayUtil.toLongArr(ids);
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static String longArrToPrintableString(long[] longArr) {
        return ArrayUtil.longArrToPrintableString(longArr);
    }

    /**
     * see ArrayUtil
     */
    @Deprecated
    public static String stringArrToPrintableString(String[] objectArr) {
        return ArrayUtil.stringArrToPrintableString(objectArr);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static String formatDate(Date date, String defaultValue) {
        return ApiDateUtil.formatDate(date, defaultValue);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static Date parseDate(String dateStr, Date defaultValue) {
        return ApiDateUtil.parseDate(dateStr, defaultValue);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static String getYearMonth(Date date) {
        return ApiDateUtil.getYearMonth(date);
    }

    public static String getYearMonthForSI(Date date) {
        return getYearMonth(date);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static String formateDateTime(Date date) {
        return ApiDateUtil.formateDateTime(date);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static Date parseDateTime(String timeStr, Date defaultValue) {
        return ApiDateUtil.parseDateTime(timeStr, defaultValue);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static final int getCurrentHour() {
        return ApiDateUtil.getCurrentHour();
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static final int getLastHour() {
        return ApiDateUtil.getLastHour();
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static final int getNextHour() {
        return ApiDateUtil.getNextHour();
    }

    public static String getAttentionHash(long uid, int tblCount) {
        int hash = getHash4split(uid, tblCount);
        String hex = Long.toHexString(hash);
        if (hex.length() == 1) {
            hex = "0" + hex;
        }
        return hex;

    }

    public static int getHash4split(long id, int splitCount) {
        try {
            long h = getCrc32(String.valueOf(id).getBytes("utf-8"));
            if (h < 0) {
                h = -1 * h;
            }
            int hash = (int) (h / splitCount % splitCount);
            return hash;
        } catch (UnsupportedEncodingException e) {
            ApiLogger.warn(
                    new StringBuilder(64).append("Error: when hash4split, id=").append(id).append(", splitCount=").append(splitCount), e);
            return -1;
        }
    }

    public static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // eat the exception, for i am not care it
        }
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static Date getFirstDayOfCurMonth() {
        return ApiDateUtil.getFirstDayOfCurMonth();
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static Date getFirstDayInMonth(Date date) {
        return ApiDateUtil.getFirstDayInMonth(date);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static Date getFirstDayInMonth(int month) {
        return ApiDateUtil.getFirstDayInMonth(month);
    }

    /**
     * see ApiDateUtil
     */
    @Deprecated
    public static boolean isCurrentMonth(Date date) {
        return ApiDateUtil.isCurrentMonth(date);
    }

    public static int nextInt() {
        return random.nextInt();
    }

    public static int nextInt(int seed) {
        return random.nextInt(seed);
    }

    /**
     * 根据lang存储值获取对应的码值
     *
     * @param lang
     * @return
     */
    public static String getLangCode(int lang) {
        String l = code2LangMap.get(lang);
        if (l != null) {
            return l;
        }
        return "zh-cn";
    }

    public static String toLangString(int lang) {
        return code2LangMap.get(lang);
    }

    public static Integer toLangCode(String lang) {
        return lang2CodeMap.get(lang);
    }

    private static final Map<String, Integer> lang2CodeMap = new HashMap<String, Integer>();
    private static final Map<Integer, String> code2LangMap = new HashMap<Integer, String>();

    static {
        lang2CodeMap.put("zh-cn", 1);
        lang2CodeMap.put("zh-tw", 2);
        lang2CodeMap.put("zh-hk", 3);
        lang2CodeMap.put("en", 4);
        lang2CodeMap.put("en-us", 5);
        code2LangMap.put(0, "zh-cn");
        code2LangMap.put(1, "zh-cn");
        code2LangMap.put(2, "zh-tw");
        code2LangMap.put(3, "zh-hk");
        code2LangMap.put(4, "en");
        code2LangMap.put(5, "en-us");
    }

    /**
     * truncate the value to the specified length
     *
     * @param value the value that will being trucated
     * @param length the threshold of trucate
     * @return the new value after truncated
     */
    public static String truncateString(Object value, int length) {
        String oldValue = value == null ? "" : value.toString();
        if (length > 0 && oldValue.length() > length) {
            oldValue = oldValue.substring(0, length);
        }
        return oldValue;
    }

    public static void main(String[] args) {
        int hash = getHash4split(10506, 256);
        int hash2 = getHash4split(10506, 512);
        System.out.println(hash/1);
        System.out.println(hash2/1);
        Date date1 = parseDateTime("2011-12-23 11:57:48", null);
        Date date2 = parseDateTime("2011-12-22 11:57:48", null);
        System.out.println("date1 timeMillis:" + date1.getTime());
        System.out.println("date2 timeMillis:" + date2.getTime());
        System.out.println("date1-date2 timeMillis:" + (date1.getTime() - date2.getTime()));
    }
}
