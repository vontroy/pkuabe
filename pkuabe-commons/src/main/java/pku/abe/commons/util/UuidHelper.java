package pku.abe.commons.util;

import java.util.Date;

public class UuidHelper {
    public static final long IDC_ID_BIT = 3L << UuidConst.HA_SEQ_BIZ_LENGTH;
    public static final long BIZ_FLAG_BIT = 15L << UuidConst.HA_SEQ_LENGTH;
    public static final long SEQ_MASK = 131071L << UuidConst.HA_BIT_LENGTH;

    public static final long MIN_VALID_ID = 3000000000000000L;
    public static final long MAX_VALID_ID = 4500000000000000L;

    /**
     * is valid id
     *
     * @param id
     * @return
     */
    public static boolean isValidId(long id) {
        return (id > MIN_VALID_ID) && (id < MAX_VALID_ID);
    }

    /**
     * get unix time from id (Accurate to seconds)
     *
     * @param id
     * @return
     */
    public static long getTimeFromId(long id) {
        return getTimeNumberFromId(id) + UuidConst.ID_OFFSET;
    }

    /**
     * get time number from id
     *
     * @param id
     * @return
     */
    public static long getTimeNumberFromId(long id) {
        return id >> UuidConst.HA_SEQ_BIZ_IDC_LENGTH;
    }

    /**
     * get idc from id
     *
     * @param id
     * @return
     */
    public static long getIdcIdFromId(long id) {
        return (id & IDC_ID_BIT) >> UuidConst.HA_SEQ_BIZ_LENGTH;
    }

    /**
     * get seq from id
     *
     * @param id
     * @return
     */
    public static long getSeqFromId(long id) {
        return id & SEQ_MASK >> UuidConst.HA_BIT_LENGTH;
//        if (isSpecUuid(id)) {
//            return (id >> 1) % UuidConst.SPEC_SEQ_LIMIT;
//        } else {
//            return id % UuidConst.SEQ_LIMIT;
//        }
    }

    /**
     * get unix time from id, start from 2014-01-01 00:00:00 <pre>UNIT:seconds</pre>
     *
     * @param id
     * @return
     */
    public static long getUnixTimeFromId(long id) {
        return (id + UuidConst.ID_OFFSET_2014) * 1000;
    }

    /**
     * get 30 bit timestamp from id，start from 2014-01-01 00:00:00 <pre>UNIT:seconds</pre>
     *
     * @param id
     * @return
     */
    public static long getTime30FromId(long id) {
        long actualId = id / 1000 - UuidConst.ID_OFFSET_2014;
        return actualId > 0 ? actualId : 0;
    }

    /**
     * get biz flag for spec uuid
     *
     * @param id
     * @return
     */
    public static long getBizFlag(long id) {
        if (isSpecUuid(id)) {
            return (id & BIZ_FLAG_BIT) >> UuidConst.HA_SEQ_LENGTH;
        }
        return -1;
    }

    public static boolean isSpecIdc(long idcFlag) {
//        for (long specIdc : UuidConst.SPEC_IDC_FLAGS) {
//            if (specIdc == idcFlag) {
//                return true;
//            }
//        }
//        return false;
        return true;
    }

    /**
     * get date time from id
     *
     * @param id
     * @return
     */
    public static Date getDateFromId(long id) {
        return new Date(getTimeFromId(id) * 1000);
    }

    /**
     * check if the uuid is spec uuid
     *
     * @param id
     * @return
     */
    private static boolean isSpecUuid(long id) {
        long idcId = getIdcIdFromId(id);
        return isSpecIdc(idcId);
    }

    /**
     * get timestamp from id with second precision.
     *
     * @param id
     * @return
     */
    public static long getTimeStampFromId(long id) {
        return (getTimeNumberFromId(id) + UuidConst.ID_OFFSET) * 1000;
    }

    public static void main(String[] args) {
        long id = 3929849129435138L;

        System.out.println(id);
        System.out.println(getDateFromId(id));
        System.out.println(getBizFlag(id));
        System.out.println(getIdcIdFromId(id));
        System.out.println(getSeqFromId(id));

    }
}
