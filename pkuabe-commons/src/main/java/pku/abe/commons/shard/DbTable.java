package pku.abe.commons.shard;


/**
 * db and table
 */
public class DbTable {
    private int db;
    private String table;

    public DbTable(int db, String table) {
        this.db = db;
        this.table = table;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DbTable) {
            DbTable dbTable = (DbTable) obj;
            return dbTable.db == this.db && dbTable.table == this.table;
        }
        return false;
    }

    @Override
    public String toString() {
        return "db/table=" + db + "/" + table;
    }

    public int getDb() {
        return db;
    }

    public String getTable() {
        return table;
    }

    public void setDb(int db) {
        this.db = db;
    }

    public void setTable(String table) {
        this.table = table;
    }

}
