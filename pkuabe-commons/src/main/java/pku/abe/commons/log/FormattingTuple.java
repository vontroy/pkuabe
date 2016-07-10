package pku.abe.commons.log;

final class FormattingTuple {

    static final FormattingTuple NULL = new FormattingTuple(null);

    private final String message;
    private final Throwable throwable;
    private final Object[] argArray;

    /**
     * 指定格式化后的字符串，生成一个包含格式化结果的 {@link FormattingTuple}。
     *
     * @param message 格式化后的字符串。
     */
    FormattingTuple(String message) {
        this(message, null, null);
    }

    /**
     * 指定格式化后的字符串、格式化的参数数组和异常原因，生成一个包含格式化结果的 {@link FormattingTuple}。
     *
     * @param message 格式化后的字符串。
     * @param argArray 格式化的参数数组。
     * @param throwable 异常原因。
     */
    FormattingTuple(String message, Object[] argArray, Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
        if (throwable == null) {
            this.argArray = argArray;
        } else {
            this.argArray = trimmedCopy(argArray);
        }
    }

    /**
     * 拷贝一个不包含最后一个元素的 Object 数组。
     */
    static Object[] trimmedCopy(Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            throw new IllegalStateException("non-sensical empty or null argument array");
        }
        final int trimmedLength = argArray.length - 1;
        Object[] trimmed = new Object[trimmedLength];
        System.arraycopy(argArray, 0, trimmed, 0, trimmedLength);
        return trimmed;
    }

    /**
     * 获取格式化后的字符串。
     *
     * @return 格式化后的字符串。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取异常原因。
     *
     * @return 异常原因。
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 获取格式化的参数数组。
     *
     * @return 格式化的参数。
     */
    public Object[] getArgArray() {
        return argArray;
    }

}
