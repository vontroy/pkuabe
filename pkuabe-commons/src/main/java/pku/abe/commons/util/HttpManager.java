package pku.abe.commons.util;

import java.util.HashSet;
import java.util.Set;

import pku.abe.commons.log.ApiLogger;
import org.apache.commons.lang.StringUtils;
import com.google.code.hs4j.network.util.ConcurrentHashSet;

public class HttpManager {
    private static Set<String> blockResources = new HashSet<String>();

    // 存储可降级的资源列表，用于给运维降级系统提示用，最多存储2000个，防止内存占用过多，2000个对降级提示来说足够了
    private static Set<String> httpResources = new ConcurrentHashSet<String>();
    // 最多存储2000个，防止内存占用过多，2000个对降级提示来说足够了
    private static int RESOURCES_MAX_SIZE = 2000;

    // private static String r1 = "http://i.t.sina.com.cn";
    // private static String r2 = "http://data.i.t.sina.com.cn";
    // private static String r3 = "http://recom.i.t.sina.com.cn";

    public static void addBlockResource(String r) {
        if (r == null || r.length() < 6) {// http://
            return;
        }
        blockResources.add(r);

        // 不符合规范的url也能降级，但是要提示给前端
        if (!Util.isValidUrl(r)) {
            throw new WarnMsgException("输入的url不符合规范");
        }


        if (!httpResources.contains(r.toLowerCase())) {
            throw new WarnMsgException(new StringBuilder("降级成功，但是降级的url不在可降级的url资源列表(此列表里最多存放").append(RESOURCES_MAX_SIZE)
                    .append("个资源)里，最好核实下降级后是否真的生效！").toString());
        }
    }

    public static void removeBlockResource(String r) {
        blockResources.remove(r);
    }

    public static boolean isBlockResource(String url) {
        if (url == null) {
            return true;
        }
        for (String br : blockResources) {
            // 不区分大小写
            if (br != null && url.toLowerCase().startsWith(br.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> getBlockResources() {
        return blockResources;
    }


    public static boolean addHttpResources(String url) {
        try {
            int size = httpResources.size();
            if (StringUtils.isBlank(url) || size > RESOURCES_MAX_SIZE) {
                ApiLogger.warn("add url to HttpManager's httpResources false, httpResources size:" + size);
                return false;
            }

            httpResources.add(StringUtils.substringBefore(url, "?").toLowerCase());
            return true;
        } catch (Exception e) {
            ApiLogger.warn("HttpManager addHttpResources error", e);
            return false;
        }
    }

    public static Set<String> getHttpResources() {
        return httpResources;
    }

    public static class WarnMsgException extends RuntimeException {
        public WarnMsgException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        System.out.println(HttpManager.addHttpResources("http://linkedme.cc"));
        System.out.println(httpResources);
        System.out.println(null instanceof WarnMsgException);
    }
}
