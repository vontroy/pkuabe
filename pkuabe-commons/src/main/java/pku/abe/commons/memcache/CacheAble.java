/**
 *
 */
package pku.abe.commons.memcache;

import java.util.Date;
import java.util.Map;

public interface CacheAble<T> {

    /*
     * offset expire unit is minute : 1440 * 30 == 43200 (2592000s) unix time for expire : will >
     * 1000000000000(2001-09-09)
     */
    public static final int maxLowerExpire = 43200;
    public static final Date maxLowerExpireDate = new Date(1000L * 60 * maxLowerExpire);
    public static final Date maxUpperExpireDate = new Date(1000000000000L);

    /**
     * get
     *
     * @param key
     * @return
     */
    T get(String key);

    /**
     * multi get
     *
     * @param keys
     * @return
     */
    Map<String, T> getMulti(String[] keys);

    /**
     * set with policy (setAll or setAndDeleteL1 or setAndIfExistL1)
     * <p>
     * setAll -- set master/slave and masterL1/slaveL1 setAndDeleteL1 -- set master/slave, and
     * delete masterL1/slaveL1 (keep L1 cache hot) setAndIfExistL1 -- set master/slave, and but if
     * value exist could be set with masterL1/slaveL1 (keep L1 cache hot)
     *
     * @param key
     * @param value
     * @return
     */
    boolean set(String key, T value);

    boolean set(String key, T value, Date expdate);

    /**
     * add master/slave and masterL1/slaveL1
     *
     * @param key
     * @param value
     * @return
     */
    boolean add(String key, T value);

    boolean add(String key, T value, Date expdate);

    /**
     * get casValue from master(getCas not support slave/L1 cache, because master casUnique only can
     * compare with self and can't cas null)
     *
     * @param key
     * @return
     */
    CasValue<T> getCas(String key);

    /**
     * cas master/slave and masterL1/slaveL1
     *
     * @param key
     * @param value
     * @return
     */
    boolean cas(String key, CasValue<T> value);

    boolean cas(String key, CasValue<T> value, Date expdate);

    /**
     * delete master/slave and masterL1/slaveL1
     *
     * @param key
     * @return
     */
    boolean delete(String key);

}
