package pku.abe.commons.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import pku.abe.commons.log.LogCollectorFactory;
import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.json.JsonBuilder;

public class TimeStatUtil {
    // 规则：第一位表示资源类型，第二位表示是否批量，后五位表示端口
    public final static int MC_TYPE = 1000000;
    public final static int REDIS_TYPE = 2000000;
    public final static int DB_TYPE = 3000000;
    public final static int HBASE_TYPE = 4000000;
    public final static int[] TIMEARR = new int[] {1, 5, 10, 20, 30, 50, 100, 200, 300, 500};// 区间值列表
    public final static int MULTI_TYPE = 100000;
    public final static int TOTAL_COUNT = 0;
    public final static int ERROR_COUNT = -1;
    private static int max_try_time = 2;

    public static void setMaxTryTime(int retry) {
        max_try_time = retry;
    }

    private static class TimeStat {

        // 定义两个map用来存储上下行各区间计数，避免获取时拼接字符串做key，影响性能
        ConcurrentHashMap<Integer, AtomicLong> wTimeMap = new ConcurrentHashMap<Integer, AtomicLong>();
        ConcurrentHashMap<Integer, AtomicLong> rTimeMap = new ConcurrentHashMap<Integer, AtomicLong>();
        AtomicLong wTotal = new AtomicLong(0);// 上行总计数
        AtomicLong rTotal = new AtomicLong(0);// 下行总计数

        // 初始化
        {
            for (int t : TIMEARR) {
                wTimeMap.put(t, new AtomicLong(0));
                rTimeMap.put(t, new AtomicLong(0));
            }
        }

    }

    public static ConcurrentHashMap<Integer, TimeStat> map = new ConcurrentHashMap<Integer, TimeStat>();

    public static ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicLong>> retryMap =
            new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicLong>>();

    private static Thread printThread;

    static {
        printThread = new Thread("TimeStatUtil") {
            public void run() {
                while (true) {
                    printMap();
                    // printRetryMap();
                    try {
                        // 5分钟执行一次
                        Thread.sleep(300000);
                    } catch (InterruptedException e) {}
                }
            }
        };
        // 将线程设置为守护线程
        try {
            printThread.setDaemon(true);
        } catch (Exception e) {}
        printThread.start();
    }

    // 用来校验重试机制及查看各区间的百分比，全量上线会废弃掉
    public static void addElapseTimeStat(int name, int type) {
        try {
            name = name > MULTI_TYPE ? (name - MULTI_TYPE) : name;
            if (retryMap.get(name) == null) {
                ConcurrentHashMap<Integer, AtomicLong> newMap = new ConcurrentHashMap<Integer, AtomicLong>();
                {
                    newMap.put(TOTAL_COUNT, new AtomicLong(0));// total
                    newMap.put(ERROR_COUNT, new AtomicLong(0));// error_count
                    for (int i = 1; i < max_try_time + 1; i++) {
                        newMap.put(i, new AtomicLong(0));
                    }
                }
                retryMap.put(name, newMap);
            }
            retryMap.get(name).get(type).incrementAndGet();
        } catch (Exception e) {
            //
        }
    }

    public static void printRetryMap() {
        for (int key : retryMap.keySet()) {
            ConcurrentHashMap<Integer, AtomicLong> test = retryMap.get(key);
            ConcurrentHashMap<Integer, AtomicLong> newMap = new ConcurrentHashMap<Integer, AtomicLong>();
            {
                newMap.put(TOTAL_COUNT, new AtomicLong(0));// total
                newMap.put(ERROR_COUNT, new AtomicLong(0));// error_count
                for (int i = 1; i < max_try_time + 1; i++) {
                    newMap.put(i, new AtomicLong(0));
                }
            }
            retryMap.put(key, newMap);
            StringBuilder sb = new StringBuilder("resourceInterval ").append(key - REDIS_TYPE);
            sb.append(" ").append(test.get(TOTAL_COUNT));
            for (int i = 1; i < max_try_time + 1; i++) {
                sb.append(" ").append(test.get(i));
            }
            sb.append(" ").append(test.get(ERROR_COUNT));
            ApiLogger.info(sb.toString());
        }

    }

    public static void printMap() {
        for (int key : map.keySet()) {
            TimeStat timeStat = map.get(key);
            if (timeStat.wTotal.get() > 0 || timeStat.rTotal.get() > 0) {
                map.put(key, new TimeStat());
                try {
                    printStat(key, timeStat);
                } catch (Exception e) {}
            }
        }

    }

    // 第一次执行时注册
    public static void register(int name) {
        if (map.get(name) == null) {
            map.putIfAbsent(name, new TimeStat());
            map.putIfAbsent(name + MULTI_TYPE, new TimeStat());
        }
    }

    private static TimeStat getTimeStat(int name) {
        TimeStat timeStat = map.get(name);

        if (timeStat == null) {
            register(name);
            return map.get(name);
        }

        return timeStat;

    }

    // 每次操作，将处理时间插入对应的区间内
    public static void addElapseTimeStat(int name, boolean isWriter, long startTime, long cost) {
        try {
            TimeStat timeStat = getTimeStat(name);
            if (cost == -1) {
                cost = System.currentTimeMillis() - startTime;
            }
            for (int t : TIMEARR) {
                if (cost < t) {
                    if (isWriter) {
                        timeStat.wTimeMap.get(t).incrementAndGet();
                        timeStat.wTotal.incrementAndGet();
                    } else {
                        timeStat.rTimeMap.get(t).incrementAndGet();
                        timeStat.rTotal.incrementAndGet();
                    }
                    return;
                }
            }
            // 如果以上条件都不满足，说明大于数据最大值，则只在总数中记录
            if (isWriter) {
                timeStat.wTotal.incrementAndGet();
            } else {
                timeStat.rTotal.incrementAndGet();
            }
        } catch (Exception e) {}

    }

    // 线程10分钟执行一次，将map中所有端口的数据输出到日志中
    public static void printStat(int key, TimeStat timestat) {
        String module;
        int port;
        if (key > HBASE_TYPE) {
            port = key - HBASE_TYPE;
            if (port > MULTI_TYPE) {
                module = "hbase_batch_";
                port = port - MULTI_TYPE;
            } else {
                module = "hbase_";
            }
        } else if (key > DB_TYPE) {
            module = "DB_";
            port = key - DB_TYPE;
        } else if (key > REDIS_TYPE) {
            port = key - REDIS_TYPE;
            if (port > MULTI_TYPE) {
                module = "redis_batch_";
                port = port - MULTI_TYPE;
            } else {
                module = "redis_";
            }
        } else {
            port = key - MC_TYPE;
            if (port > MULTI_TYPE) {
                module = "memcached_batch_";
                port = port - MULTI_TYPE;
            } else if (port > 0) {
                module = "memcached_";
            } else {
                return;
            }
        }

        JsonBuilder wjb = new JsonBuilder();
        JsonBuilder rjb = new JsonBuilder();
        long wt = timestat.wTotal.get();
        long rt = timestat.rTotal.get();
        wjb.append("name", module + port + "_writer");
        rjb.append("name", module + port + "_read");
        wjb.append("total", wt);
        rjb.append("total", rt);

        for (int i = 0; i < TIMEARR.length; i++) {
            wjb.append(TIMEARR[i] + "", timestat.wTimeMap.get(TIMEARR[i]).get());
            rjb.append(TIMEARR[i] + "", timestat.rTimeMap.get(TIMEARR[i]).get());
        }
        // 调用LogCollector
        // 对于调用为0的不做记录
        if (wt > 0) {
            LogCollectorFactory.getLogCollector().log("resourceInterval", module + "writer", wjb.flip().toString());
        }
        if (rt > 0) {
            LogCollectorFactory.getLogCollector().log("resourceInterval", module + "read", rjb.flip().toString());
        }

    }

}
