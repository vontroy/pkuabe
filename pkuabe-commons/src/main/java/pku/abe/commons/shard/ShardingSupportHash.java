/**
 *
 */
package pku.abe.commons.shard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.util.CommonUtil;
import pku.abe.commons.util.HashUtil;

public class ShardingSupportHash<T> implements ShardingSupport<T> {
    private Map<Integer, T> clients;
    private String hashAlg = HashUtil.HashAlg.CRC32;
    private int hashGene = DEFAULT_HASH_GENE;
    private int tablePerDb = DEFAULT_TBL_PER_DB;

    private String noneHash = HashUtil.NoneHash.NEW;

    @Override
    public T getClient(long id) {
        Integer db = getDbTable(id).getDb();
        return getClientByDb(db);
    }

    @Override
    public T getClient(String id) {
        Integer db = getDbTable(id).getDb();
        return getClientByDb(db);
    }

    @Override
    public T getClientByDb(Integer db) {
        return clients.get(db);
    }

    @Override
    public DbTable getDbTable(long id) {
        return getDbTable(String.valueOf(id));
    }

    public DbTable getDbTable(String id) {
        int raw = HashUtil.getHash(id, hashGene, hashAlg, noneHash);
        int db = raw / tablePerDb;
        String table = String.valueOf(raw % tablePerDb);

        DbTable dbTable = new DbTable(db, table);
        if (CommonUtil.isDebugEnabled()) {
            ApiLogger.debug("DaoShardingHash getDbTable dbTable:" + dbTable.toString());
        }
        return dbTable;
    }

    @Override
    @Deprecated
    public DbTable getDbTable(long id, Date date) {
        throw new RuntimeException("DaoShardingHash getDbTable(long, Date) method not supported");
    }

    @Override
    public Map<Integer, List<Long>> getDbSharding(long[] ids) {
        Map<Integer, List<Long>> dbIdsMap = new HashMap<Integer, List<Long>>();
        for (long id : ids) {
            DbTable dbTable = getDbTable(id);
            List<Long> list = dbIdsMap.get(dbTable.getDb());

            if (list == null) {
                list = new ArrayList<Long>();
                dbIdsMap.put(dbTable.getDb(), list);
            }
            list.add(id);
        }
        if (CommonUtil.isDebugEnabled()) {
            ApiLogger.debug("DaoShardingHash getDbSharding, ids:" + Arrays.toString(ids) + ", dbIdsMap:" + dbIdsMap);
        }
        return dbIdsMap;
    }

    @Override
    public Map<Integer, List<String>> getDbSharding(String[] ids) {
        Map<Integer, List<String>> dbIdsMap = new HashMap<Integer, List<String>>();
        for (String id : ids) {
            DbTable dbTable = getDbTable(id);
            List<String> list = dbIdsMap.get(dbTable.getDb());

            if (list == null) {
                list = new ArrayList<String>();
                dbIdsMap.put(dbTable.getDb(), list);
            }
            list.add(id);
        }
        if (CommonUtil.isDebugEnabled()) {
            ApiLogger.debug("DaoShardingHash getDbSharding, ids:" + Arrays.toString(ids) + ", dbIdsMap:" + dbIdsMap);
        }
        return dbIdsMap;
    }

    public Map<Integer, T> getClients() {
        return clients;
    }

    public void setClients(Map<String, T> clients) {
        this.clients = ShardingUtil.<T>parseClients(clients);
    }

    public String getHashAlg() {
        return hashAlg;
    }

    public void setHashAlg(String hashAlg) {
        this.hashAlg = hashAlg;
    }

    public int getHashGene() {
        return hashGene;
    }

    public int getTablePerDb() {
        return tablePerDb;
    }

    public void setHashGene(int hashGene) {
        this.hashGene = hashGene;
    }

    public void setTablePerDb(int tablePerDb) {
        this.tablePerDb = tablePerDb;
    }

    public String getNoneHash() {
        return noneHash;
    }

    public void setNoneHash(String noneHash) {
        this.noneHash = noneHash;
    }

}
