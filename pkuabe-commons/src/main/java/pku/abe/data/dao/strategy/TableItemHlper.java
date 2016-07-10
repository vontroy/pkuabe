package pku.abe.data.dao.strategy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.util.ApiUtil;
import pku.abe.commons.util.UuidHelper;
import pku.abe.data.dao.util.JdbcTemplate;

public final class TableItemHlper {
    // private static final int SPLIT_NUMBER = 256;
    private static Logger logger = Logger.getLogger(TableItemHlper.class);
    private static final String DB_NAME_EXPRESSION = "$db$";
    private static final String TABLE_NAME_EXPRESSION = "$tb$";

    public static JdbcTemplate[] chooseJdbcTemplates(TableItem item, Long id) {
        if (item.isHierarchy() == false) {
            return item.getJdbcTemplates();
        }
        Date date = UuidHelper.getDateFromId(id);
        String year = DateFormatUtils.format(date, "yyyy");
        Map<String, JdbcTemplate[]> cluster = item.getJdbcTemplateCluster();
        JdbcTemplate[] templates = cluster.get(year);
        if (ArrayUtils.isEmpty(templates)) {
            ApiLogger.error("!!!!!!!Can not get correct template, fall down to choose backup template!!!!!!, id=" + String.valueOf(id));
            return item.getJdbcTemplates();
        }
        return templates;
    }

    public static JdbcTemplate getJdbcTemplate(TableItem item, Long id) {
        if (item != null && id != null) {
            JdbcTemplate[] templates = chooseJdbcTemplates(item, id);
            if (ArrayUtils.isNotEmpty(templates)) {
                Integer index = ApiUtil.getHash4split(id, item.getDbCount() * Math.max(item.getTableCount(), 1));
                Integer dbIndex = index / item.getTableCount();
                index = dbIndex / (item.getDbCount() / templates.length);
                return templates[index];
            } else {
                logger.error(StringUtils.defaultIfEmpty(item.getTableNamePrefix(), "") + " jdbcTemplates is empty or null");
            }

        } else {
            logger.error("tableitem is null");

        }
        return null;
    }

    public static String getSql(TableItem item, String sqlName, Long dbId, Long tbId) {
        String tableName = getTableName(item, tbId);
        String dbName = getDbName(item, dbId);
        String sql = item.getSqls().get(sqlName);
        if (StringUtils.isNotBlank(sql)) {
            sql = sql.replace("$db$", dbName);
            sql = sql.replace("$tb$", tableName);

        } else {
            logger.error("find the sql by name " + sqlName + " is empty or null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }
        return sql;

    }

    public static String getSql(TableItem item, String sqlName) {
        String tableName = item.getTableNamePrefix();
        String dbName = item.getDbNamePrefix();
        String sql = item.getSqls().get(sqlName);
        if (StringUtils.isNotBlank(sql)) {
            sql = sql.replace("$db$", dbName);
            sql = sql.replace("$tb$", tableName);

        } else {
            logger.error("find the sql by name " + sqlName + " is empty or null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }
        return sql;

    }

    public static String getSql(TableItem item, String sqlName, Long dbId, Date date) {
        String tableName = getTableName(item, date);
        String dbName = getDbName(item, dbId);
        String sql = item.getSqls().get(sqlName);
        if (StringUtils.isNotBlank(sql)) {
            sql = sql.replace(DB_NAME_EXPRESSION, dbName);
            sql = sql.replace(TABLE_NAME_EXPRESSION, tableName);

        } else {
            logger.error("find the sql by name " + sqlName + " is empty or null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }
        return sql;

    }

    public static String getTableName(TableItem item, Long id) {
        if (item != null) {
            TNamePostfixType type = "yymmdd".equalsIgnoreCase(item.getTableNamePostFix())
                    ? TNamePostfixType.yyMMdd
                    : "yymm".equalsIgnoreCase(item.getTableNamePostFix()) ? TNamePostfixType.yyMM : TNamePostfixType.index;
            String tableName = null;
            switch (type) {
                case yyMM: // yymm
                    tableName = getDateTableNameById(item.getTableNamePrefix(), id, false);
                    break;
                case yyMMdd: // yymmdd
                    tableName = getDateTableNameById(item.getTableNamePrefix(), id, true);
                    break;
                case index: // index;
                    tableName = getIndexTableNameById(item, id);
                default:
                    break;
            }
            return tableName;
        } else {
            logger.error("tableitem is null");

        }
        return null;
    }

    public static String getTableName(TableItem item, Date date) {
        if (item != null) {
            TNamePostfixType type = "yymmdd".equalsIgnoreCase(item.getTableNamePostFix())
                    ? TNamePostfixType.yyMMdd
                    : "yymm".equalsIgnoreCase(item.getTableNamePostFix()) ? TNamePostfixType.yyMM : TNamePostfixType.index;
            String tableName = null;
            switch (type) {
                case yyMM: // yymm
                    tableName = getDateTableNameByDate(item.getTableNamePrefix(), date, false);
                    break;
                case yyMMdd: // yymmdd
                    tableName = getDateTableNameByDate(item.getTableNamePrefix(), date, true);
                    break;
                default:
                    break;
            }
            return tableName;
        } else {
            logger.error("tableitem is null");

        }
        return null;
    }

    public static String getDbName(TableItem item, Long id) {
        if (item != null) {
            String dbName = null;
            dbName = getIndexDbNameById(item, id);
            return dbName;
        } else {
            logger.error("tableitem is null");

        }
        return null;
    }

    public static enum TNamePostfixType {
        yyMM(0), yyMMdd(1), index(2);
        private int type = 0;

        private TNamePostfixType(int type) {
            this.type = type;
        }

        public int value() {
            return type;
        }

    }

    private static String getDateTableNameById(String tblPrefix, Long id, boolean isDisplayDay) {
        if (StringUtils.isNotBlank(tblPrefix) && id != null) {
            Long milliseconds = UuidHelper.getTimeFromId(id) * 1000;
            String yyMMdd = DateFormatUtils.format(milliseconds, isDisplayDay ? "yyMMdd" : "yyMM");
            return new StringBuilder().append(tblPrefix).append("_").append(yyMMdd).toString();
        } else {
            logger.error("tblPrefix is null or empty, id is null");
        }
        return null;
    }

    private static String getDateTableNameByDate(String tblPrefix, Date date, boolean isDisplayDay) {
        if (StringUtils.isNotBlank(tblPrefix) && date != null) {
            String yyMMdd = DateFormatUtils.format(date, isDisplayDay ? "yyMMdd" : "yyMM");
            return new StringBuilder().append(tblPrefix).append("_").append(yyMMdd).toString();
        } else {
            logger.error("tblPrefix is null or empty, id is null");
        }
        return null;
    }

    private static String getIndexTableNameById(TableItem item, Long id) {
        if (StringUtils.isNotBlank(item.getTableNamePrefix()) && id != null && item.getTableCount() > 0 && item.getDbCount() > 0) {
            int tblIndex = 0;
            tblIndex = ApiUtil.getHash4split(id, item.getDbCount() * item.getTableCount());
            tblIndex = tblIndex % item.getTableCount();
            return new StringBuilder().append(item.getTableNamePrefix()).append("_").append(tblIndex).toString();
        } else {
            logger.error("tblPrefix is null or empty, date is null");
        }
        return null;
    }

    private static String getIndexDbNameById(TableItem item, Long id) {
        if (StringUtils.isNotBlank(item.getDbNamePrefix()) && id != null) {
            int dbIndex = 0;
            dbIndex = ApiUtil.getHash4split(id, item.getDbCount() * Math.max(item.getTableCount(), 1));
            dbIndex = dbIndex / item.getTableCount();
            return new StringBuilder().append(item.getDbNamePrefix()).append("_").append(dbIndex).toString();
        } else {
            logger.error("tblPrefix is null or empty, date is null");
        }
        return null;
    }

    /**
     * @param dbName
     * @param tableName
     * @param sqlName
     * @return
     */
    public static String getSqlKey(String dbName, String tableName, String sqlName) {
        String sqlCacheKey = new StringBuffer().append(dbName).append(".").append(tableName).append(".").append(sqlName).toString();
        return sqlCacheKey.toString();
    }

    public static class My {
        public String name;
    }

    public static void main(String[] args) {

        calIndex(3317379083796775l);
        calTimeline(5553290834L);
        calIndex(5553290834L);

        // 根据appID和deepLinkId得到deeplink具体的库和表
        System.out.println(getDbTable(10170, 3413620637564930L, "deeplink"));
        // 根据identityId得到client具体的库和表
        System.out.println(getDbTable(3368955058323458L, 3368955058323458L, "client"));
    }

    private static void calTimeline(long id) {
        int dbIndex = 0;
        dbIndex = ApiUtil.getHash4split(id, 32 * Math.max(1, 1));
        dbIndex = dbIndex / 1;

        System.out.println("timeline id:" + id + " db=" + dbIndex + " table=" + getDateTableNameById("aa", id, false));
    }

    public static void calRepost(long id) {
        int dbCount = 8;
        int tableCount = 1;
        int index = ApiUtil.getHash4split(id, dbCount * Math.max(tableCount, 1));
        int dbIndex = index / tableCount;
        int tblIndex = index % tableCount;

        System.out.println("repost id:" + id + " db=" + dbIndex + " table=" + tblIndex);
    }

    private static void calIndex(long id) {
        int dbCount = 32;
        int tableCount = 8;
        int index = ApiUtil.getHash4split(id, dbCount * Math.max(tableCount, 1));
        int dbIndex = index / tableCount;
        int tblIndex = index % tableCount;

        System.out.println("index id:" + id + " db=" + dbIndex + " table=" + tblIndex);
    }

    public static void calContent(long id) {
        int dbIndex = 0;
        dbIndex = ApiUtil.getHash4split(id, 32 * Math.max(1, 1));
        dbIndex = dbIndex / 1;

        System.out.println("content id:" + id + " result:" + dbIndex);
    }

    public static Map<String, String> getDbTable(long dbId, long tableId, String type) {
        Map<String, String> dbTable = new HashMap<>();
        TableItem tableItem;
        if ("deeplink".equals(type)) {
            Date date = UuidHelper.getDateFromId(tableId);
            tableItem = new TableItem("deeplink", 16, "deeplink_info", "yymm", 1);
            dbTable.put("dbName", TableItemHlper.getDbName(tableItem, dbId));
            dbTable.put("tbaleName", TableItemHlper.getTableName(tableItem, date));

        } else if ("client".equals(type)) {
            tableItem = new TableItem("client", 4, "client_info", null, 8);
            dbTable.put("dbName", TableItemHlper.getDbName(tableItem, dbId));
            dbTable.put("tbaleName", TableItemHlper.getTableName(tableItem, tableId));

        } else if ("count".equals(type)) {
            tableItem = new TableItem("count", 1, "url_count", "yymm", 1);
            dbTable.put("dbName", TableItemHlper.getDbName(tableItem, dbId));
            dbTable.put("tbaleName", TableItemHlper.getTableName(tableItem, tableId));
        }
        return dbTable;
    }

}
