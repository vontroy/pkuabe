package pku.abe.commons.memcache;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.storageproxy.StorageAble;
import pku.abe.commons.util.ApiUtil;

public class MemCacheStorage<T> extends StorageAble<T> implements CacheAble<T> {

    // set by McqProcessor.startReading
    public static boolean isInProcessor = false;

    static int LOCK_EXPIRE_MILLIS = 2000;
    static Date LOCK_EXPIRE_TIME = new Date(LOCK_EXPIRE_MILLIS); // 2 second

    Date expireTime;

    // the using cache, which also is master
    MemcacheClient cacheClientMaster;
    // master L1 cache list
    List<MemcacheClient> cacheClientL1;
    // the backup/olden cache, when add or reduce cache，we need to get data from backup cache
    MemcacheClient cacheClientBackup;

    // the slave/standby cache, for double set，to avoid single point
    MemcacheClient cacheClientSlave;
    // slave L1 cache list
    List<MemcacheClient> cacheClientSlaveL1;
    MemcacheClient cacheClientTemp;

    // 暂时不再从backup清理数据，bakcup的cache中老数据依靠踢出、过期来完成清理， 等永丰稳定后，再设为true
    boolean deleteFrmBackupCache = false;

    boolean updateTempCache = true;

    // slaveL1通过配置为其它机房的缓存，通过updateSlaveL1来控制队列机是否更新其它机房的缓存
    AtomicBoolean updateSlaveL1 = new AtomicBoolean(false);

    // 通过配置readOnly来控制队列机是否更新缓存
    AtomicBoolean readOnly = new AtomicBoolean(false);

    AtomicInteger point = new AtomicInteger(0);


    /**
     * get时，如果主cache没有，尝试从老的cache中获取，如果获取到，把数据写到主cache，并从老cache清理，最后返回该数据
     *
     * @see 当从老的cache获取的casValue数据会去掉cas 标记，避免迁移时cas失败
     */
    @Override
    public T get(String key) {
        T value = null;
        MemcacheClient client = null;

        if (cacheClientL1 != null && cacheClientL1.size() > 0) {
            client = chooseOneL1Client();
            if (client != null) {
                value = get(key, client);
            }
        }

        // L1获取到值之后应该直接返回，避免重新set L1导致脏数据过期时间被刷新 chenfei 2014.4.16
        if (value != null) {
            return value;
        }

        if (value == null) {
            value = get(key, cacheClientMaster);
        }

        if (value == null && cacheClientBackup != null) {
            value = getAndSetFromBackupCache(key);
        }

        if (value == null && cacheClientTemp != null) {
            value = get(key, cacheClientTemp);
        }

        // 如果master没有，就尝试从slave来取，去掉content的限制，所有的cache都需要从slave来取 fishermen 2011.8.4
        if (value == null && cacheClientSlave != null) {
            value = get(key, cacheClientSlave);
        }

        if (value != null && client != null) {
            set(key, value, expireTime, client, false);
        }

        return value;
    }

    /**
     * get value only from master via key
     *
     * @param rawKey
     * @return
     */
    public T getFromMaster(String key) {
        return get(key, cacheClientMaster);
    }

    @Override
    public CasValue<T> getCasFromMaster(String key) {
        return (CasValue) cacheClientMaster.gets(key);
    }

    /**
     * 以len/(len + 1)的概率调用L1，剩余请求穿透
     *
     * @return
     */
    private MemcacheClient chooseOneL1Client() {
        int v = point.incrementAndGet();
        if (v > 1000000000) {
            point.set(0);
        }
        // FIXME 不符合方法设计原则，需要重构，by daoru, 2011-10-21
        int len = cacheClientL1.size();
        v = v % (len + 1);
        if (v >= len) {
            return null;
        }
        return cacheClientL1.get(v);
    }

    /**
     * getCas支持不了L1 cache, 因为只能是master与master自己的比较
     *
     * @param key
     * @return
     */
    @Override
    public CasValue<T> getCas(String key) {
        CasValue<T> value = (CasValue) cacheClientMaster.gets(key);

        if (value == null && cacheClientBackup != null) {
            value = getAndSetCasFromBackupCache(key);
        }
        if (value == null && cacheClientSlave != null) {
            value = (CasValue) cacheClientSlave.gets(key);
        }
        if (value == null && cacheClientTemp != null) {
            value = (CasValue) cacheClientTemp.gets(key);
        }

        return value;
    }

    /**
     * 批量获取,为了提高效率，getMulti如果获取的是cas值，此处不保证cas unique key的正确性，也就是说：调用者不能用getMulti获得value进行set
     * （目前没有类似操作）
     *
     * @see 当从老的cache获取的casValue数据会去掉cas 标记，避免迁移时cas失败
     */
    @SuppressWarnings("unused")
    @Override
    public Map<String, T> getMulti(String[] keys) {
        Map<String, T> values = new HashMap<String, T>();
        MemcacheClient client = null;
        List<String> leftkeys = null;

        if (cacheClientL1 != null && cacheClientL1.size() > 0) {
            client = chooseOneL1Client();
            if (client != null) {
                values = getMulti(keys, client);
            }
        }

        /* 本机房最新mc */
        if (keys.length > values.size()) {
            leftkeys = getMulti(values, keys, cacheClientMaster);
        }

        /* 本机房原有mc，只获取 */
        if (keys.length > values.size() && cacheClientBackup != null) {
            getAndSetFromBackupCache(keys, values);
        }

        /* 后备机房原有mc，对cas key同步更新 */
        if (keys.length > values.size() && cacheClientTemp != null) {
            getMulti(values, keys, cacheClientTemp);
        }

        /* 后备机房最新mc，对cas key同步更新 */
        // master 如果没有从 slave 读,不仅仅局限于content fishermen 2011.4.15
        if (keys.length > values.size() && cacheClientSlave != null) {
            if (keys.length > 0) {
                int len1 = values.size();
                int expect = keys.length - len1;
                getMulti(values, keys, cacheClientSlave);
                int len2 = values.size();
                if (len2 - len1 > 20)
                    ApiLogger.debug("GetMulti from slave good, add " + (len2 - len1) + "/" + expect + " keys[0] " + keys[0]);
                else if (expect - (len2 - len1) > 20)
                    ApiLogger.debug("GetMulti from slave bad, add " + (len2 - len1) + "/" + expect + " keys[0] " + keys[0]);
            }
        }

        // 回写L1 cache
        if (client != null && leftkeys != null && leftkeys.size() > 0) {
            for (String key : leftkeys) {
                T value = values.get(key);
                if (value == null) {
                    continue;
                }

                set(key, value, expireTime, client, false);
            }
        }

        return values;
    }

    @Override
    public boolean set(String key, T value) {
        return set(key, value, expireTime);
    }

    /**
     * set key and value to mc, if set cacheClient success, we also set cacheClientStandby
     *
     * @see we not set cas for cacheClientStandby, for the value is read from cacheClient
     */
    @Override
    public boolean set(String key, T value, Date expdate) {
        boolean rs = set(key, value, expdate, cacheClientMaster, true);
        if (cacheClientSlave != null) {
            set(key, value, expdate, cacheClientSlave, false);
        }
        // 确保新老cache都更新，避免中间状态
        if (cacheClientBackup != null) {
            set(key, value, expdate, cacheClientBackup, false);
        }
        if (cacheClientTemp != null && updateTempCache) {
            set(key, value, expdate, cacheClientTemp, false);
        }

        // not set in web. in web it will be set to chosen one next time read through
        if (isInProcessor) {
            set(key, value, expdate, cacheClientL1, false);
        }

        // set in web and processor
        if (isUpdateSlaveL1() && isInProcessor) {
            set(key, value, expdate, cacheClientSlaveL1, false);
        }

        return rs;
    }

    /**
     * vika memcache client use set commond not cas
     */
    @Deprecated
    @Override
    public boolean setCas(String key, CasValue cas) {
        return setCas(key, cas, expireTime);
    }

    /**
     * vika memcache client use set commond not cas cas set key and value to mc, if set cacheClient
     * success, we also set cacheClientStandby
     *
     * @see we not set cas for cacheClientStandby, for the value is read from cacheClient
     */
    @Deprecated
    @Override
    public boolean setCas(String key, CasValue cas, Date expdate) {
        boolean rs = cacheClientMaster.setCas(key, cas, expdate);

        // 除master外，其他应该是cacheClientSlave.set()，不能用setCas，因为1）master的cas unique key,不一定等于slave的cas
        // unique key
        if (cacheClientSlave != null && cas != null) {
            cacheClientSlave.set(key, cas.getValue(), expdate);
        }
        // 确保新老cache都更新，避免中间状态
        if (cacheClientBackup != null) {
            cacheClientBackup.set(key, cas.getValue(), expdate);
        }
        if (cacheClientTemp != null && updateTempCache) {
            cacheClientTemp.set(key, cas.getValue(), expdate);
        }

        // not set in web. in web it will be set to chosen one next time read through
        if (isInProcessor) {
            set(key, cas, expdate, cacheClientL1);
        }

        // set in web and processor
        if (isUpdateSlaveL1() && isInProcessor) {
            set(key, cas, expdate, cacheClientSlaveL1);
        }

        return rs;
    }

    /**
     * incr key and value to mc, if set cacheClient success, we also set cacheClientStandby
     *
     * @see we not set cas for cacheClientStandby, for the value is read from cacheClient
     */
    @SuppressWarnings("unchecked")
    @Override
    public T incr(String key) {
        Long value = cacheClientMaster.incr(key);

        if (cacheClientSlave != null) {
            cacheClientSlave.incr(key);
        }
        // 确保新老cache都更新，避免中间状态
        if (cacheClientBackup != null) {
            cacheClientBackup.incr(key);
        }
        if (cacheClientTemp != null && updateTempCache) {
            cacheClientTemp.incr(key);
        }

        // not set in web. in web it will be set to chosen one next time read through
        if (isInProcessor) {
            incr(key, cacheClientL1);
        }

        // set in web and processor
        if (isUpdateSlaveL1() && isInProcessor) {
            incr(key, cacheClientSlaveL1);
        }

        // incr 只能返回 long ，所以进行强制转换
        return (T) value;
    }

    /**
     * incr key and value to mc, if set cacheClient success, we also set cacheClientStandby
     *
     * @see we not set cas for cacheClientStandby, for the value is read from cacheClient
     */
    @Override
    public T decr(String key) {
        Long value = cacheClientMaster.decr(key);

        if (cacheClientSlave != null) {
            cacheClientSlave.decr(key);
        }
        // 确保新老cache都更新，避免中间状态
        if (cacheClientBackup != null) {
            cacheClientBackup.decr(key);
        }
        if (cacheClientTemp != null && updateTempCache) {
            cacheClientTemp.decr(key);
        }

        // not set in web. in web it will be set to chosen one next time read through
        if (isInProcessor) {
            decr(key, cacheClientL1);
        }

        // set in web and processor
        if (isUpdateSlaveL1() && isInProcessor) {
            decr(key, cacheClientSlaveL1);
        }

        return (T) value;
    }

    // ----------- fix setCas bug -----------------
    @Override
    public boolean cas(String key, CasValue cas) {
        return cas(key, cas, expireTime);
    }

    /**
     * cas set key and value to mc, if cas cacheClient success, we also set cacheClientStandby
     *
     * @see we not set cas for cacheClientStandby, for the value is read from cacheClient
     */
    @Override
    public boolean cas(String key, CasValue cas, Date expdate) {
        boolean rs = cacheClientMaster.cas(key, cas, expdate);
        if (rs == false) {
            ApiLogger.warn("WARN: cas false for key" + key);
            return rs;
        }

        // 除master外，其他应该是cacheClientSlave.set()，不能用setCas，因为1）master的cas unique key,不一定等于slave的cas
        // unique key
        if (cacheClientSlave != null) {
            cacheClientSlave.set(key, cas.getValue(), expdate);
        }
        // 确保新老cache都更新，避免中间状态
        if (cacheClientBackup != null) {
            cacheClientBackup.set(key, cas.getValue(), expdate);
        }
        if (cacheClientTemp != null && updateTempCache) {
            cacheClientTemp.set(key, cas.getValue(), expdate);
        }

        // not set in web. in web it will be set to chosen one next time read through
        if (isInProcessor) {
            set(key, cas, expdate, cacheClientL1);
        }

        // set in web and processor
        if (isUpdateSlaveL1() && isInProcessor) {
            set(key, cas, expdate, cacheClientSlaveL1);
        }

        return rs;
    }

    @Override
    public boolean delete(String key) {
        boolean rs = cacheClientMaster.delete(key);

        if (cacheClientBackup != null) {
            cacheClientBackup.delete(key);
        }
        if (cacheClientSlave != null) {
            cacheClientSlave.delete(key);
        }
        if (cacheClientTemp != null && updateTempCache) {
            cacheClientTemp.delete(key);
        }

        delete(key, cacheClientL1);

        if (isUpdateSlaveL1()) {
            delete(key, cacheClientSlaveL1);
        }

        return rs;
    }

    public MemcacheClient getCacheClient() {
        return cacheClientMaster;
    }

    public MemcacheClient getCacheClientMaster() {
        return cacheClientMaster;
    }

    public void setCacheClientMaster(MemcacheClient cacheClientMaster) {
        this.cacheClientMaster = cacheClientMaster;
    }

    /**
     * expire: unit is minute
     *
     * @param expire
     */
    public void setExpire(long expire) {
        if (expire > 0) {
            this.expireTime = new Date(1000l * 60 * expire);
        }
    }

    public MemcacheClient getCacheClientBackup() {
        return cacheClientBackup;
    }

    public void setCacheClientBackup(MemcacheClient cacheClientBackup) {
        this.cacheClientBackup = cacheClientBackup;
    }

    public MemcacheClient getCacheClientSlave() {
        return cacheClientSlave;
    }

    public void setCacheClientSlave(MemcacheClient cacheClientSlave) {
        this.cacheClientSlave = cacheClientSlave;
    }

    public void setCacheClientTemp(MemcacheClient cacheClientTemp) {
        this.cacheClientTemp = cacheClientTemp;
    }

    public void setUpdateTempCache(boolean updateTempCache) {
        this.updateTempCache = updateTempCache;
    }

    /**
     * @return the cacheClientL1
     */
    public List<MemcacheClient> getCacheClientL1() {
        return cacheClientL1;
    }

    /**
     * @param cacheClientL1 the cacheClientL1 to set
     */
    public void setCacheClientL1(List<MemcacheClient> cacheClientL1) {
        this.cacheClientL1 = cacheClientL1;
    }

    public List<MemcacheClient> getCacheClientSlaveL1() {
        return cacheClientSlaveL1;
    }

    public void setCacheClientSlaveL1(List<MemcacheClient> cacheClientSlaveL1) {
        this.cacheClientSlaveL1 = cacheClientSlaveL1;
    }

    @SuppressWarnings("unchecked")
    private T getAndSetFromBackupCache(String key) {
        T value = null;
        if (!StorageAble.isCasKey(key)) {
            // 对于非cas值，直接迁移
            value = get(key, cacheClientBackup);
            if (value != null) {
                if (deleteFrmBackupCache) {
                    cacheClientBackup.delete(key);
                }
                set(key, value);
                if (ApiLogger.isDebugEnabled()) {
                    ApiLogger.debug(new StringBuilder(64).append("==== [get frm backup]Recache for key=").append(key));
                }
            }
        } else {
            // 对于cas值，cache迁移数据时需要加分布锁
            try {
                lock(key, true);
                value = get(key, cacheClientMaster);
                if (value != null) {
                    return value;
                }
                value = get(key, cacheClientBackup);
                if (value == null) {
                    return null;
                }

                // not cas set to cacheClient, bug re-get the cas value, for cas values is needed
                ((CasValue) value).resetCas();
                set(key, value);
                if (ApiLogger.isDebugEnabled()) {
                    ApiLogger.debug(new StringBuilder(64).append("==== [get frm backup]Recache for key=").append(key));
                }
            } catch (Exception e) {
                ApiLogger.warn(new StringBuilder(64).append("Error: when transfer data for key").append(key), e);
                return null;
            } finally {
                releaseLock(key);
            }
            // re-get,to get the correct cas value
            value = get(key, cacheClientMaster);
            // delete dirty data
            if (deleteFrmBackupCache) {
                cacheClientBackup.delete(key);
            }

        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private CasValue<T> getAndSetCasFromBackupCache(String key) {
        CasValue<T> value = null;
        // 对于cas值，cache迁移数据时需要加分布锁
        try {
            lock(key, true);
            value = (CasValue) cacheClientMaster.gets(key);
            if (value != null) {
                return value;
            }
            value = (CasValue) cacheClientBackup.gets(key);
            if (value == null) {
                return null;
            }

            // not cas set to cacheClient, bug re-get the cas value, for cas values is needed
            value.resetCas();
            setCas(key, value);
            if (ApiLogger.isDebugEnabled()) {
                ApiLogger.debug(new StringBuilder(64).append("==== [get frm backup]Recache for key=").append(key));
            }
        } catch (Exception e) {
            ApiLogger.warn(new StringBuilder(64).append("Error: when transfer data for key").append(key), e);
            return null;
        } finally {
            releaseLock(key);
        }
        // re-get,to get the correct cas value
        value = (CasValue) cacheClientMaster.gets(key);
        // delete dirty data
        if (deleteFrmBackupCache) {
            cacheClientBackup.delete(key);
        }

        return value;
    }

    /**
     * via mc add operation, we create a distributed lock
     *
     * @param key
     * @param retry: if false, we return true when add false first time
     * @return
     */
    private boolean lock(String key, boolean retry) {
        String lockKey = getLockKey(key);
        int i = 100;
        int sleepTime = LOCK_EXPIRE_MILLIS / i;

        while (i-- > 0) {
            if (cacheClientMaster.add(lockKey, 1, LOCK_EXPIRE_TIME) || !retry) {
                return true;
            }
            ApiUtil.safeSleep(sleepTime);
            if (i <= 80 && i % 10 == 0) {
                ApiLogger.warn(new StringBuilder(64).append("warn - careful, lock too slow. key=").append(key).append(", t=")
                        .append((100 - i) * sleepTime));
            }
        }
        ApiLogger.warn(new StringBuilder(64).append("Error - lock false, check cache now! key=").append(key).append(", t=2000ms"));
        return false;
    }

    private boolean releaseLock(String key) {
        String lockKey = getLockKey(key);
        boolean released = cacheClientMaster.delete(lockKey);
        if (!released) {
            ApiLogger.warn(new StringBuilder(64).append("Fatal: release lock false, for key=").append(key));
        }
        return released;
    }

    private String getLockKey(String rawKey) {
        return rawKey + "_lock";
    }


    /**
     * get data from memcache
     *
     * @param key
     * @param mc
     * @param autoCheckCas : if ture, we will check if the key is casKey, if it is ,we will use
     *        "gets" to get cas value, or just use "get"
     * @return
     */
    @SuppressWarnings("unchecked")
    private T get(String key, MemcacheClient mc) {
        if (StorageAble.isCasKey(key)) {
            return (T) (parseCasValue((CasValue) mc.gets(key), key));
        } else {
            return (T) mc.get(key);
        }
    }

    /**
     * 将数据从老的cache迁移到新的cache时,迁移的数据如果是casValue，会去掉cas标记 去掉cas标记原因：1
     * ）目前及可预计的将来不会存在对getMulti的value再set cas的情况，2 ）获取正确的cas unique key 需要重新gets，性能损耗
     *
     * @param allKeys
     * @param cachedValues: 主cache中缓存的data
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<String> getAndSetFromBackupCache(String[] allKeys, Map<String, T> cachedValues) {
        // get the keys which are not cached in the main cache
        List<String> missingKeys = new ArrayList<String>(allKeys.length - cachedValues.size());
        for (String key : allKeys) {
            if (!cachedValues.containsKey(key)) {
                missingKeys.add(key);
            }
        }

        // transfer datas from olden cache to using cache, and delete old datas
        if (missingKeys.size() > 0) {
            String[] missingKeysArr = new String[missingKeys.size()];
            missingKeysArr = missingKeys.toArray(missingKeysArr);
            Map<String, T> valuesInOlderCache = getMulti(missingKeysArr, cacheClientBackup);
            cachedValues.putAll(valuesInOlderCache);
        }

        // FIXME set back to master ?

        return missingKeys;

    }

    @SuppressWarnings("unchecked")
    private Map<String, T> getMulti(String[] keys, MemcacheClient mc) {
        Map<String, T> values = null;
        if (StorageAble.isCasKey(keys[0])) {
            Map<String, Object> casValues = mc.getsMulti(keys);
            values = new HashMap<String, T>();
            T casValue = null;
            for (Map.Entry<String, Object> entry : casValues.entrySet()) {
                casValue = (T) parseCasValue((CasValue) entry.getValue(), entry.getKey());
                if (casValue != null) {
                    values.put(entry.getKey(), casValue);
                }
            }
        } else {
            values = (Map<String, T>) mc.getMulti(keys);
        }

        for (String key : keys) {
            if (values.get(key) == null) {
                values.remove(key);
            }
        }
        return values;
    }

    /**
     * 获取 results 没有的 key
     *
     * @param results
     * @param keys
     * @param mc
     */
    @SuppressWarnings("unchecked")
    private List<String> getMulti(Map<String, T> results, String[] allKeys, MemcacheClient mc) {
        Map<String, T> values = null;
        if (allKeys.length - results.size() <= 0) return null;
        // String[] keys2 = new String[allKeys.length - results.size()];
        List<String> leftKeysList = new ArrayList<String>();
        for (String key : allKeys) {
            if (!results.containsKey(key)) {
                leftKeysList.add(key);
            }
        }
        if (leftKeysList.size() == 0) {
            return null;
        }
        String[] keys2 = new String[leftKeysList.size()];
        leftKeysList.toArray(keys2);
        if (StorageAble.isCasKey(allKeys[0])) {
            Map<String, Object> casValues = mc.getsMulti(keys2);
            values = new HashMap<String, T>();
            T casValue = null;
            for (Map.Entry<String, Object> entry : casValues.entrySet()) {
                casValue = (T) parseCasValue((CasValue) entry.getValue(), entry.getKey());
                if (casValue != null) {
                    values.put(entry.getKey(), casValue);
                }
            }
        } else {
            values = (Map<String, T>) mc.getMulti(keys2);
        }

        for (String key : keys2) {
            T obj = values.get(key);
            if (obj != null) {
                results.put(key, obj);
            }
        }

        return leftKeysList;
    }

    /**
     * 目前只有Vector使用cas，故不用判断type
     *
     * @param casValue
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object parseCasValue(CasValue casValue, String key) {
        if (casValue == null || casValue.getValue() == null) {
            return null;
        }
        Object value = casValue.getValue();
        // if(value instanceof VectorItemValue){
        // return new VectorItem(casValue);
        // }else if(value instanceof TailVectorItemValue){
        // return new TailVectorItem(casValue);
        // }else if(value instanceof DoubleLongitudeVectorItemValue){
        // return new DoubleLongitudeVectorItem(casValue);
        // }else if(value instanceof TailDoubleLongitudeVectorItemValue){
        // return new DoubleLongitudeVectorItem(casValue);
        // }
        // else{
        // ApiLogger.warn(new StringBuilder(32).append("[Careful] Unknown cas vauel,
        // key").append(key));
        // return null;
        // }
        return null;
    }

    /**
     * set key and value to mc, if autoSetCas is true, we set cas, or we just set the cas.value
     *
     * @param key
     * @param value
     * @param expdate
     * @param mc
     * @param autoSetCas if true, we set cas, or we just set the cas.value
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean set(String key, T value, Date expdate, MemcacheClient mc, boolean autoSetCas) {
        if (isReadOnly()) {
            return true;
        }
        if (value instanceof CasValue) {
            if (autoSetCas) {
                return mc.setCas(key, (CasValue) value, expdate);
            } else {
                return mc.set(key, ((CasValue) value).getValue(), expdate);
            }
        } else {
            return mc.set(key, value, expdate);
        }
    }

    /*
     * set for mcList
     */
    private boolean set(String key, T value, Date expdate, List<MemcacheClient> mcList, boolean autoSetCas) {
        if (isReadOnly()) {
            return true;
        }
        boolean result = true;
        if (mcList != null && mcList.size() > 0) {
            for (MemcacheClient client : mcList) {
                boolean success = set(key, value, expdate, client, autoSetCas);
                if (success == false) {
                    result = false;
                }
            }
        }
        if (result == false) {
            ApiLogger.warn("mcList set key:" + key + ", result:" + result);
        }
        return result;
    }

    /*
     * set for mcList
     */
    private boolean set(String key, CasValue cas, Date expdate, List<MemcacheClient> mcList) {
        if (isReadOnly()) {
            return true;
        }
        boolean result = true;
        if (mcList != null && mcList.size() > 0) {
            for (MemcacheClient client : mcList) {
                boolean success = client.set(key, cas.getValue(), expdate);
                if (success == false) {
                    result = false;
                }
            }
        }
        if (result == false) {
            ApiLogger.warn("mcList set cas key:" + key + ", result:" + result);
        }
        return result;
    }

    private void incr(String key, List<MemcacheClient> mcList) {
        if (isReadOnly()) {
            return;
        }
        if (mcList != null && mcList.size() > 0) {
            for (MemcacheClient client : mcList) {
                client.incr(key);
            }
        }
    }

    private void decr(String key, List<MemcacheClient> mcList) {
        if (isReadOnly()) {
            return;
        }
        if (mcList != null && mcList.size() > 0) {
            for (MemcacheClient client : mcList) {
                client.decr(key);
            }
        }
    }

    /*
     * delete for mcList
     */
    private boolean delete(String key, List<MemcacheClient> mcList) {
        if (isReadOnly()) {
            return true;
        }
        boolean result = true;
        if (mcList != null && mcList.size() > 0) {
            for (MemcacheClient client : mcList) {
                boolean success = client.delete(key);
                if (success == false) {
                    result = false;
                }
            }
        }
        return result;
    }

    public boolean isUpdateSlaveL1() {
        return updateSlaveL1.get();
    }

    public void setUpdateSlaveL1(boolean updateSlaveL1) {
        this.updateSlaveL1.set(updateSlaveL1);
    }

    public boolean isReadOnly() {
        return readOnly.get();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly.set(readOnly);
    }

    @Override
    public boolean add(String key, T value) {
        throw new UnsupportedOperationException("add(String, T)");
    }

    @Override
    public boolean add(String key, T value, Date expdate) {
        throw new UnsupportedOperationException("add(String, T, Date)");
    }

}
