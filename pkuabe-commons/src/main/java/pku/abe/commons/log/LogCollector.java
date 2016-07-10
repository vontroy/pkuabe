package pku.abe.commons.log;

/**
 * <b>Log Format</b> <br>
 * [host name] [date time] [unique log id] [module] [schema] [type] [data]
 */
public interface LogCollector {

    public static final String DEFAULT_LOG_ID = "0";

    public void log(String logId, String module, String schema, DType type, Object data);

    /**
     * provide default logId and type for json string
     *
     * @param module
     * @param schema
     * @param json
     */
    public void log(String module, String schema, String json);

    public static enum DType {
        RAWSTR(0), JSON(1), BYTES(2);
        private int value;

        DType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }
}
