package pku.abe.data.dao.strategy;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import pku.abe.commons.util.Util;
import org.apache.commons.collections.MapUtils;

import pku.abe.data.dao.util.JdbcTemplate;

public class TableContainer {
    public Map<String, TableItem> tablesItems;
    private static Map<String, TableChannel> channelMap = new ConcurrentHashMap<String, TableChannel>();
    private static AtomicLong atomicLong = new AtomicLong(15);

    /**
     * @param tableItemName
     * @return
     */
    public TableItem getTableItem(String tableItemName) {
        if (MapUtils.isNotEmpty(this.tablesItems)) {
            return tablesItems.get(tableItemName);
        }
        return null;
    }

    public Map<String, TableItem> getTablesItems() {
        return tablesItems;
    }

    public void setTablesItems(Map<String, TableItem> tablesItems) {
        this.tablesItems = tablesItems;
    }

    public TableChannel getTableChannel(String tableItemName, String sqlName, String dbStrategyId, String tableStrategyId) {
        TableChannel channel = getTableChannel(tableItemName, sqlName, (long) Util.crc32(dbStrategyId.getBytes()), (long)Util.crc32(tableStrategyId.getBytes()));

        return channel;
    }

    public TableChannel getTableChannel(String tableItemName, String sqlName, Long dbStrategyId, Long tableStrategyId) {
        TableItem tableItem = this.getTableItem(tableItemName);


        String tableFullName = TableItemHlper.getTableName(tableItem, tableStrategyId);
        String dbFullName = TableItemHlper.getDbName(tableItem, dbStrategyId);
        String sqlKey = TableItemHlper.getSqlKey(dbFullName, tableFullName, sqlName);

        TableChannel channel = channelMap.get(sqlKey);
        if (channel == null) {
            channel = new TableChannel();
            JdbcTemplate template = TableItemHlper.getJdbcTemplate(tableItem, dbStrategyId);
            String sql = TableItemHlper.getSql(tableItem, sqlName, dbStrategyId, tableStrategyId);
            channel.setSql(sql);
            channel.setJdbcTemplate(template);
            channel.setId(atomicLong.incrementAndGet() + System.currentTimeMillis());
            channel.setJdbcTemplate(template);
            channelMap.put(sqlKey, channel);
        }
        return channel;
    }

    public TableChannel getTableChannel(String tableItemName, String sqlName, Long dbStrategyId, Date date) {
        TableItem tableItem = this.getTableItem(tableItemName);


        String tableFullName = TableItemHlper.getTableName(tableItem, date);
        String dbFullName = TableItemHlper.getDbName(tableItem, dbStrategyId);
        String sqlKey = TableItemHlper.getSqlKey(dbFullName, tableFullName, sqlName);

        TableChannel channel = channelMap.get(sqlKey);
        if (channel == null) {
            channel = new TableChannel();
            JdbcTemplate template = TableItemHlper.getJdbcTemplate(tableItem, dbStrategyId);
            String sql = TableItemHlper.getSql(tableItem, sqlName, dbStrategyId, date);
            channel.setSql(sql);
            channel.setJdbcTemplate(template);
            channel.setId(atomicLong.incrementAndGet() + System.currentTimeMillis());
            channel.setJdbcTemplate(template);
            channelMap.put(sqlKey, channel);
        }
        return channel;


    }

    public TableChannel getDefaultTableChannel(String tableItemName, String sqlName) {
        TableItem tableItem = this.getTableItem(tableItemName);


        String tableFullName = tableItem.getTableNamePrefix();
        String dbFullName = tableItem.getDbNamePrefix();
        String sqlKey = TableItemHlper.getSqlKey(dbFullName, tableFullName, sqlName);

        TableChannel channel = channelMap.get(sqlKey);
        if (channel == null) {
            channel = new TableChannel();
            JdbcTemplate template = tableItem.getJdbcTemplates()[0];
            String sql = TableItemHlper.getSql(tableItem, sqlName);
            channel.setSql(sql);
            channel.setJdbcTemplate(template);
            channel.setId(atomicLong.incrementAndGet() + System.currentTimeMillis());
            channel.setJdbcTemplate(template);
            channelMap.put(sqlKey, channel);
        }
        return channel;
    }

}
