package pku.abe.commons.counter.dao.impl;

import pku.abe.commons.counter.CountConst;
import pku.abe.commons.counter.dao.CountDao;
import pku.abe.commons.counter.task.CountTaskExecutor;
import pku.abe.commons.counter.util.CountCodecUtil;
import pku.abe.commons.counter.util.CountUtil;
import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.redis.JedisClient;
import pku.abe.commons.shard.ShardingSupport;
import pku.abe.commons.thread.ExecutorServiceUtil;
import pku.abe.commons.util.Util;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CountDaoImpl implements CountDao {

    private ShardingSupport<JedisClient> shardingSupport;

    /**
     * get key
     *
     * @param id
     * @return
     */
    public static String getKey(long id) {
        return String.valueOf(id);
    }

    /**
     * get key
     *
     * @param id
     * @return
     */
    public static String getKey(long id, String field) {
        return String.valueOf(id) + "." + field;
    }

    @Override
    public int get(final long id, final String field) {
        String result = null;
        try {
            Callable<String> task = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    JedisClient client = shardingSupport.getClient(id);
                    return client.get(getKey(id, field));
                }
            };
            result = ExecutorServiceUtil.invoke(CountTaskExecutor.db, task, CountConst.Timeout.count, TimeUnit.MILLISECONDS, true);
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl get, id:" + id + ", field:" + field + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn get, id:" + id + ", field:" + field + ", result:" + result);
            // StatLog.inc(CountConst.TaskName.count);
        }

        int idCount = Util.convertInt(result);
        return idCount;
    }

    @Override
    public Map<Long, Integer> gets(long[] ids, final String field) {
        final Map<Long, Integer> allIdsCount = new ConcurrentHashMap<Long, Integer>();

        try {
            Map<Integer, List<Long>> dbIdsMap = shardingSupport.getDbSharding(ids);

            List<Callable<Map<Long, Integer>>> tasks = new ArrayList<Callable<Map<Long, Integer>>>();
            for (Map.Entry<Integer, List<Long>> entry : dbIdsMap.entrySet()) {
                final Integer db = entry.getKey();
                final List<Long> idList = entry.getValue();

                Callable<Map<Long, Integer>> task = new Callable<Map<Long, Integer>>() {
                    @Override
                    public Map<Long, Integer> call() throws Exception {
                        String[] keys = new String[idList.size()];
                        for (int i = 0; i < idList.size(); i++) {
                            keys[i] = getKey(idList.get(i), field);
                        }

                        JedisClient client = shardingSupport.getClientByDb(db);
                        List<String> values = client.mget(keys);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl gets, db:" + db + ", keys:" + Arrays.toString(keys) + ", values:" + values);
                        }

                        Map<Long, Integer> idListCount = new HashMap<Long, Integer>();
                        for (int i = 0; i < keys.length; i++) {
                            Long key = idList.get(i); // or keys[i];
                            int idCount = Util.convertInt(values.get(i));
                            if (idCount != 0) {
                                idListCount.put(key, idCount);
                            }
                        }

                        allIdsCount.putAll(idListCount);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl gets, db:" + db + ", idList:" + idList + ", idListCount:" + idListCount);
                        }
                        return idListCount;
                    }
                };

                tasks.add(task);
            }

            ExecutorServiceUtil.invokes(CountTaskExecutor.db, tasks, CountConst.Timeout.counts, TimeUnit.MILLISECONDS, true);
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn gets, ids:" + ids + ", allIdsCount:" + allIdsCount, e);
        }
        return allIdsCount;
    }

    @Override
    public Map<Long, List<Integer>> gets(long[] ids, final String[] fields) {
        final Map<Long, List<Integer>> allIdsCounts = new ConcurrentHashMap<Long, List<Integer>>();

        try {
            Map<Integer, List<Long>> dbIdsMap = shardingSupport.getDbSharding(ids);

            List<Callable<Map<Long, List<Integer>>>> tasks = new ArrayList<Callable<Map<Long, List<Integer>>>>();
            for (Map.Entry<Integer, List<Long>> entry : dbIdsMap.entrySet()) {
                final Integer db = entry.getKey();
                final List<Long> idList = entry.getValue();

                Callable<Map<Long, List<Integer>>> task = new Callable<Map<Long, List<Integer>>>() {
                    @Override
                    public Map<Long, List<Integer>> call() throws Exception {
                        List<String> keyList = new ArrayList<String>();
                        for (long id : idList) {
                            for (String field : fields) {
                                keyList.add(getKey(id, field));
                            }
                        }
                        JedisClient client = shardingSupport.getClientByDb(db);
                        String[] keys = keyList.toArray(new String[0]);
                        List<String> values = client.mget(keys);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl getsAll, db:" + db + ", keys:" + Arrays.toString(keys) + ", values:" + values);
                        }

                        Map<Long, List<Integer>> idListCounts = new HashMap<Long, List<Integer>>();
                        if (values != null) {
                            for (int i = 0; i < idList.size(); i++) {
                                List<Integer> idCounts = new ArrayList<Integer>();
                                Long id = idList.get(i);
                                int length = fields.length;
                                int num = i * length;
                                for (int j = 0; j < length; j++) {
                                    int value = Util.convertInt(values.get(num + j));
                                    idCounts.add(value);
                                }
                                if (!idCounts.isEmpty()) {
                                    idListCounts.put(id, idCounts);
                                }
                            }
                        }

                        allIdsCounts.putAll(idListCounts);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl getsAll, db:" + db + ", idList:" + idList + ", idListCounts:" + idListCounts);
                        }
                        return idListCounts;
                    }
                };

                tasks.add(task);
            }

            ExecutorServiceUtil.invokes(CountTaskExecutor.db, tasks, CountConst.Timeout.counts, TimeUnit.MILLISECONDS, true);
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn getsAll, ids:" + ids + ", allIdsCounts:" + allIdsCounts, e);
        }
        return allIdsCounts;
    }

    @Override
    public Map<String, Integer> getAll(final long id) {
        String result = null;
        try {
            Callable<String> task = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    JedisClient client = shardingSupport.getClient(id);
                    return client.get(getKey(id));
                }
            };
            result = ExecutorServiceUtil.invoke(CountTaskExecutor.db, task, CountConst.Timeout.count, TimeUnit.MILLISECONDS, true);
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl getAll, id:" + id + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn get, id:" + id + ", result:" + result);
            // StatLog.inc(CountConst.TaskName.count);
        }
        Map<String, Integer> idCounts = CountCodecUtil.toMap(result);
        return idCounts;
    }

    @Override
    public Map<Long, Map<String, Integer>> getsAll(long[] ids) {
        final Map<Long, Map<String, Integer>> allIdsCounts = new ConcurrentHashMap<Long, Map<String, Integer>>();

        try {
            Map<Integer, List<Long>> dbIdsMap = shardingSupport.getDbSharding(ids);

            List<Callable<Map<Long, Map<String, Integer>>>> tasks = new ArrayList<Callable<Map<Long, Map<String, Integer>>>>();
            for (Map.Entry<Integer, List<Long>> entry : dbIdsMap.entrySet()) {
                final Integer db = entry.getKey();
                final List<Long> idList = entry.getValue();

                Callable<Map<Long, Map<String, Integer>>> task = new Callable<Map<Long, Map<String, Integer>>>() {
                    @Override
                    public Map<Long, Map<String, Integer>> call() throws Exception {
                        String[] keys = new String[idList.size()];
                        for (int i = 0; i < idList.size(); i++) {
                            keys[i] = getKey(idList.get(i));
                        }

                        JedisClient client = shardingSupport.getClientByDb(db);
                        List<String> values = client.mget(keys);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl getsAll, db:" + db + ", keys:" + Arrays.toString(keys) + ", values:" + values);
                        }

                        Map<Long, Map<String, Integer>> idListCounts = new HashMap<Long, Map<String, Integer>>();
                        for (int i = 0; i < keys.length; i++) {
                            Long key = idList.get(i); // or keys[i];
                            Map<String, Integer> idCounts = CountCodecUtil.toMap(values.get(i));
                            if (idCounts != null) {
                                idListCounts.put(key, idCounts);
                            }
                        }

                        allIdsCounts.putAll(idListCounts);
                        if (CountUtil.isDebugEnabled()) {
                            ApiLogger.debug("CountDaoImpl getsAll, db:" + db + ", idList:" + idList + ", idListCounts:" + idListCounts);
                        }
                        return idListCounts;
                    }
                };

                tasks.add(task);
            }

            ExecutorServiceUtil.invokes(CountTaskExecutor.db, tasks, CountConst.Timeout.counts, TimeUnit.MILLISECONDS, true);
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn getsAll, ids:" + ids + ", allIdsCounts:" + allIdsCounts, e);
        }
        return allIdsCounts;
    }

    @Override
    public int incr(long id, String field, int value) {
        long result = CountConst.ValueFalse.longFalse;
        try {
            JedisClient client = shardingSupport.getClient(id);
            result = client.incrBy(getKey(id, field), value);
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl incr, id:" + id + ", field:" + field + ", value:" + value + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn incr, id:" + id + ", field:" + field + ", value:" + value + ", result:" + result);
        }
        return (int) result;
    }

    @Override
    public boolean set(long id, String field, int value) {
        boolean result = CountConst.ValueFalse.booleanFalse;
        try {
            JedisClient client = shardingSupport.getClient(id);
            result = (boolean) client.set(getKey(id, field), value);
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl set, id:" + id + ", field:" + field + ", value:" + value + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn set, id:" + id + ", field:" + field + ", value:" + value + ", result:" + result);
        }
        return result;
    }

    @Override
    public boolean delete(long id) {
        long result = CountConst.ValueFalse.longFalse;
        try {
            JedisClient client = shardingSupport.getClient(id);
            result = client.del(getKey(id));
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl delete, id:" + id + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn delete, id:" + id + ", result:" + result);
        }
        return result > 0;
    }

    @Override
    public boolean delete(long id, String field) {
        long result = CountConst.ValueFalse.longFalse;
        try {
            JedisClient client = shardingSupport.getClient(id);
            result = client.del(getKey(id, field));
            if (CountUtil.isDebugEnabled()) {
                ApiLogger.debug("CountDaoImpl delete, id:" + id + ", field:" + field + ", result:" + result);
            }
        } catch (Exception e) {
            ApiLogger.warn("CountDaoImpl warn delete, id:" + id + ", field:" + field + ", result:" + result);
        }
        return result > 0;
    }

    public ShardingSupport<JedisClient> getShardingSupport() {
        return shardingSupport;
    }

    public void setShardingSupport(ShardingSupport<JedisClient> shardingSupport) {
        this.shardingSupport = shardingSupport;
    }

}
