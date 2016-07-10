package pku.abe.commons.util;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class Constants {
    
    public static final String DEEPLINK_HTTPS_PREFIX = "https://lkme.cc";
    public static final String LIVE_TEST_API_FLAG = "i";

    
    public static final String DEEPLINK_HTTP_PREFIX = "http://lkme.cc/";
    public static final String LINKEDME_HTTPS_PREFIX = "https://www.linkedme.cc/";
    public static final String SPOTLIGHT_PREFIX = "cc.lkme.";

    public static final String CREATE_URL_API = DEEPLINK_HTTP_PREFIX + LIVE_TEST_API_FLAG + "/sdk/url";
    public static final String DASHBOARD_API_URL = LINKEDME_HTTPS_PREFIX + LIVE_TEST_API_FLAG;
    public static final String LOGO_BASE_URL = DEEPLINK_HTTP_PREFIX + LIVE_TEST_API_FLAG + "/sdk/images/";
    public static final String APP_LOGO_IMG_TYPE = ".png";

    public static final boolean enableProfiling = true;

    public static AtomicLong vectorTime = new AtomicLong(0);

    public static int MAX_FRIEND_COUNT = 2000;

    public static final String KEY_SEPERATOR = ".";
    public static final String ALL_HASH_FIELDS = "all_hash_fields";

    // 脏数据缓存过期时间 1 分钟
    public static Date EXPTIME_VECTOR_DIRTY = new Date(1000l * 60);

    // Meta vector content missed expire time is 1 hour.
    public static Date EXPTIME_META_VECTOR_CONTENT_MISSED = new Date(1000l * 60 * 60);

    // Meta vector userType missed expire time is 1 hour.
    public static Date EXPTIME_META_VECTOR_USERTYPE_MISSED = new Date(1000l * 60 * 60);

    // timeline multi DB查询超时
    public static long TIMEOUT_MULTI_USER_TIMELINE = 600;
    // memcache read timeout, msec
    public static long TIMEOUT_MULTIGET_VECTOR = 5000;

    // test var
    public static long MAX_UID = 20000;
    public static long MIN_UID = 10000;

    // try cas times
    public static int CAS_TIME = 2;
    public static int MAX_CAS_TIME = 3;

    // return type
    public static String RETURN_TYPE_JSON = "json";
    public static String RETURN_TYPE_XML = "xml";

    public static int OP_CACHE_TIMEOUT = 200;

    // 消息处理超时时间
    public static int OP_MQPROC_TIMEOUT = 100;

    /**
     * 是否有权限访问完整的user信息。<br>
     * AUTHORIZED：有权限获得完整信息；SEMI_AUTHORIZED：可以获得处理后的完整信息；NOT_AUTHORIZED：只能获得非完整信息
     */
    public static final int AUTHORIZED = 1;
    public static final int SEMI_AUTHORIZED = 2;
    public static final int NOT_AUTHORIZED = 3;


    // APP LOGO 图片存放地址
    public static final String ImgPath = "/data1/logo_pic/";
}
