package pku.abe.commons.storageproxy;

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import pku.abe.commons.memcache.CacheAble;
import pku.abe.commons.memcache.CasValue;
import pku.abe.commons.util.Constants;
import pku.abe.commons.util.UseTimeStasticsMonitor;

public abstract class CacheAbleHelper<V> implements CacheAble<V> {
    private static final String CacheableHelperMonitor = "CacheableHelperMonitor";

    /**
     * 查询cache
     *
     * @param cacheKeys, key由rawkey+suffix组成
     * @return Map<String, V>
     */
    protected abstract Map<String, V> getFromPrefer(Collection<String> cacheKeys);

    /**
     * Cache没有，查db
     *
     * @param rawKeys
     * @return Map<String, V>
     */
    protected abstract Map<String, V> getFromBackup(Collection<String> rawKeys);

    /**
     * cache中没有，而db中有的一条数据，要缓存到cache中
     *
     * @param key
     * @param value null should be handled when getFromBackup doesn't return.
     */
    protected abstract void recache(String key, V value);

    /**
     * 统计监控的名称
     *
     * @return
     */
    protected abstract String getMonitorName();

    protected boolean hasBackup = true;

    @Override
    public boolean add(String arg0, V arg1, Date arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(String arg0, V arg1) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean cas(String arg0, CasValue<V> arg1, Date arg2) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean cas(String arg0, CasValue<V> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CasValue<V> getCas(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean set(String arg0, V arg1, Date arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean set(String arg0, V arg1) {
        throw new UnsupportedOperationException();
    }

    /**
     * 查询单条数据
     */
    @Override
    public V get(final String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }

        UseTimeStasticsMonitor monitor = getMonitor();
        LinkedList<Long> stamps = monitor.start(null, false);
        boolean hit = true;
        try {
            Map<String, V> values = getFromPrefer(Arrays.asList(key));
            V value = values == null ? null : values.get(key);
            monitor.mark(stamps, false);

            if (hasBackup && value == null) {
                Map<String, V> backupValues = getFromBackup(Arrays.asList(key));
                final V backValue = backupValues == null ? null : backupValues.get(key);
                value = backValue;
                monitor.mark(stamps, false);

                StorageProxyHelper.proxyPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        UseTimeStasticsMonitor monitor = getMonitor();
                        LinkedList<Long> stamps = monitor.start(null, false);
                        recache(key, backValue);
                        monitor.end(stamps, "set_back_cache", false, false, Constants.OP_CACHE_TIMEOUT);
                    }
                });
            }
            return value;
        } finally {
            monitor.end(stamps, "get", hit, false, Constants.OP_CACHE_TIMEOUT);
        }
    }

    /**
     * 批量查询
     */
    @Override
    public Map<String, V> getMulti(String[] keys) {
        if (ArrayUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        UseTimeStasticsMonitor monitor = getMonitor();
        LinkedList<Long> stamps = monitor.start(null, false);

        boolean hit = true;
        try {
            // keys, 不带suffix; values, key也是不带suffix
            Map<String, V> values = getFromPrefer(Arrays.asList(keys));
            monitor.mark(stamps, false);

            if (hasBackup && (values == null || values.size() < keys.length)) {
                hit = false;

                final Collection<String> missedKeys = new LinkedList<String>(Arrays.asList(keys));
                missedKeys.removeAll(values.keySet());

                // 查询DB
                final Map<String, V> backupValues = getFromBackup(missedKeys);
                monitor.mark(stamps, false);

                // 回中缓存
                StorageProxyHelper.proxyPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        UseTimeStasticsMonitor monitor = getMonitor();
                        LinkedList<Long> stamps = monitor.start(null, false);
                        if (backupValues == null) {
                            for (String missKey : missedKeys) {
                                recache(missKey, null);
                            }
                        } else {
                            for (String missKey : missedKeys) {
                                V backupValue = backupValues.get(missKey);
                                recache(missKey, backupValue);
                            }
                        }

                        monitor.end(stamps, "set_back_cache", false, false, Constants.OP_CACHE_TIMEOUT);
                    }
                });
                values.putAll(backupValues);
            }

            // cacheValue与dbValue合并
            return values;
        } finally {
            monitor.end(stamps, "get_multi", hit, false, Constants.OP_CACHE_TIMEOUT);
        }
    }

    public static String getCacheKey(String rawKey, String suffix) {
        if (StringUtils.isBlank(rawKey)) {
            throw new IllegalArgumentException("illegal cache raw key=" + rawKey);
        }

        if (StringUtils.isNotBlank(suffix)) {
            return rawKey + suffix;
        } else {
            return rawKey;
        }
    }

    /**
     * rawkey和suffix组成cache中查询需要的cacheKey
     *
     * @param rawKeys
     * @param suffix
     * @return cacheKey
     */
    public static Collection<String> getCacheKeys(Collection<String> rawKeys, String suffix) {
        if (CollectionUtils.isEmpty(rawKeys)) {
            return Collections.emptyList();
        }

        if (StringUtils.isNotBlank(suffix)) {
            Collection<String> keys = new ArrayList<String>(rawKeys.size());
            for (String rawkey : rawKeys) {
                keys.add(getCacheKey(rawkey, suffix));
            }
            return keys;
        } else {
            return rawKeys;
        }
    }

    private UseTimeStasticsMonitor getMonitor() {
        String monitorName = getMonitorName();
        if (StringUtils.isBlank(monitorName)) {
            monitorName = CacheableHelperMonitor;
        }

        return UseTimeStasticsMonitor.getInstance(monitorName);
    }
}
