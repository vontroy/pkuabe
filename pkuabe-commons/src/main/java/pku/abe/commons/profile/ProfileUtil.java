package pku.abe.commons.profile;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import pku.abe.commons.json.JsonBuilder;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

public class ProfileUtil {

    public static String SEPARATE = "\\|";
    public static String PORT_SEPARATE = ":";
    public static String IP_SEPARATE = ",";

    public static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    public static ConcurrentMap<String, AccessStatisticItem> accessStatistics = new ConcurrentHashMap<String, AccessStatisticItem>();

    static {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // access statistic
                logAccessStatistic(true);
            }
        }, ProfileConstants.STATISTIC_PEROID, ProfileConstants.STATISTIC_PEROID, TimeUnit.SECONDS);
    }

    public static void accessStatistic(String type, String name, long currentTimeMillis, long costTimeMillis) {
        accessStatistic(type, name, currentTimeMillis, costTimeMillis, ProfileConstants.SLOW_COST);
    }

    private static String getMCKey(String type, String name) {
        String[] hostPorts = name.split(IP_SEPARATE);
        if (StringUtils.isNotEmpty(hostPorts[0])) {
            String[] hostPort = hostPorts[0].split(PORT_SEPARATE);
            if (ArrayUtils.isNotEmpty(hostPort) && hostPort.length == 2) {
                return type + "|" + hostPort[1];
            }
        }
        return null;
    }

    public static void accessStatistic(String type, String name, long currentTimeMillis, long costTimeMillis, long slowThredshold) {
        String key = type + "|" + name;
        if (ProfileType.MC.value().equals(type) && StringUtils.isNotEmpty(name)) {
            String mcKey = getMCKey(type, name);
            if (StringUtils.isNotEmpty(mcKey)) {
                key = mcKey;
            }
        }

        try {
            AccessStatisticItem item = getStatisticItem(key, currentTimeMillis, slowThredshold);
            item.statistic(currentTimeMillis, costTimeMillis);
        } catch (Exception e) {}
    }

    public static AccessStatisticItem getStatisticItem(String key, long currentTime, long slowThredshold) {
        AccessStatisticItem item = accessStatistics.get(key);

        if (item == null) {
            accessStatistics.putIfAbsent(key,
                    new AccessStatisticItem(key, currentTime, ProfileConstants.STATISTIC_PEROID * 2, slowThredshold));
            item = accessStatistics.get(key);
        }

        return item;
    }

    public static void logAccessStatistic(boolean clear) {
        DecimalFormat mbFormat = new DecimalFormat("#0.00");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long currentTimeMillis = System.currentTimeMillis();

        for (Map.Entry<String, AccessStatisticItem> entry : accessStatistics.entrySet()) {
            AccessStatisticItem item = entry.getValue();

            AccessStatisticResult result = item.getStatisticResult(currentTimeMillis, ProfileConstants.STATISTIC_PEROID);

            if (clear) {
                item.clearStatistic(currentTimeMillis, ProfileConstants.STATISTIC_PEROID);
            }

            String key = entry.getKey();
            String[] keys = key.split(SEPARATE);
            if (keys.length != 2) {
                continue;
            }
            String type = keys[0];
            String name = keys[1];

            JsonBuilder jsonBuilder = new JsonBuilder();
            jsonBuilder.append("timestamp", df.format(new Date()));
            jsonBuilder.append("type", type);
            jsonBuilder.append("name", name);
            jsonBuilder.append("slowThreshold", result.slowThreshold);
            if (result.totalCount == 0) {
                jsonBuilder.append("total_count", 0);
                jsonBuilder.append("slow_count", 0);
                jsonBuilder.append("avg_time", 0.00);
                // jsonBuilder.append("avg_tps", 0);
                // jsonBuilder.append("max_tps", 0);
                // jsonBuilder.append("min_tps", 0);
                jsonBuilder.append("interval1", 0);
                jsonBuilder.append("interval2", 0);
                jsonBuilder.append("interval3", 0);
                jsonBuilder.append("interval4", 0);
                jsonBuilder.append("interval5", 0);

            } else {
                jsonBuilder.append("total_count", result.totalCount);
                jsonBuilder.append("slow_count", result.slowCount);
                jsonBuilder.append("avg_time", Double.parseDouble(mbFormat.format(result.costTime / result.totalCount)));
                // jsonBuilder.append("avg_tps", result.totalCount /
                // ProfileConstants.STATISTIC_PEROID);
                // jsonBuilder.append("max_tps", result.maxCount);
                // jsonBuilder.append("min_tps", result.minCount > 0 ? result.minCount : 0);
                jsonBuilder.append("interval1", result.intervalCounts[0]);
                jsonBuilder.append("interval2", result.intervalCounts[1]);
                jsonBuilder.append("interval3", result.intervalCounts[2]);
                jsonBuilder.append("interval4", result.intervalCounts[3]);
                jsonBuilder.append("interval5", result.intervalCounts[4]);
            }
            String monitorInfo = jsonBuilder.flip().toString();
            ProfileLoggerUtil.monitor(monitorInfo);
        }
    }

    public static class AccessStatisticItem {
        private String name;
        private int currentIndex;
        private AtomicInteger[] costTimes = null;
        private AtomicInteger[] totalCounter = null;
        private AtomicInteger[] slowCounter = null;
        private Histogram histogram = null;
        private int length;
        private long slowThreshold;
        private AtomicInteger[] interval1 = null;
        private AtomicInteger[] interval2 = null;
        private AtomicInteger[] interval3 = null;
        private AtomicInteger[] interval4 = null;
        private AtomicInteger[] interval5 = null;

        public AccessStatisticItem(String name, long currentTimeMillis, long slowThreshold) {
            this(name, currentTimeMillis, ProfileConstants.STATISTIC_PEROID * 2, slowThreshold);
        }

        public AccessStatisticItem(String name, long currentTimeMillis, int length, long slowThreshold) {
            this.name = name;
            this.costTimes = initAtomicIntegerArr(length);
            this.totalCounter = initAtomicIntegerArr(length);
            this.slowCounter = initAtomicIntegerArr(length);
            this.length = length;
            this.slowThreshold = slowThreshold;
            this.interval1 = initAtomicIntegerArr(length);
            this.interval2 = initAtomicIntegerArr(length);
            this.interval3 = initAtomicIntegerArr(length);
            this.interval4 = initAtomicIntegerArr(length);
            this.interval5 = initAtomicIntegerArr(length);
            this.currentIndex = getIndex(currentTimeMillis, length);
            this.histogram = InternalMetricsFactory.getRegistryInstance(name)
                    .histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis"));
        }

        private AtomicInteger[] initAtomicIntegerArr(int size) {
            AtomicInteger[] arrs = new AtomicInteger[size];
            for (int i = 0; i < arrs.length; i++) {
                arrs[i] = new AtomicInteger(0);
            }

            return arrs;
        }

        /**
         * currentTimeMillis: 此刻记录的时间 (ms) costTimeMillis: 这次操作的耗时 (ms)
         *
         * @param currentTimeMillis
         * @param costTimeMillis
         */
        void statistic(long currentTimeMillis, long costTimeMillis) {
            int tempIndex = getIndex(currentTimeMillis, length);

            if (currentIndex != tempIndex) {
                synchronized (this) {
                    // 这一秒的第一条统计，把对应的存储位的数据置0
                    if (currentIndex != tempIndex) {
                        reset(tempIndex);
                        currentIndex = tempIndex;
                    }
                }
            }

            costTimes[currentIndex].addAndGet((int) costTimeMillis);
            totalCounter[currentIndex].incrementAndGet();

            if (costTimeMillis >= slowThreshold) {
                slowCounter[currentIndex].incrementAndGet();
            }
            String type = name.split(ProfileUtil.SEPARATE)[0];
            if (type.equals(ProfileType.API.value())) {
                if (costTimeMillis < ProfileConstants.API_INTERVAL1) {
                    interval1[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.API_INTERVAL1 && costTimeMillis < ProfileConstants.API_INTERVAL2) {
                    interval2[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.API_INTERVAL2 && costTimeMillis < ProfileConstants.API_INTERVAL3) {
                    interval3[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.API_INTERVAL3 && costTimeMillis < ProfileConstants.API_INTERVAL4) {
                    interval4[currentIndex].incrementAndGet();
                } else {
                    interval5[currentIndex].incrementAndGet();
                }
            }
            else if (type.equals(ProfileType.SERVICE.value()) || type.equals(ProfileType.HTTP.value())) {
                if (costTimeMillis < ProfileConstants.SERVICE_INTERVAL1) {
                    interval1[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.SERVICE_INTERVAL1 && costTimeMillis < ProfileConstants.SERVICE_INTERVAL2) {
                    interval2[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.SERVICE_INTERVAL2 && costTimeMillis < ProfileConstants.SERVICE_INTERVAL3) {
                    interval3[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.SERVICE_INTERVAL3 && costTimeMillis < ProfileConstants.SERVICE_INTERVAL4) {
                    interval4[currentIndex].incrementAndGet();
                } else {
                    interval5[currentIndex].incrementAndGet();
                }
            } else if (type.equals(ProfileType.MC.value()) || type.equals(ProfileType.REDIS.value())
                    || type.equals(ProfileType.DB.value()) || type.equals(ProfileType.MCQ.value())) {
                if (costTimeMillis < ProfileConstants.RESOURCE_INTERVAL1) {
                    interval1[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.RESOURCE_INTERVAL1 && costTimeMillis < ProfileConstants.RESOURCE_INTERVAL2) {
                    interval2[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.RESOURCE_INTERVAL2 && costTimeMillis < ProfileConstants.RESOURCE_INTERVAL3) {
                    interval3[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.RESOURCE_INTERVAL3 && costTimeMillis < ProfileConstants.RESOURCE_INTERVAL4) {
                    interval4[currentIndex].incrementAndGet();
                } else {
                    interval5[currentIndex].incrementAndGet();
                }
            } else if (type.equals(ProfileType.HBASE.value()) || type.equals(ProfileType.HBASEFAILFAST.value())) {
                if (costTimeMillis < ProfileConstants.STORAGE_RESOURCE_INTERVAL1) {
                    interval1[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.STORAGE_RESOURCE_INTERVAL1
                        && costTimeMillis < ProfileConstants.STORAGE_RESOURCE_INTERVAL2) {
                    interval2[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.STORAGE_RESOURCE_INTERVAL2
                        && costTimeMillis < ProfileConstants.STORAGE_RESOURCE_INTERVAL3) {
                    interval3[currentIndex].incrementAndGet();
                } else if (costTimeMillis >= ProfileConstants.STORAGE_RESOURCE_INTERVAL3
                        && costTimeMillis < ProfileConstants.STORAGE_RESOURCE_INTERVAL4) {
                    interval4[currentIndex].incrementAndGet();
                } else {
                    interval5[currentIndex].incrementAndGet();
                }
            }
            histogram.update(costTimeMillis);
            InternalMetricsFactory.getRegistryInstance(name).histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis"))
                    .update(costTimeMillis);
        }

        public void statistic(long currentTimeMillis, int[] intervals) {
            if (intervals.length != 5) {
                throw new IllegalArgumentException("illegal intervals:" + Arrays.toString(intervals));
            }

            int tempIndex = getIndex(currentTimeMillis, length);
            if (currentIndex != tempIndex) {
                synchronized (this) {
                    // 这一秒的第一条统计，把对应的存储位的数据置0
                    if (currentIndex != tempIndex) {
                        reset(tempIndex);
                        currentIndex = tempIndex;
                    }
                }
            }

            int sum = 0;
            for (int interval : intervals) {
                sum += interval;
            }
            costTimes[currentIndex].addAndGet(sum);
            totalCounter[currentIndex].incrementAndGet();

            interval1[currentIndex].addAndGet(intervals[0]);
            interval2[currentIndex].addAndGet(intervals[1]);
            interval3[currentIndex].addAndGet(intervals[2]);
            interval4[currentIndex].addAndGet(intervals[3]);
            interval5[currentIndex].addAndGet(intervals[4]);
            histogram.update(sum);
            InternalMetricsFactory.getRegistryInstance(name).histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis"))
                    .update(sum);
        }

        private int getIndex(long currentTimeMillis, int periodSecond) {
            return (int) ((currentTimeMillis / 1000) % periodSecond);
        }

        private void reset(int index) {
            costTimes[index].set(0);
            totalCounter[index].set(0);
            slowCounter[index].set(0);
            interval1[index].set(0);
            interval2[index].set(0);
            interval3[index].set(0);
            interval4[index].set(0);
            interval5[index].set(0);
        }

        AccessStatisticResult getStatisticResult(long currentTimeMillis, int peroidSecond) {
            long currentTimeSecond = currentTimeMillis / 1000;
            currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

            int startIndex = getIndex(currentTimeSecond * 1000, length);

            AccessStatisticResult result = new AccessStatisticResult();

            result.slowThreshold = slowThreshold;
            for (int i = 0; i < peroidSecond; i++) {
                int currentIndex = (startIndex - i + length) % length;

                result.costTime += costTimes[currentIndex].get();
                result.totalCount += totalCounter[currentIndex].get();
                result.slowCount += slowCounter[currentIndex].get();
                result.intervalCounts[0] += interval1[currentIndex].get();
                result.intervalCounts[1] += interval2[currentIndex].get();
                result.intervalCounts[2] += interval3[currentIndex].get();
                result.intervalCounts[3] += interval4[currentIndex].get();
                result.intervalCounts[4] += interval5[currentIndex].get();
                if (totalCounter[currentIndex].get() > result.maxCount) {
                    result.maxCount = totalCounter[currentIndex].get();
                } else if (totalCounter[currentIndex].get() < result.minCount || result.minCount == -1) {
                    result.minCount = totalCounter[currentIndex].get();
                }
            }

            return result;
        }

        void clearStatistic(long currentTimeMillis, int peroidSecond) {
            long currentTimeSecond = currentTimeMillis / 1000;
            currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

            int startIndex = getIndex(currentTimeSecond * 1000, length);

            for (int i = 0; i < peroidSecond; i++) {
                int currentIndex = (startIndex - i + length) % length;

                reset(currentIndex);
            }
        }
    }
}
