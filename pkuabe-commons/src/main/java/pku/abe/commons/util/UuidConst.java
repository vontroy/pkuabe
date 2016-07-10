package pku.abe.commons.util;

public class UuidConst {

    public static final long ID_OFFSET = 1262318400;  //2010-01-01 12:00:00
    public static final long ID_OFFSET_2014 = 1388505600; // 2014-01-01 00:00:00

    public static final long HA_BIT_LENGTH = 1;
    public static final long SEQ_BIT_LENGTH = 17;
    public static final long BIZ_BIT_LENGTH = 4;
    public static final long IDC_BIT_LENGTH = 2;
    public static final long TIME_BIT_LENGTH = 28;

    public static final long HA_SEQ_BIZ_IDC_LENGTH = HA_BIT_LENGTH + SEQ_BIT_LENGTH + BIZ_BIT_LENGTH + IDC_BIT_LENGTH;

    public static final long HA_SEQ_BIZ_LENGTH = HA_BIT_LENGTH + SEQ_BIT_LENGTH + BIZ_BIT_LENGTH;

    public static final long HA_SEQ_LENGTH = HA_BIT_LENGTH + SEQ_BIT_LENGTH;

    /**
     * idc 为12、13的发号器分发spec uuid，因为原始所有的idc等字段都用long，此处统一也使用long类型
     */
    public static final long[] SPEC_IDC_FLAGS = new long[] {12L, 13L};

    public static enum BizFlag {

        defaultBiz(0), activity(1), api(2);

        private int value;

        private BizFlag(int v) {
            this.value = v;
        }

        public int getValue() {
            return value;
        }

        public static BizFlag parseFlag(int flagValue) {
            if (flagValue == defaultBiz.value) {
                return defaultBiz;
            } else if (flagValue == activity.value) {
                return activity;
            } else if (flagValue == api.value) {
                return api;
            }
            return null;
        }
    }
}
