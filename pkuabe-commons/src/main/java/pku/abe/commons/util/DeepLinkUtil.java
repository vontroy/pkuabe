package pku.abe.commons.util;

/**
 * Created by LinkedME01 on 16/3/19.
 */
public class DeepLinkUtil {
    public static String getDeepLinkFromUrl(String url) {
        if(url == null) {
            return null;
        }
        String[] strArr = url.split("/|\\?");
        if(strArr.length > 4) {
            return strArr[4];
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(getDeepLinkFromUrl("https://lk.me/abcd/efghtia?adfa=bbb"));
    }
}
