package pku.abe.commons.uuid;

import pku.abe.commons.util.MD5Utils;

import java.util.UUID;

/**
 * Created by vontroy on 16-7-11.
 */
public class UUIDUtils {
    public static String createUUID() {
        String result;
        UUID uuid = UUID.randomUUID();
        String temp = uuid.toString().toUpperCase();
        result = temp.substring(0, 8) + temp.substring(9, 13) + temp.substring(14, 18) + temp.substring(19, 23) + temp.substring(24);
        return result;
    }

    public static String createHardwareId(String hardwareId) {
        hardwareId = hardwareId.toUpperCase();
        return hardwareId.substring(0, 8) + hardwareId.substring(9, 13) + hardwareId.substring(14, 18) + hardwareId.substring(19, 23)
                + hardwareId.substring(24);
    }

    public static String createIdentityId(String linkedMeKey, String hardwareId) {
        String sourceStr = linkedMeKey + hardwareId;
        return MD5Utils.MD5Sixteen(sourceStr);
    }
}
