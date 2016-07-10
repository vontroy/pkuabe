package pku.abe.commons.util;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

public class RequestTraceContext {
    private static final ThreadLocal<RequestTraceContext> requestContext = new ThreadLocal<RequestTraceContext>();
    private static final AtomicLong idGenerator = new AtomicLong();
    private static long refreshIdPrefix = 0;

    private long id;
    private String requestId;
    private String api;

    private long uid;
    private String appKey;
    private String originalSource;

    private int appId;
    private String spr;// 代表客户端版本信息
    private int exposureType;

    private String ip;// 用户ip
    private Long vid;// 游客vid
    private boolean isAggreDarwin = false;
    // 第32位置为1，目的是为了设置vip flag 位。32,33位置为01,表示vip
    private static long FLAG_VIP_MASK_32 = 1L << 31;

    // 33位为0，其余为1，目的是为了设置vip flag 位 。32,33位置为01,表示vip
    // 111111111111111111111111111111011111111111111111111111111111111
    private static long FLAG_VIP_MASK_33 = 9223372032559808511L;

    // 32,33位为00
    // 111111111111111111111111111111001111111111111111111111111111111
    private static long FLAG_NOT_VIP = 9223372030412324863L;

    private static long VIP_FLAG = 1L;

    static {
        int intIp = IPUtils.ipToInt(IPUtils.getLocalIp());
        intIp = intIp & 0x00FFFFFF;
        intIp <<= 6;

        int minute = Calendar.getInstance().get(Calendar.MINUTE); // 获取当前的分钟数

        refreshIdPrefix = ((long) (intIp | minute)) << 33;
    }

    public RequestTraceContext(long id) {
        this.id = id;
    }

    public RequestTraceContext(String api) {
        this(api, 0, null, 0, null);
    }

    public RequestTraceContext(String api, String requestId) {
        this(api, 0, null, 0, null);
        this.requestId = requestId;
    }

    public RequestTraceContext(String api, long uid, String appKey, int appId, String originalSource) {
        this.api = api;
        this.id = refreshIdPrefix | idGenerator.incrementAndGet();

        // API common data
        this.uid = uid;
        this.appKey = appKey;
        this.appId = appId;
        this.originalSource = originalSource;
    }

    public static void init(long requestId) {
        requestContext.set(new RequestTraceContext(requestId));
    }

    public static void init(String api) {
        requestContext.set(new RequestTraceContext(api));
    }

    public static void init(String api, String requestId) {
        requestContext.set(new RequestTraceContext(api, requestId));
    }

    /**
     * explict initialize current thread context.
     *
     * @param api
     */
    public static void init(String api, long uid, String appKey, int appId, String originalSource) {
        requestContext.set(new RequestTraceContext(api, uid, appKey, appId, originalSource));
    }

    /**
     * set current thread context according to parent thread context.
     *
     * @param parentContext
     */
    public static void spawn(RequestTraceContext parentContext) {
        requestContext.set(parentContext);
    }

    /**
     * clear current thread context, which may leads to {@link #get()} return null.
     */
    public static void clear() {
        requestContext.remove();
    }

    /**
     * get current thread's context. the context may be initialized by parent thread or explicit
     * call {@link #init(String)}
     *
     * @return null if current thread doesn't have any context.
     */
    public static RequestTraceContext get() {
        return requestContext.get();
    }

    /**
     *
     */
    public static void finish() {
        clear();
    }

    public static boolean isPC() {
        return checkOriginalSource("3818214747");
    }

    public static boolean isMAPI() {
        return checkOriginalSource("3439264077");
    }

    public static boolean checkOriginalSource(String appkey) {
        if (isBlank(appkey)) {
            return false;
        }

        RequestTraceContext context = get();
        if (context != null) {
            return appkey.equals(context.getOriginalSource());
        }
        return false;
    }

    public static String getRequestId() {
        RequestTraceContext context = get();
        if (context != null) {
            return context.requestId;
        }
        return null;
    }

    public static long getRefreshId() {
        RequestTraceContext context = get();
        if (context != null) {
            return context.id;
        }
        return 0;
    }

    public static boolean isFromApi(String api) {
        if (isBlank(api)) {
            return false;
        }

        RequestTraceContext context = get();
        if (context != null) {
            return api.equals(context.getApi());
        }
        return false;
    }

    public static String getClientVersion() {
        if (isBlank(get().spr)) {
            return null;
        }

        String[] sprElem = get().spr.split(";");
        if (sprElem == null || sprElem.length == 0) {
            return null;
        }

        String fromField = null;

        for (String field : sprElem) {
            if (field != null && field.startsWith("from")) {
                fromField = field;
                break;
            }
        }

        if (fromField == null) { // 没有from
            return null;
        }

        String[] fromElem = fromField.split(":");
        if (fromElem == null || fromElem.length != 2) {// from:1046095010,格式不对
            return null;
        }

        String fromValue = fromElem[1];
        if (isBlank(fromValue) || fromValue.length() < 10) {
            return null;
        }

        return fromValue;

    }

    /**
     * 以整形数，获取用户ip
     *
     * @return
     */
    public int getUserIp() {
        int ipValue = 0;

        RequestTraceContext context = get();
        if (context != null) {
            if (context.ip != null && context.ip.length() > 0) {
                ipValue = IPUtils.ipToInt(context.ip);
            }
        }

        return ipValue;
    }

    /**
     * 以long获取游客的vid
     *
     * @return
     */
    public long getVisitorVid() {
        long visitorVid = 0L;

        RequestTraceContext context = get();
        if (context != null) {
            if (context.vid != null) {
                try {
                    visitorVid = context.vid.longValue();
                } catch (Exception e) {}
            }
        }

        return visitorVid;
    }

    public boolean isAggreDarwin() {
        return isAggreDarwin;
    }

    public void setAggreDarwin(boolean isAggreDarwin) {
        this.isAggreDarwin = isAggreDarwin;
    }

    public Long getVid() {
        return vid;
    }

    public void setVid(Long vid) {
        this.vid = vid;
    }

    public long getId() {
        return id;
    }

    public String getApi() {
        return api;
    }

    public long getUid() {
        return uid;
    }

    public String getAppKey() {
        return appKey;
    }

    public int getAppId() {
        return appId;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(String originalSource) {
        this.originalSource = originalSource;
    }

    public String getSpr() {
        return spr;
    }

    public void setSpr(String spr) {
        this.spr = spr;
    }

    public int getExposureType() {
        return exposureType;
    }

    public void setExposureType(int exposureType) {
        this.exposureType = exposureType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    private static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    public void markVipFlagOnRequestId(VipType vipType) {
        if (VipType.VIP.equals(vipType)) {
            // 32,33位置为01,表示vip
            this.id = this.id | FLAG_VIP_MASK_32 & FLAG_VIP_MASK_33;
        } else if (VipType.UN_VIP.equals(vipType)) {
            // 32,33位置为00,表示非vip
            this.id &= FLAG_NOT_VIP;
        }
    }

    public static boolean isVip() {
        RequestTraceContext context = get();
        if (context != null) {
            // 提取出32，33位
            // 6442450944L为110000000000000000000000000000000
            long vipFlag = context.id & 6442450944L;
            if (vipFlag >> 31 == VIP_FLAG) {
                return true;
            }
        }
        return false;
    }

    public static enum VipType {
        VIP, UN_VIP;
    }

    ;

    public static void main(String[] args) {
        // System.out.println(VipType.VIP.equals(VipType.VIP));
        // RequestTraceContext.init("test");
        RequestTraceContext context = RequestTraceContext.get();
        context.markVipFlagOnRequestId(VipType.UN_VIP);
        System.out.println(RequestTraceContext.isVip());

    }
}
