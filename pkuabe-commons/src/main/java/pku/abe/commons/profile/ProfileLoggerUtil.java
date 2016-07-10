package pku.abe.commons.profile;

import org.apache.log4j.Logger;

public class ProfileLoggerUtil {
    private static Logger profileLog = Logger.getLogger("profile");

    public static void monitor(String string) {
        profileLog.info(string);
    }
}
