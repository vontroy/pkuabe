package pku.abe.commons.log;

import java.util.HashMap;
import java.util.Map;

final class MessageFormatter {

    static final char DELIM_START = '{';
    static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    /**
     * 防止被直接实例化。
     */
    private MessageFormatter() {
        // 空。
    }

    /**
     * 根据给出的格式化参数，对格式化模板进行格式化。
     *
     * @param messagePattern 格式化模板。
     * @param arg 格式化参数。
     * @return 格式化后的结果。
     */
    static FormattingTuple format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[] {arg});
    }

    /**
     * 根据给出的格式化参数，对格式化模板进行格式化。
     *
     * @param messagePattern 格式化模板。
     * @param argA 第一个格式化参数。
     * @param argB 第二个格式化参数。
     * @return 格式化后的结果。
     */
    static FormattingTuple format(String messagePattern, Object argA, Object argB) {
        return arrayFormat(messagePattern, new Object[] {argA, argB});
    }

    static Throwable getThrowableCandidate(Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            return null;
        }

        final Object lastEntry = argArray[argArray.length - 1];
        if (lastEntry instanceof Throwable) {
            return (Throwable) lastEntry;
        }

        return null;
    }

    static boolean isEscapedDelimeter(String messagePattern, int delimeterStartIndex) {
        return delimeterStartIndex != 0 && messagePattern.charAt(delimeterStartIndex - 1) == ESCAPE_CHAR;
    }

    static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
        return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
    }

    private static void deeplyAppendParameter(StringBuffer sbuf, Object o, Map<Object[], Void> seenMap) {
        if (o == null) {
            sbuf.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            // 不是原始类数组。
            safeObjectAppend(sbuf, o);
        } else {
            // 检查是否是原始类数组，因为原始类的数组无法被转成 Object[]。
            if (o instanceof boolean[]) {
                booleanArrayAppend(sbuf, (boolean[]) o);
            } else if (o instanceof byte[]) {
                byteArrayAppend(sbuf, (byte[]) o);
            } else if (o instanceof char[]) {
                charArrayAppend(sbuf, (char[]) o);
            } else if (o instanceof short[]) {
                shortArrayAppend(sbuf, (short[]) o);
            } else if (o instanceof int[]) {
                intArrayAppend(sbuf, (int[]) o);
            } else if (o instanceof long[]) {
                longArrayAppend(sbuf, (long[]) o);
            } else if (o instanceof float[]) {
                floatArrayAppend(sbuf, (float[]) o);
            } else if (o instanceof double[]) {
                doubleArrayAppend(sbuf, (double[]) o);
            } else {
                objectArrayAppend(sbuf, (Object[]) o, seenMap);
            }
        }
    }

    private static void safeObjectAppend(StringBuffer sbuf, Object o) {
        try {
            String oAsString = o.toString();
            sbuf.append(oAsString);
        } catch (Throwable t) {
            System.err.println("InternalLogging: Failed toString() invocation on an object of type [" + o.getClass().getName() + ']');
            t.printStackTrace();
            sbuf.append("[FAILED toString()]");
        }
    }

    private static void objectArrayAppend(StringBuffer sbuf, Object[] a, Map<Object[], Void> seenMap) {
        sbuf.append('[');
        if (!seenMap.containsKey(a)) {
            seenMap.put(a, null);
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                deeplyAppendParameter(sbuf, a[i], seenMap);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            seenMap.remove(a);
        } else {
            sbuf.append("...");
        }
        sbuf.append(']');
    }

    private static void booleanArrayAppend(StringBuffer sbuf, boolean[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void byteArrayAppend(StringBuffer sbuf, byte[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void charArrayAppend(StringBuffer sbuf, char[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void shortArrayAppend(StringBuffer sbuf, short[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void intArrayAppend(StringBuffer sbuf, int[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void longArrayAppend(StringBuffer sbuf, long[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void floatArrayAppend(StringBuffer sbuf, float[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void doubleArrayAppend(StringBuffer sbuf, double[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    /**
     * 根据给出的格式化参数集，对格式化模板进行格式化。
     *
     * @param messagePattern 格式化模板。
     * @param argArray 格式化参数集。
     * @return 格式化后的结果。
     */
    static FormattingTuple arrayFormat(final String messagePattern, final Object[] argArray) {
        Throwable throwableCandidate = getThrowableCandidate(argArray);

        if (messagePattern == null) {
            return new FormattingTuple(null, argArray, throwableCandidate);
        }
        if (argArray == null) {
            return new FormattingTuple(messagePattern);
        }

        StringBuffer sbuf = new StringBuffer(messagePattern.length() + 50);
        int i = 0, j, L;
        for (L = 0; L < argArray.length; ++L) {
            j = messagePattern.indexOf(DELIM_STR, i);
            if (j == -1) {
                // 没有剩余需要格式化的东西了。Fuck off。
                if (i == 0) {
                    // 普通的文本串。
                    return new FormattingTuple(messagePattern, argArray, throwableCandidate);
                } else {
                    // 把剩余的文本子串附上。
                    sbuf.append(messagePattern.substring(i, messagePattern.length()));
                    return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
                }
            } else {
                if (isEscapedDelimeter(messagePattern, j)) {
                    if (!isDoubleEscaped(messagePattern, j)) {
                        --L;
                        sbuf.append(messagePattern.substring(i, j - 1));
                        sbuf.append(DELIM_START);
                        i = j + 1;
                    } else {
                        sbuf.append(messagePattern.substring(i, j - 1));
                        deeplyAppendParameter(sbuf, argArray[L], new HashMap<Object[], Void>());
                        i = j + 2;
                    }
                } else {
                    sbuf.append(messagePattern.substring(i, j));
                    deeplyAppendParameter(sbuf, argArray[L], new HashMap<Object[], Void>());
                    i = j + 2;
                }
            }
        }

        sbuf.append(messagePattern.substring(i, messagePattern.length()));
        if (L < argArray.length - 1) {
            return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
        } else {
            return new FormattingTuple(sbuf.toString(), argArray, null);
        }
    }

}
