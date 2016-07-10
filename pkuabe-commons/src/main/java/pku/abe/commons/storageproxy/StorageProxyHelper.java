package pku.abe.commons.storageproxy;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.log.StatLog;
import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;
import pku.abe.commons.thread.TraceableThreadExecutor;

/**
 * cache set backe 进行并行化处理，队列中最多放10000个批回写任务，超过则丢
 *
 * @version V1.0 created at: 2011-6-27 下午06:34:45
 */
public class StorageProxyHelper<T> {

    public static Switcher dirtyCacheSwitcher =
            SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager().getSwitcher("feature.status.cache.dirty");

    public static ThreadPoolExecutor proxyPool =
            new TraceableThreadExecutor(48, 48, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new AbortPolicy());

    private static int MAX_COUNT_IN_QUEUE = 10000;

    private static SwitcherManager swManager;

    private static Switcher dirtyDataWriteSwitcher;

    static {
        StatLog.registerExecutor("store_proxy_pool", proxyPool);
        swManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
        dirtyDataWriteSwitcher = swManager.registerSwitcher("feature.storageproxy.dirtydata.writeback", false);
    }


    @SuppressWarnings("unchecked")
    public static Future<Boolean> submit(StorageAble storage, Map<String, ? extends Object> kvs) {
        if (proxyPool.getQueue().size() < MAX_COUNT_IN_QUEUE) {
            ProxyRecacheTask task = new ProxyRecacheTask(storage, kvs);
            return proxyPool.submit(task);
        }
        StatLog.inc("abort_recache");
        ApiLogger.warn("Stop cache values which are from db to cache");
        return null;
    }

    public static Future<Boolean> submit(StorageAble preferStorage, Map<String, ? extends Object> kvs, String[] leftKeys,
            StorageAble backupStorage) {
        if (proxyPool.getQueue().size() < MAX_COUNT_IN_QUEUE) {
            ProxyRecacheTask task = new ProxyRecacheTask(preferStorage, kvs, leftKeys, backupStorage);
            return proxyPool.submit(task);
        }
        StatLog.inc("abort_recache");
        ApiLogger.warn("Stop cache values which are from db to cache");
        return null;
    }


    private static class ProxyRecacheTask<T> implements Callable<Boolean> {
        private StorageAble<T> preferStorage;
        private Map<String, T> kvs;
        private String[] leftKeys;
        private StorageAble<T> backupStorage;

        private ProxyRecacheTask(StorageAble<T> storage, Map<String, T> kvs) {
            this.preferStorage = storage;
            this.kvs = kvs;
        }

        private ProxyRecacheTask(StorageAble<T> storage, Map<String, T> kvs, String[] leftKeys, StorageAble<T> backupStorage) {
            this.preferStorage = storage;
            this.kvs = kvs;
            this.leftKeys = leftKeys;
            this.backupStorage = backupStorage;
        }

        @Override
        public Boolean call() throws Exception {
            Map<String, T> values2 = null;
            if (leftKeys != null && backupStorage != null) {
                values2 = backupStorage.getMulti(leftKeys);
                if (values2 != null) {
                    kvs.putAll(values2);
                }
            }

            for (Map.Entry<String, T> entry : kvs.entrySet()) {
                String key = entry.getKey();
                T value = entry.getValue();
                if (value != null) {
                    boolean isDirty = false;
                    boolean isContentMissed = false;
                    boolean isUserTypeMissed = false;
                    // if (value instanceof DoubleLongitudeVectorItem) {
                    // isDirty = ((DoubleLongitudeVectorItem) value).isDirty();
                    // } else if (value instanceof VectorItem) {
                    // isDirty = ((VectorItem) value).isDirty();
                    // } else if ( value instanceof byte[]){
                    // String suffix = StorageAble.getKeySuffix(key);
                    // if (StorageAble.CacheSuffix.META_VECTOR_STATUS_DATE.equals(suffix)
                    // || StorageAble.CacheSuffix.META_VECTOR_STATUS_LATEST.equals(suffix)
                    // || StorageAble.CacheSuffix.PAGE_VECTOR_STATUS_DATE.equals(suffix)) {
                    // MetaItem item = MetaItemPBUtil.toItem((byte[]) value);
                    // isDirty = MetaItemPBUtil.isDirty(item);
                    // isContentMissed = MetaItemPBUtil.isContentMissed(item);
                    // isUserTypeMissed = MetaItemPBUtil.isUserTypeMissed(item);
                    // }
                }
                // if((isContentMissed || isUserTypeMissed) && dirtyDataWriteSwitcher.isClose()){
                // continue;
                // } else if (isDirty && dirtyCacheSwitcher.isOpen()) {
                // preferStorage.set(key, value, Constants.EXPTIME_VECTOR_DIRTY);
                // ApiLogger.warn("StorageProxyHelper cache set dirty value. key:" + key + ",
                // expire: + " + Constants.EXPTIME_VECTOR_DIRTY.getTime());
                // } else if (isContentMissed && dirtyCacheSwitcher.isOpen()) {
                // preferStorage.set(key, value, Constants.EXPTIME_META_VECTOR_CONTENT_MISSED);
                // ApiLogger.warn("StorageProxyHelper cache set dirty value. key:" + key + ",
                // expire: + " + Constants.EXPTIME_META_VECTOR_CONTENT_MISSED.getTime());
                // } else if (isUserTypeMissed && dirtyCacheSwitcher.isOpen()) {
                // preferStorage.set(key, value, Constants.EXPTIME_META_VECTOR_USERTYPE_MISSED);
                // ApiLogger.warn("StorageProxyHelper cache set usertype dirty value. key:" + key +
                // ", expire: + " + Constants.EXPTIME_META_VECTOR_USERTYPE_MISSED.getTime());
                // } else {
                // preferStorage.set(key, value);
                // }
                // }
            }
            return true;
        }
    }
}
