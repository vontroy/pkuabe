package pku.abe.commons.cookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by qipo on 15/9/15.
 */
public class CookieHelper {

    private static final String LKME_COOKIE_NAME = "linkedme_id";

    /**
     * set cookie expiration which is thirty days default system
     */

    private final static int COOKIE_EXPIRE_MAX_VALUE = Integer.MAX_VALUE;

    /**
     * delete a Cookie
     */

    public static void removeCookie(HttpServletResponse response, Cookie cookie) {
        if (cookie != null) {
            cookie.setPath("/");
            cookie.setValue("");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    /**
     * delete a Cookie
     */

    public static void removeCookie(HttpServletResponse response, Cookie cookie, String domain) {
        if (cookie != null) {
            cookie.setPath("/");
            cookie.setValue("");
            cookie.setMaxAge(0);
            cookie.setDomain(domain);
            response.addCookie(cookie);
        }
    }

    /**
     * get the cookie value by his name, if don't exist, return null
     */

    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = getCookie(request, name);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    /**
     * get the cookie object by his name, if don't exist, return null
     */

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie cookies[] = request.getCookies();

        if (cookies == null || name == null || name.length() == 0) {
            return null;
        }

        Cookie cookie = null;
        for (int i = 0; i < cookies.length; i++) {
            if (!cookies[i].getName().equals(name)) {
                continue;
            }
            cookie = cookies[i];
            if (request.getServerName().equals(cookie.getDomain())) {
                break;
            }
        }

        return cookie;
    }

    /**
     * add a cookie and set the expiration is month
     */
    public static void setCookie(HttpServletResponse response, String name, String value) {
        setCookie(response, name, value, COOKIE_EXPIRE_MAX_VALUE);
    }

    /**
     * add a new Cookie and set a long expire time (seconds)
     */
    public static void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        if (value == null) {
            value = "";
        }

        Cookie cookie = new Cookie(name, value);

        if (maxAge != 0) {
            cookie.setMaxAge(maxAge);
        } else {
            cookie.setMaxAge(COOKIE_EXPIRE_MAX_VALUE);
        }

        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * construction function
     */

    public CookieHelper() {}

    /**
     * get and set function
     */

    public static String getCookieName() {
        return LKME_COOKIE_NAME;
    }
}
