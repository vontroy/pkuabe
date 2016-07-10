package pku.abe.commons.counter.util;

import pku.abe.commons.log.ApiLogger;

import java.util.concurrent.atomic.AtomicBoolean;

public class CountUtil {
    /**
     * count debug
     */
    public static AtomicBoolean countDebug = new AtomicBoolean(false);

    public static boolean isDebugEnabled() {
        return ApiLogger.isDebugEnabled() && countDebug.get();
    }
}
