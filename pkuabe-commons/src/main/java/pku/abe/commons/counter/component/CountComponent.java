package pku.abe.commons.counter.component;

import java.util.List;
import java.util.Map;

public interface CountComponent {
    int get(long id, String field);

    Map<Long, Integer> gets(long[] ids, String field);

    Map<String, Integer> getAll(long id);

    Map<Long, List<Integer>> gets(long[] ids, String[] fields);

    Map<Long, Map<String, Integer>> getsAll(long[] ids);

    int incr(long id, String field, int value);

    boolean set(long id, String field, int value);

    boolean delete(long id);

    boolean delete(long id, String field);

    /*
     * switch
     */
    boolean isDaoEnable();

    void setDaoEnable(boolean daoEnable);
}
