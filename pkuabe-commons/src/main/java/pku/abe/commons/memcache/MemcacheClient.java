package pku.abe.commons.memcache;

import java.util.Date;
import java.util.Map;

public interface MemcacheClient {
    public void setServerPort(String serverPort);

    public void setServerPortList(String[] serverPort);

    public boolean set(String key, Object value);

    public boolean set(String key, Object value, Date expdate);

    /**
     * use set commond not cas
     */
    @Deprecated
    public boolean setCas(String key, CasValue<Object> value);

    @Deprecated
    public boolean setCas(String key, CasValue<Object> value, Date expdate);

    // ----------- fix setCas bug -----------------
    public boolean cas(String key, CasValue<Object> value);

    public boolean cas(String key, CasValue<Object> value, Date expdate);

    public boolean append(String key, Object value);

    public Object get(String key);

    public boolean delete(String key);

    public Map<String, Object> getMulti(String[] keys);

    /**
     * Adds data to the server; only the key and the value are specified.
     *
     * @param key key to store data under
     * @param value value to store
     * @return true, if the data was successfully stored
     */
    public boolean add(String key, Object value);

    public boolean add(String key, Object value, Date expdate);

    /**
     * 将key对象的数字进行原子的加一操作
     *
     * @param key 带操作的key
     * @return 操作结果
     */
    public long incr(String key);

    /**
     * 将key对象的数字进行原子的加法操作
     *
     * @param key 带操作的key
     * @param inc 被减数
     * @return 操作结果
     */
    public long incr(String key, long inc);

    /**
     * 将key对象的数字进行原子的减一操作
     *
     * @param key 带操作的key
     * @return 操作结果
     */
    public long decr(String key);

    /**
     * 将key对象的数字进行原子的减法操作
     *
     * @param key 带操作的key
     * @param inc 被减数
     * @return 操作结果
     */
    public long decr(String key, int inc);

    public CasValue<Object> gets(String key);

    public Map<String, Object> getsMulti(String[] keys);
}
