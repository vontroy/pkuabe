package pku.abe.commons.mcq;

public class CommonConst {

    public static final String UTF8 = "UTF-8";

    /**
     * 512B类型mcq可写入消息最大长度
     */
    public static final int MCQ_512B_BOUNDARY = 512;

    /**
     * empty long array (flyweight)
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    /**
     * empty long object array (flyweight)
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    /**
     * empty object array (flyweight)
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static final int TIMELINE_SIZE = 1000;

    /**
     * vector
     */
    public final static int VECTOR_INITIAL = 0;
    public final static int VECTOR_LIMIT = 200;
    public final static int VECTOR_THRESHOLD = 10;

}
