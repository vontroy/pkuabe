package pku.abe.commons.storageproxy;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.log.StatLog;
import pku.abe.commons.memcache.CasValue;
import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;
import pku.abe.commons.util.Constants;
import pku.abe.commons.util.UseTimeStasticsMonitor;

public class StorageProxy<T> extends StorageAble<T> {
    private static Switcher dirtyCacheSwitcher;
    private static Switcher asyncGetFromBackupSwitcher;

    static {
        SwitcherManager switcherManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
        dirtyCacheSwitcher = switcherManager.getSwitcher("feature.status.cache.dirty");
        asyncGetFromBackupSwitcher = switcherManager.registerSwitcher("feature.status.asyncGetFromBackup.enable", true);
    }

    private StorageAble<T> preferedStorage;

    private StorageAble<T> backupStorage;

    public static final boolean debug = false;

    private boolean asyncGetFromBackup = false;


    public boolean isAsyncGetFromBackup() {
        return asyncGetFromBackup;
    }

    public void setAsyncGetFromBackup(boolean asyncGetFromBackup) {
        this.asyncGetFromBackup = asyncGetFromBackup;
    }

    /**
     * return T only if cache available, no read through from db useful for build vector cache, skip
     * the non-active users.
     */
    public T getIfAvailable(String key) {
        if (key == null) {
            ApiLogger.warn("Error: key is null for StorageProxy.get(String)");
            return null;
        }
        T value = preferedStorage.get(key);
        return value;
    }

    /**
     * return T only if cache in master, no read through from db and other mcs, useful for build
     * vector cache, skip the non-active users.
     */
    public T getMasterIfAvailable(String key) {
        if (key == null) {
            ApiLogger.warn("Error: key is null for StorageProxy.getMasterIfAvailable(String)");
            return null;
        }
        T value = preferedStorage.getFromMaster(key);
        return value;
    }

    @Override
    public CasValue<T> getCasFromMaster(String key) {
        if (StringUtils.hasText(key)) {
            return preferedStorage.getCasFromMaster(key);
        } else {
            ApiLogger.warn("Error: key is null for StorageProxy.getCasMasterIfAvailable(String)");
            return null;
        }
    }

    @Override
    public T get(String key) {
        if (key == null) {
            ApiLogger.warn("Error: key is null for StorageProxy.get(String)");
            return null;
        }

        T value = preferedStorage.get(key);

        if (value == null && needSearchBackupStorage(backupStorage, key)) {
            value = backupStorage.get(key);


        }

        return value;
    }

    @Override
    public T getNotSetBack(String key) {
        if (key == null) {
            ApiLogger.warn("Error: key is null for StorageProxy.getNotBackSet(String)");
            return null;
        }

        T value = preferedStorage.get(key);

        if (value == null && needSearchBackupStorage(backupStorage, key)) {
            value = backupStorage.get(key);
        }

        return value;
    }


    @Override
    public CasValue<T> getCas(String key) {
        if (key == null) {
            ApiLogger.warn("Error: key is null for StorageProxy.get(String)");
            return null;
        }
        long t1 = System.currentTimeMillis();
        CasValue<T> value = preferedStorage.getCas(key);
        long t2 = System.currentTimeMillis();

        long t3 = -1, t4 = -1;
        if (value == null && needSearchBackupStorage(backupStorage, key)) {
            value = backupStorage.getCas(key);
            t3 = System.currentTimeMillis();
            if (value != null) {
                preferedStorage.setCas(key, value);
            }
            t4 = System.currentTimeMillis();
        }

        long preCacheGetTime = t2 - t1;
        long backCacheGetTime = t3 - t2;
        long preCacheSetbackTime = t4 - t3;
        // if(preCacheGetTime > 50){
        // StringBuilder sb = new StringBuilder(128).append("[get] key=").append(key).append(",
        // PreferCache, isMem:")
        // .append((preferedStorage instanceof MemCacheStorage)).append(",
        // isProxy=").append(preferedStorage instanceof StorageProxy)
        // .append(", preCacheGetT=").append(preCacheGetTime);
        // if(backCacheGetTime > 0){
        // sb.append(", preCacheGetT=").append(preCacheGetTime ).append(",
        // backCacheGetTime=").append( backCacheGetTime)
        // .append(", preCacheSetbackTime=").append(preCacheSetbackTime);
        // }
        // ApiLogger.logForTest(sb, null);
        // }else if(backCacheGetTime > 50){
        // ApiLogger.logForTest(new StringBuilder(64).append("[get] key=").append(key).append(",
        // PreferCache, isMem:").append((preferedStorage instanceof MemCacheStorage))
        // .append(", isProxy=").append((preferedStorage instanceof StorageProxy)).append(",
        // preCacheGetT=").append(preCacheGetTime)
        // .append(", backCacheGetTime=").append(backCacheGetTime).append(",
        // preCacheSetbackTime=").append(preCacheSetbackTime), null);
        // }
        return value;
    }

    /**
     * 支持attention取5000以后的ids
     *
     * @param backupStorage
     * @param key
     * @return
     */
    private boolean needSearchBackupStorage(StorageAble<T> backupStorage, String key) {
        if (backupStorage == null) {
            return false;
        }
        // if(key.endsWith(StorageAble.CacheSuffix.ATTENTION_FOLLOWERS)
        // && !key.endsWith(firstFollowersSuffix)){
        // return false;
        // }
        return true;
    }

    // private void setUserFollowers(UserAttentions userAttention, String key){
    // int indexCount = (userAttention.getAttentions().length - 1) / Constants.MAX_FRIEND_CACHE_SIZE
    // + 1;
    // String rawKey = StorageAble.getSqlKey(key);
    // for(int i = 1; i <= indexCount; i++){
    // String midKey = Constants.KEY_SEPERATOR + i;
    // String utKey = StorageAble.getCacheKey(rawKey, StorageAble.Type.attention_uid_followers,
    // midKey);
    // UserAttentions ut = new UserAttentions();
    // ut.setCount(userAttention.getCount());
    // ut.setUid(userAttention.getUid());
    // int from = (i - 1) * Constants.MAX_FRIEND_CACHE_SIZE;
    // int count = i < indexCount ? Constants.MAX_FRIEND_CACHE_SIZE : userAttention.getCount() %
    // Constants.MAX_FRIEND_CACHE_SIZE;
    // if(count == 0){
    // count = Constants.MAX_FRIEND_CACHE_SIZE;
    // }
    // long[] ats = new long[count];
    // System.arraycopy(userAttention.getAttentions(), from, ats, 0, count);
    // ut.setAttentions(ats);
    // preferedStorage.set(utKey, (T)ut);
    // }
    // }

    /**
     * 如果需要从backup查，则查出的数据再写入prefered cache, getMulti不从本地查也不更新本地cache
     */
    @Override
    public Map<String, T> getMulti(String[] keys) {
        if (keys == null || keys.length == 0) {
            ApiLogger.info("Error: keys is null or length is zero for StorageProxy.getMulti(String)");
            return new HashMap<String, T>(1);
        }

        // getMulti 不使用localCache
        // if(preferedStorage instanceof LocalCacheStorage){
        // // this code is problem, why ignore the following code?
        // ApiLogger.warn("Fatal config error, bad proxy config.");
        // return backupStorage.getMulti(keys);
        // }
        boolean hit = true;
        long t1 = System.currentTimeMillis();
        long t2 = 0, t3 = 0, t4 = 0;
        UseTimeStasticsMonitor mcGetsMonitor = UseTimeStasticsMonitor.getInstance(getSuffixKey(keys));
        LinkedList<Long> stamps = mcGetsMonitor.start(null, debug);
        Map<String, T> values1 = preferedStorage.getMulti(keys);
        mcGetsMonitor.mark(stamps, debug);
        t2 = System.currentTimeMillis();

        if (values1.size() < keys.length && backupStorage != null) {
            hit = false;
            Set<String> leftKeys = new HashSet<String>();
            leftKeys.addAll(Arrays.asList(keys));
            leftKeys.removeAll(values1.keySet());
            if (leftKeys.size() > 0) {
                String[] leftKeyArr = new String[leftKeys.size()];
                leftKeys.toArray(leftKeyArr);
                if (asyncGetFromBackup && asyncGetFromBackupSwitcher.isOpen() && keys.length >= Constants.MAX_FRIEND_COUNT) {
                    StorageProxyHelper.submit(preferedStorage, values1, leftKeyArr, backupStorage);
                    mcGetsMonitor.mark(stamps, debug);
                } else {
                    Map<String, T> values2 = backupStorage.getMulti(leftKeyArr);
                    mcGetsMonitor.mark(stamps, debug);
                    t3 = System.currentTimeMillis();
                    if (values2 != null) {
                        values1.putAll(values2);
                        StorageProxyHelper.submit(preferedStorage, values1);
                    }
                    mcGetsMonitor.mark(stamps, debug);
                    t4 = System.currentTimeMillis();
                }
            } else {
                ApiLogger.info(new StringBuilder(64).append("Info: duplicate key:").append(Arrays.asList(keys)));
            }
        }

        String debugInfo = null;
        // if (debug) {
        // StringBuilder sb = new StringBuilder(128).append("[getMulti]
        // keys[0]=").append(keys[0]).append(", PreferCache, isMem:")
        // .append((preferedStorage instanceof MemCacheStorage)).append(",
        // isProxy=").append(preferedStorage instanceof StorageProxy);
        // debugInfo = sb.toString();
        // }

        if (Constants.enableProfiling) {
            String keySuffix = getBareKeySuffix(keys[0]);
            if (StatLog.isCacheStatkey(keySuffix)) {
                if (t4 > 0) {
                    StatLog.incProcessTime("mc_getMulti" + keySuffix, 1, (t2 - t1));
                    StatLog.incProcessTime("db_getMult" + keySuffix, 1, (t3 - t2));
                    StatLog.incProcessTime("mc_re_setMulti" + keySuffix, 1, (t4 - t3));
                    StatLog.incProcessTime("storage_getMult_db" + keySuffix, 1, (t4 - t1));
                } else {
                    StatLog.incProcessTime("storage_getMult_cache" + keySuffix, 1, (t2 - t1));
                }
            }
        }

        mcGetsMonitor.end(stamps, debugInfo, hit, false, Constants.OP_CACHE_TIMEOUT);
        // mvd、mvl中缓存失效穿透到db回中缓存时，要获取用户的类型，回中微博到mvd和mvl。
        return values1;
    }

    /**
     * 获取key的后缀
     *
     * @param keys
     * @return
     */
    protected String getSuffixKey(String[] keys) {
        String key = getBareKeySuffix(keys[0]);
        if (key.startsWith(".") && key.length() > 0) {
            return key.substring(1);
        }
        return key;
    }

    /**
     * 继续查询的条件：1 如果一条记录都没有去到； 2：如果backup是memcache，而先查的localCache没有查完，也去memcache查
     * 3:如果memcache没有查到一个数据
     *
     * @param values
     * @param keySize
     * @return
     */
    private boolean needSearchBackupStorage(Map<String, T> values, int keySize) {
        boolean needSearch = values == null;
        // || values.size() == 0
        // || (values.size() < keySize
        // && backupStorage != null
        // && backupStorage instanceof MemCacheStorage);
        // if(!needSearch){
        // if(preferedStorage instanceof MemCacheStorage
        // || preferedStorage instanceof StorageProxy){
        // boolean hasNonNullValue = false;
        // for(T v : values.values()){
        // if(v != null){
        // hasNonNullValue = true;
        // break;
        // }
        // }
        // if(!hasNonNullValue){
        // needSearch = true;
        // }
        // }
        // }
        return needSearch;
    }

    @Override
    public boolean set(String key, T value) {
        boolean preCacheResult = preferedStorage.set(key, value);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, value) : true;

        return preCacheResult && bakCacheResult;
    }

    @Override
    public boolean set(String key, T value, Date expdate) {
        boolean preCacheResult = preferedStorage.set(key, value, expdate);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, value, expdate) : true;

        return preCacheResult && bakCacheResult;
    }

    /**
     * vika memcache client use set commond not cas
     */
    @Override
    @Deprecated
    public boolean setCas(String key, CasValue<T> value) {
        boolean preCacheResult = preferedStorage.setCas(key, value);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, (T) value.getValue()) : true;

        return preCacheResult && bakCacheResult;
    }

    /**
     * vika memcache client use set commond not cas
     */
    @Override
    @Deprecated
    public boolean setCas(String key, CasValue<T> value, Date expdate) {
        boolean preCacheResult = preferedStorage.setCas(key, value, expdate);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, (T) value.getValue(), expdate) : true;

        return preCacheResult && bakCacheResult;
    }

    // ----------- fix setCas bug -----------------
    @Override
    public boolean cas(String key, CasValue<T> value) {
        boolean preCacheResult = preferedStorage.cas(key, value);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, (T) value.getValue()) : true;

        return preCacheResult && bakCacheResult;
    }

    @Override
    public boolean cas(String key, CasValue<T> value, Date expdate) {
        boolean preCacheResult = preferedStorage.cas(key, value, expdate);
        boolean bakCacheResult = backupStorage != null ? backupStorage.set(key, (T) value.getValue(), expdate) : true;

        return preCacheResult && bakCacheResult;
    }

    @Override
    public T incr(String key) {
        T rs = preferedStorage.incr(key);
        if (backupStorage != null) {
            backupStorage.incr(key);
        }
        return rs;
    }

    public T decr(String key) {
        T t = preferedStorage.decr(key);
        if (backupStorage != null) {
            backupStorage.decr(key);
        }
        return t;
    }

    @Override
    public boolean delete(String key) {
        boolean preferedStorageDeleteResult = preferedStorage.delete(key);
        boolean backupStorageDeleteResult = backupStorage != null ? backupStorage.delete(key) : true;

        return preferedStorageDeleteResult && backupStorageDeleteResult;
    }

    public StorageAble<T> getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(StorageAble<T> backupStorage) {
        this.backupStorage = backupStorage;
    }

    public StorageAble<T> getPreferedStorage() {
        return preferedStorage;
    }

    public void setPreferedStorage(StorageAble<T> preferedStorage) {
        this.preferedStorage = preferedStorage;
    }
}
