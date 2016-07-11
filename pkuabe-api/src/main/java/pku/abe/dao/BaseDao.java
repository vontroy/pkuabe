package pku.abe.dao;

import pku.abe.commons.log.ApiLogger;
import pku.abe.data.dao.strategy.TableChannel;
import pku.abe.data.dao.strategy.TableContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by LinkedME01 on 16/3/8.
 */
public abstract class BaseDao {
    protected TableContainer tableContainer;

    public TableContainer getTableContainer() {
        return tableContainer;
    }

    public void setTableContainer(TableContainer tableContainer) {
        this.tableContainer = tableContainer;
    }

    protected Map<TableChannel, Set<Long>> aggregationIds(long[] ids, String tableItemName, String sqlName) {
        Map<TableChannel, Set<Long>> resultMap = new HashMap<TableChannel, Set<Long>>();
        for (int i = 0; i < ids.length; i++) {
            Long id = ids[i];
            if (id != null) {
                TableChannel channel = tableContainer.getTableChannel(tableItemName, sqlName, id, id);
                if (channel != null) {
                    Set<Long> set = resultMap.get(channel);
                    if (set == null) {
                        set = new HashSet<Long>();
                        resultMap.put(channel, set);
                    }
                    set.add(id);
                } else {
                    ApiLogger.info("table channel is null by id:" + id);
                }
            }
        }

        return resultMap;
    }

}
