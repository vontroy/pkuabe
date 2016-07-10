package pku.abe.commons.counter.dao;

import java.util.List;
import java.util.Map;

public interface CountDao {
    /**
     * result format is "9"
     *
     * @param id
     * @param field
     * @return
     */
    int get(long id, String field);

    /**
     * performance not good: for each get id
     *
     * @param ids
     * @param field
     * @return
     */
    Map<Long, Integer> gets(long[] ids, String field);

    /**
     * result format is "uid1.cntbf, uid1.cntuf, uid2.cntbf, uid2.cntuf"
     * 
     * @param
     * @return
     */
    Map<Long, List<Integer>> gets(long[] ids, String[] fields);

    /**
     * result format is "repost:9,comment:8,like:1"
     *
     * @param id
     * @return
     */
    Map<String, Integer> getAll(long id);

    /**
     * performance is ok: sharding by dbIndex and concurrent multi_get
     *
     * @param ids
     * @return
     */
    Map<Long, Map<String, Integer>> getsAll(long[] ids);

    /**
     * incr id field's value: incr if value is positive number, decr if value is negative number
     *
     * @param id
     * @param field
     * @param value
     * @return
     */
    int incr(long id, String field, int value);

    /**
     * set id field's value
     *
     * @param id
     * @param field
     * @param value
     * @return
     */
    boolean set(long id, String field, int value);

    /**
     * delete id key
     *
     * @param id
     * @return
     */
    boolean delete(long id);

    boolean delete(long id, String field);
}
