package pku.abe.commons.mcq;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import pku.abe.commons.memcache.VikaCacheClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReaderMcqClientList extends McqClientList {
    private static Logger logger = Logger.getLogger(ReaderMcqClientList.class);


    private Map<String, ClientStatus> status = new HashMap<String, ClientStatus>();

    public Map<String, VikaCacheClient> getExistMcqClientMap() {
        return existMcqClientMap;
    }

    public Map<String, ClientStatus> getExistMcqClientStatusMap() {
        return this.status;
    }

    @Override
    public boolean refresh() {
        List<VikaCacheClient> vikaCacheClients = getVikaCacheClients();
        Set<String> statusRemoveKeys = new HashSet<String>();
        if (MapUtils.isNotEmpty(status)) {
            for (String key : status.keySet()) {
                statusRemoveKeys.add(key);
            }
        }

        if (CollectionUtils.isNotEmpty(vikaCacheClients)) {
            logger.info("vikaCacheClients size is: " + vikaCacheClients.size());
            for (VikaCacheClient vikaCacheClient : vikaCacheClients) {
                String key = vikaCacheClient.getServerPort();
                if (status.containsKey(key) && !status.get(key).equals(ClientStatus.remove)) {//
                    status.put(key, ClientStatus.old);
                    statusRemoveKeys.remove(key);
                } else { //
                    status.put(key, ClientStatus.add);
                }
            }
            for (String key : statusRemoveKeys) {
                status.put(key, ClientStatus.remove);
            }
        } else {
            logger.info("vikaCacheClients size is: 0 or empty");
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public static void main(String[] args) {


        ReaderMcqClientList clientList = new ReaderMcqClientList();
        clientList.setServerPort("s4680i.mars.grid.sina.com.cn:4680");
        List<VikaCacheClient> lists = clientList.getVikaCacheClients();
        if (CollectionUtils.isNotEmpty(lists)) {
            for (VikaCacheClient vikaCacheClient : lists) {
                System.out.println(vikaCacheClient.getServerPort());
            }
        }

    }


}
