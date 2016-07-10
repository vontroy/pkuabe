package pku.abe.data.dao.strategy;


import java.io.Serializable;
import java.util.Map;

import pku.abe.commons.log.ApiLogger;
import pku.abe.data.dao.util.JdbcTemplate;

public class TableItem implements Serializable {
    private static final long serialVersionUID = 9202187462877426151L;
    private JdbcTemplate[] jdbcTemplates;
    private Map<String, String> sqls;
    private String tableNamePrefix;
    private String dbNamePrefix;
    private int tableCount = 0;
    private int dbCount = 0;
    private String itemName;
    private String tableNamePostFix;
    private Map<String, JdbcTemplate[]> jdbcTemplateCluster;
    private boolean hierarchy = false;

    public TableItem() {}

    public TableItem(String dbNamePrefix, int dbCount, String tableNamePrefix, String tableNamePostFix, int tableCount) {
        this.dbCount = dbCount;
        this.tableNamePostFix = tableNamePostFix;
        this.tableCount = tableCount;
        this.dbNamePrefix = dbNamePrefix;
        this.tableNamePrefix = tableNamePrefix;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getDbCount() {
        return dbCount;
    }

    public void setDbCount(int dbCount) {
        this.dbCount = dbCount;
    }

    public int getTableCount() {
        return tableCount;
    }

    public void setTableCount(int tableCount) {
        this.tableCount = tableCount;
    }

    public String getTableNamePostFix() {
        return tableNamePostFix;
    }

    public void setTableNamePostFix(String postFix) {
        this.tableNamePostFix = postFix;
    }

    public String getDbNamePrefix() {
        return dbNamePrefix;
    }

    public void setDbNamePrefix(String dbNamePrefix) {
        this.dbNamePrefix = dbNamePrefix;
    }

    public String getTableNamePrefix() {
        return tableNamePrefix;
    }

    public void setTableNamePrefix(String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
    }

    public JdbcTemplate[] getJdbcTemplates() {
        return jdbcTemplates;
    }

    public void setJdbcTemplates(JdbcTemplate[] jdbcTemplates) {
        this.jdbcTemplates = jdbcTemplates;
    }

    public Map<String, JdbcTemplate[]> getJdbcTemplateCluster() {
        return jdbcTemplateCluster;
    }

    public void setJdbcTemplateCluster(Map<String, JdbcTemplate[]> jdbcTemplateCluster) {
        this.jdbcTemplateCluster = jdbcTemplateCluster;
    }

    public boolean isHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(boolean hierarchy) {
        ApiLogger.info("+++++++++++++++++++++++++++++++++++++++");
        ApiLogger.info("Table itemName:" + itemName + ", Hierarchy:" + hierarchy);
        ApiLogger.info("+++++++++++++++++++++++++++++++++++++++");
        this.hierarchy = hierarchy;
    }

    public Map<String, String> getSqls() {
        return sqls;
    }

    public void setSqls(Map<String, String> sqls) {
        this.sqls = sqls;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((itemName == null) ? 0 : itemName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TableItem other = (TableItem) obj;
        if (itemName == null) {
            if (other.itemName != null) return false;
        } else if (!itemName.equals(other.itemName)) return false;
        return true;
    }


}
