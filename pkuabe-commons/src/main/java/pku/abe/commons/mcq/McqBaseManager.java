package pku.abe.commons.mcq;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mcq Base Manager
 */
public class McqBaseManager {

    /**
     * 是否从队列读取消息
     */
    public static AtomicBoolean IS_ALL_READ = new AtomicBoolean(true);

    /**
     * 停止所有队列读取消息
     */
    public static void stopReadAll() {
        IS_ALL_READ.compareAndSet(true, false);
    }

    /**
     * 启动所有队列读取消息
     */
    public static void startReadAll() {
        IS_ALL_READ.compareAndSet(false, true);
    }

    public static String status() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("\r\nreading_mcq(chenbo,true):\t");
        sb.append(IS_ALL_READ.get());
        return sb.toString();
    }

}
