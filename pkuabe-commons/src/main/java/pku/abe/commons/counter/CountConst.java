package pku.abe.commons.counter;

import java.util.Map;

public class CountConst {
    public static class Timeout {
        public static long count = 200; // or 200
        public static long counts = 400; // or 400
    }

    public static class ValueFalse {
        public static final int intFalse = 0;
        public static final long longFalse = 0;
        public static final boolean booleanFalse = false;
        public static final Map<String, Integer> mapFalse = null;
    }

    public static class ValueDefault {
        public static final int intReturn = 0;
        public static final int intTrue = 1;
        public static final int intIncr = 1;
        public static final int intDecr = -1;
    }

}
