package pku.abe.commons.log;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.perf4j.log4j.Log4JStopWatch;

import pku.abe.commons.util.RequestTraceContext;

public class ApiLogger {
    public static long MC_FIRE_TIME = 50; // MC操作超时

    public static long DB_FIRE_TIME = 100; // DB操作超时

    public static long REDIS_FIRE_TIME = 50; // Redis操作超时

    private static Logger log = Logger.getLogger("api");
    private static Logger infoLog = Logger.getLogger("info");
    private static Logger warnLog = Logger.getLogger("warn");
    private static Logger errorLog = Logger.getLogger("error");

    private static Logger redoLog = Logger.getLogger("redoLog");
    private static Logger fireLog = Logger.getLogger("fire");

    private static Logger bizLog = Logger.getLogger("biz");


    /**
     * 触发自动降级的日志文件。
     */
    private static Logger autoTriggerLog = Logger.getLogger("trigger");

    private static Logger scribeLog = Logger.getLogger("scribe");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LogManager.shutdown();
            }
        });
    }

    /**
     * perf4j log
     */
    private static final Logger prefLogger = Logger.getLogger("perf4j");

    public static boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    public static boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public static void trace(Object msg) {
        log.trace(msg);

        debug(msg, DebugType.trace.name());
    }

    public static void debug(Object msg) {
        debug(msg, null);
    }

    /**
     * @param msg
     * @param debugType 枚举
     */
    public static void debug(Object msg, String debugType) {
        if (log.isDebugEnabled()) {
            msg = assembleDebugMsg(msg, debugType);
            if (log.isDebugEnabled()) {
                log.debug(msg);
            } else {
                // 如果没有开启debug级别，记录vip日志时用info级别
                printVipLog(msg);
            }
        }
    }

    private static void printVipLog(Object msg) {
        log.info(msg);
    }

    public static void debug(Object msg, String debugType, Throwable t) {
        // vip 用户记录 日志
        if (log.isDebugEnabled()) {
            msg = assembleDebugMsg(msg, debugType);
            log.info(msg, t);
        }
    }

    private static Object assembleDebugMsg(Object msg, String debugType) {
        // 日志最后添加reuqestid，格式为r=1
        String requestIdMsg = getRequestId();
        // 如果已经打印过requestid，就不再打印
        if (msg == null || StringUtils.isBlank(requestIdMsg) || msg.toString().indexOf(requestIdMsg) < 0) {
            msg = assembleRequestId(msg);
        }
        if (StringUtils.isBlank(debugType)) {
            debugType = DebugType.debug.name();
        }
        msg = assembleDebugTypeAndUid(msg, debugType);
        return msg;
    }

    private static Object assembleDebugTypeAndUid(Object msg, String debugType) {
        String uid = getUIdLogInfo();
        if (msg != null) {
            StringBuilder buf = new StringBuilder(msg.toString());
            buf.append(" ").append(uid).append(" ").append(debugType);
            msg = buf.toString();
        }
        return msg;
    }

    private static String getUIdLogInfo() {
        RequestTraceContext context = RequestTraceContext.get();
        String uid = "uid=0";
        if (context != null) {
            StringBuilder buf = new StringBuilder();
            buf.append("uid=").append(context.getUid());
            uid = buf.toString();
        }
        return uid;
    }

    public static void fire(Object msg) {
        if (fireLog.isInfoEnabled()) {
            msg = assembleRequestId(msg);
            fireLog.info(msg);

            // 如果是vip用户，记录日志
            debug(msg, DebugType.fire.name());
        }
    }

    /**
     * 输出一条用于辅助触发自动降级策略的日志消息。
     *
     * @param msg 日志消息。
     */
    public static void autoTrigger(String msg) {
        if (autoTriggerLog.isInfoEnabled()) {
            autoTriggerLog.info(msg, null);

            // 如果是vip用户，记录日志
            debug(msg, DebugType.autoTrigger.name());
        }
    }

    /**
     * 输出一条用于辅助触发自动降级策略的日志消息。内容根据格式化模板和格式化参数输出。 如果自动触发被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param arg 格式化参数。
     */
    public static void autoTrigger(String format, Object arg) {
        if (autoTriggerLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.format(format, arg);
            autoTriggerLog.info(tuple.getMessage(), tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(tuple.getMessage(), DebugType.autoTrigger.name(), tuple.getThrowable());
        }
    }

    /**
     * 输出一条用于辅助触发自动降级策略的日志消息。内容根据格式化模板和格式化参数输出。 如果自动触发被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param argA 第一个格式化参数。
     * @param argB 第二个格式化参数。
     */
    public static void autoTrigger(String format, Object argA, Object argB) {
        if (autoTriggerLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.format(format, argA, argB);
            autoTriggerLog.info(tuple.getMessage(), tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(tuple.getMessage(), DebugType.autoTrigger.name(), tuple.getThrowable());
        }
    }

    /**
     * 输出一条用于辅助触发自动降级策略的日志消息。内容根据格式化模板和格式化参数输出。 如果自动触发被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param arguments 三个以上一系列的格式化参数。
     */
    public static void autoTrigger(String format, Object... arguments) {
        if (autoTriggerLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
            autoTriggerLog.info(tuple.getMessage(), tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(tuple.getMessage(), DebugType.autoTrigger.name(), tuple.getThrowable());
        }
    }

    public static void logRedo(Object msg, Throwable e) {
        redoLog.warn(msg, e);
    }

    public static void scribe(Object msg) {
        if (scribeLog.isDebugEnabled()) {
            scribeLog.debug(msg);
        }
    }

    public static void info(Object msg) {
        if (infoLog.isInfoEnabled()) {
            // 日志最后添加reuqestid，格式为r=1
            msg = assembleRequestId(msg);
            infoLog.info(msg);

            // 如果是vip用户，记录日志
            debug(msg, DebugType.info.name());
        }
    }

    /**
     * 输出一条用于 INFO 日志。内容根据格式化模板和格式化参数输出。 如果 INFO 级别被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param arg 格式化参数。
     */
    public static void info(String format, Object arg) {
        if (infoLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.format(format, arg);
            Object msg = assembleRequestId(tuple.getMessage());
            infoLog.info(msg, tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(msg, DebugType.info.name(), tuple.getThrowable());
        }
    }

    /**
     * 输出一条 INFO 日志。内容根据格式化模板和格式化参数输出。 如果 INFO 级别被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param argA 第一个格式化参数。
     * @param argB 第二个格式化参数。
     */
    public static void info(String format, Object argA, Object argB) {
        if (infoLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.format(format, argA, argB);
            Object msg = assembleRequestId(tuple.getMessage());
            infoLog.info(msg, tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(msg, DebugType.info.name(), tuple.getThrowable());
        }
    }

    /**
     * 输出一条 INFO 日志。内容根据格式化模板和格式化参数输出。 如果 INFO 级别被禁用，此方法调用会避免无用的日志内容创建过程。
     *
     * @param format 格式化模板。
     * @param arguments 三个以上一系列的格式化参数。
     */
    public static void info(String format, Object... arguments) {
        if (infoLog.isInfoEnabled()) {
            FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
            Object msg = assembleRequestId(tuple.getMessage());
            infoLog.info(msg, tuple.getThrowable());

            // 如果是vip用户，记录日志
            debug(msg, DebugType.info.name(), tuple.getThrowable());
        }
    }

    public static void warn(Object msg) {
        // 日志最后添加reuqestid，格式为r=1
        msg = assembleRequestId(msg);
        warnLog.warn(msg);

        // 如果是vip用户，记录日志
        debug(msg, DebugType.warn.name());
    }

    public static void warn(Object msg, Throwable e) {
        // 日志最后添加reuqestid，格式为r=1
        msg = assembleRequestId(msg);
        warnLog.warn(msg, e);

        // 如果是vip用户，记录日志
        debug(msg, DebugType.warn.name(), e);
    }

    public static void error(Object msg) {
        // 日志最后添加reuqestid，格式为r=1
        msg = assembleRequestId(msg);
        errorLog.error(msg);

        // 如果是vip用户，记录日志
        debug(msg, DebugType.error.name());
    }

    public static void error(Object msg, Throwable e) {
        // 日志最后添加reuqestid，格式为r=1
        msg = assembleRequestId(msg);
        errorLog.error(msg, e);

        // 如果是vip用户，记录日志
        debug(msg, DebugType.error.name(), e);
    }

    public static void biz(Object msg) {
        bizLog.info(msg);
    }

    /**
     * start a perflog session <br/>
     * PerfLogSession perfSession = ApiLogger.perf("codeblock"); perfSession.start();
     * perfSession.step("codeblock_step1"); .... perfSession.step("codeblock_step2"); ....
     * perfSession.end();
     *
     * @param tag
     * @return
     */
    public static PerfLogSession perf(String tag) {
        Log4JStopWatch stopWatch = new Log4JStopWatch(prefLogger);
        return new PerfLogSession(tag, stopWatch);
    }

    /**
     * @param tag
     * @param timeThreshold 只有 耗费时间大于 timeThreshold 的时候才会记录日志
     * @return PerfLogSession instance
     * @see #perf(String)
     */
    public static PerfLogSession perf(String tag, long timeThreshold) {
        Log4JStopWatch stopWatch = new Log4JStopWatch(prefLogger);
        stopWatch.setTimeThreshold(timeThreshold);
        return new PerfLogSession(tag, stopWatch);
    }

    public static Object assembleRequestId(Object msg) {
        if (msg != null) {
            // 日志最后添加reuqestid，格式为r=1
            msg = assembleRequestId(msg.toString(), " ");
        } else {
            msg = assembleRequestId("null", " ");
        }
        return msg;
    }

    public static String assembleRequestId(String msg, String spit) {
        String requestid = getRequestId();
        StringBuffer buf = new StringBuffer(msg);
        // 日志最后添加reuqestid，格式为r=1,requestid为空时不添加
        if (!StringUtils.isEmpty(requestid)) {
            buf.append(spit).append(requestid);
            msg = buf.toString();
        }
        return msg;
    }

    public static String getRequestId() {
        RequestTraceContext context = RequestTraceContext.get();
        String requestid = "";
        if (context != null) {
            StringBuffer buf = new StringBuffer();
            buf.append("r=").append(context.getId());
            requestid = buf.toString();
        }
        return requestid;
    }

    // 记录vip 日志时标明原来的日志。
    public static enum DebugType {
        debug, info, warn, error, exposure, trace, fire, autoTrigger, apierror, exposureLog, apistatLog;
    }
}
