package pku.abe.commons.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;

/**
 * 时间片取样统计的localcahe实现
 */
public class SampleStatsLocalCache<T> {

    private final ReentrantLock lock = new ReentrantLock();
    private static final int CYCLE_DURATION = 5 * 1000; // 10s to change Local
    public static final int HIT_RATE_MIN = 1;
    private static final int SEG_NUM = 5;
    private static final int RESULT_NUM = 50;


    private int cycleDuration;// 每隔多久转换一次buffer
    private int segDuration;// 每隔多久进行一次统计
    private int resultNum;// 最终结果保存大小
    private int minHitRate;// 最少访问次数后，才进行set


    private CacheBuffer<T> currentBuffer;
    private CacheBuffer<T> backupBuffer;
    private long lastCycleSeq = 0;
    private long lastSeq = 0;
    private AtomicLong hitCounter = new AtomicLong(0);
    private AtomicLong totalCounter = new AtomicLong(0);
    private String name;

    private static final SwitcherManager sm = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
    private static final Switcher switcher = sm.registerSwitcher("feature.localcache.enable_sample_localcache", true);
    private static final long MAX_EXPIRE_TIME = 100 * 1000;// 如果交换失败，则多长时间后，不再使用缓存

    private long lastSwitchTime = System.currentTimeMillis();

    public SampleStatsLocalCache() {
        this("default", CYCLE_DURATION, SEG_NUM, RESULT_NUM, HIT_RATE_MIN);
    }

    public SampleStatsLocalCache(String name) {
        this(name, CYCLE_DURATION, SEG_NUM, RESULT_NUM, HIT_RATE_MIN);
    }


    public SampleStatsLocalCache(String name, int cycleDuration, int segDuration, int resultNum, int minHitRate) {
        if (StringUtils.isBlank(name)) {
            name = "default";
        }
        this.cycleDuration = cycleDuration;
        this.segDuration = segDuration;
        this.resultNum = resultNum;
        this.minHitRate = minHitRate;
        this.name = name;
        this.currentBuffer = new CacheBuffer<T>(this.name, this.resultNum, this.minHitRate);
        this.backupBuffer = new CacheBuffer<T>(this.name, this.resultNum, this.minHitRate);
    }


    public T get(String key) {
        if (switcher.isClose()) {
            return null;
        }
        try {
            T result = this.currentBuffer.get(key);

            long totalCount = totalCounter.incrementAndGet(), hitCount = this.hitCounter.get();;
            if (System.currentTimeMillis() - this.lastSwitchTime >= MAX_EXPIRE_TIME) {
                result = null;
            }
            long time = System.currentTimeMillis();
            long seq = time / this.cycleDuration;
            long segSeq = time % this.cycleDuration / this.segDuration;
            boolean op = false;

            if (seq > this.lastCycleSeq) {
                lock.lock();
                if (seq > this.lastCycleSeq) {
                    this.lastCycleSeq = seq;
                    op = true;
                }
                lock.unlock();
                if (op == true) {
                    this.backupBuffer.calculate();
                    switchCurrentAndBuckup();
                }
            }
            if (!op && segSeq > this.lastSeq) {
                lock.lock();
                try {
                    if (segSeq > this.lastSeq) {
                        this.lastSeq = segSeq;
                    }
                    if (lastSeq == (cycleDuration / segDuration - 1)) {
                        lastSeq = 0;
                    }
                    op = true;
                } catch (Throwable e) {
                    // 需要避免因为异常抛出，导致后面lock没有unlock
                    ApiLogger.error("catch a throwable:", e);
                }
                lock.unlock();
                if (op) {
                    boolean isStated = this.backupBuffer.isInStat(key);
                    this.backupBuffer.stat(key);
                    if (result != null && !isStated) {// 每一轮交换，对于第一个key，都让其穿透，从而保证cache的数据最长不超过CYCLE_DURATION，同时命中率不会太低
                        result = null;// 让此次请求穿透，从而更新备份缓存
                    }
                }
            }
            if (result != null) {
                hitCount = this.hitCounter.incrementAndGet();
            }
            if (totalCount % 1000 == 0 && totalCount != 0) {// 每隔一千个，打印一次日志
                ApiLogger.info("[SampleStatsCache." + this.name + ".get]total:" + totalCount + ",hit:" + hitCount + ", hitRate:"
                        + ((double) hitCount / (double) totalCount));
            }
            return result;
        } catch (Exception e) {
            ApiLogger.error("[SampleStatsCache.exception]key:" + key, e);
            return null;
        }
    }

    public void set(String key, T value) {
        if (switcher.isClose()) {
            return;
        }
        if (key == null || value == null) {
            return;
        }
        try {
            if (this.currentBuffer.isInStat(key) || this.backupBuffer.isInStat(key)) {// 控制临时缓存对象数量，数量等于CYCLE_DURATION/SEG_NUM
                this.backupBuffer.set(key, value);
            }
        } catch (Exception e) {
            ApiLogger.error("[SampleStatsCache.exception]key:" + key, e);
            return;
        }
    }

    private void switchCurrentAndBuckup() {
        CacheBuffer<T> buffer = this.currentBuffer;
        this.currentBuffer = this.backupBuffer;
        this.backupBuffer = buffer;
        this.backupBuffer = new CacheBuffer<T>(this.name, this.resultNum, this.minHitRate);
        this.lastSwitchTime = System.currentTimeMillis();
        ApiLogger.info("[SampleStatsCache." + this.name + ".switching]hit:" + this.hitCounter + ", totalCounter:" + this.totalCounter);
        // ApiLogger.info("[SampleStatsCache."+this.name+".switching]cache
        // items:"+currentBuffer.statList.toString());
        totalCounter.set(0);
        hitCounter.set(0);
        this.lastSeq = 0;
    }

    private static class CacheBuffer<T> {
        private Map<String, T> cache;
        private List<Item<T>> statList;
        private Set<String> statSet;
        private String name;


        private int capacity;
        private int hitRate;

        public CacheBuffer(String name, int capacity, int hitRate) {
            this.capacity = capacity;
            this.hitRate = hitRate;
            this.cache = new ConcurrentHashMap<String, T>();// 当前的set逻辑，是之前一个时间段，被统计的，以及当前时间段被统计的总数，所以最多可能有两倍
            this.statList = Collections.synchronizedList(new ArrayList<Item<T>>(100));
            this.statSet = Collections.synchronizedSet(new HashSet<String>(100));
            this.name = name;
        }

        public void set(String key, T o) {
            this.cache.put(key, o);
        }

        public void stat(String key) {
            this.statList.add(new Item<T>(key));
            this.statSet.add(key);
        }

        public T get(String key) {
            return this.cache.get(key);
        }

        public void calculate() {
            Map<String, Item<T>> map = new HashMap<String, Item<T>>();
            for (Item<T> item : this.statList) {
                if (item == null) {
                    continue;
                }
                if (!map.containsKey(item.name)) {
                    map.put(item.name, item);
                } else {
                    Item<T> mapitem = map.get(item.name);
                    mapitem.count++;
                }
            }
            List<Item<T>> items = new ArrayList<Item<T>>(map.values());
            Map<String, T> resultMap = new HashMap<String, T>();
            Collections.sort(items);
            StringBuilder sb = new StringBuilder();
            for (int i = items.size() - 1; i >= 0; i--) {
                Item<T> item = items.get(i);
                if (item.count > this.hitRate && resultMap.size() < this.capacity && !resultMap.containsKey(item.name)) {
                    T t = cache.get(item.name);
                    if (t != null) {
                        sb.append("name:").append(item.name).append(",count:").append(item.count).append(";");
                        resultMap.put(item.name, cache.get(item.name));
                    }
                } else {
                    break;
                }
            }
            ApiLogger.info("[SampleStatsCache.calculate" + name + "]" + sb.toString());
            this.cache = resultMap;
            this.statList = items;
        }

        public boolean isInStat(String key) {
            if (key == null) {
                return false;
            }
            return this.statSet.contains(key);
        }

        private static class Item<T> implements Comparable<Item<T>> {
            public String name;
            public int count;

            public Item(String name) {
                this.name = name;
                this.count = 1;
            }

            public int compareTo(Item<T> o) {
                if (this.count > o.count) {
                    return 1;
                } else if (this.count < o.count) {
                    return -1;
                }
                return 0;
            }

            public String toString() {
                return "name:" + name + ",count:" + count;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((name == null) ? 0 : name.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj == null) return false;
                if (getClass() != obj.getClass()) return false;
                Item other = (Item) obj;
                if (name == null) {
                    if (other.name != null) return false;
                } else if (!name.equals(other.name)) return false;
                return true;
            }
        }

        public String toString() {
            return this.cache.toString();
        }
    }
}

