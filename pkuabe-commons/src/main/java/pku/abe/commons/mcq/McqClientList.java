package pku.abe.commons.mcq;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import pku.abe.commons.memcache.VikaCacheClient;
import pku.abe.commons.util.HostUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class McqClientList {
    private static Logger logger = Logger.getLogger(McqClientList.class);
    private int minSpareConnections = 25;
    private int maxSpareConnections = 35;
    private long compressThreshold = 512;
    private boolean compressEnable = true;
    private String serverPort = null;
    private Long refreshRate = 1 * 30 * 1000l;
    protected Map<String, VikaCacheClient> existMcqClientMap = new ConcurrentHashMap<String, VikaCacheClient>();


    public int getMinSpareConnections() {
        return minSpareConnections;
    }

    public void setMinSpareConnections(int minSpareConnections) {
        this.minSpareConnections = minSpareConnections;
    }

    public int getMaxSpareConnections() {
        return maxSpareConnections;
    }

    public void setMaxSpareConnections(int maxSpareConnections) {
        this.maxSpareConnections = maxSpareConnections;
    }

    public long getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(long compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    public boolean isCompressEnable() {
        return compressEnable;
    }

    public void setCompressEnable(boolean compressEnable) {
        this.compressEnable = compressEnable;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public Long getRefreshRate() {
        return refreshRate;
    }

    public void setRefreshRate(Long refreshRate) {
        this.refreshRate = refreshRate;
    }

    public enum ClientStatus {
        add, // new add ip status
        remove, // will remove ip status
        old, // no change ip status
    }

    /**
     * @return
     */
    public abstract boolean refresh();


    protected List<VikaCacheClient> getVikaCacheClients() {
        Assert.hasText(this.getServerPort(), "[Assertion failed] - serverPort must have server_domain:port");
        List<VikaCacheClient> clients = new ArrayList<VikaCacheClient>();
        String tempServerPort = this.getServerPort().replaceAll("[,; ]+", ";");
        String[] serverPorts = StringUtils.split(tempServerPort, ";");
        for (int i = 0; i < serverPorts.length; i++) {
            String sPort = serverPorts[i];
            if (StringUtils.isNotBlank(sPort)) {
                List<VikaCacheClient> clientsForSPort = this.getVikaCacheClientsByServerPort(sPort);
                if (CollectionUtils.isNotEmpty(clientsForSPort)) {
                    clients.addAll(clientsForSPort);
                } else {
                    logger.error("parse " + sPort + " clients is empty or null");
                }
            }
        }
        return clients;
    }


    private List<VikaCacheClient> getVikaCacheClientsByServerPort(String serverPort) {
        String domain = StringUtils.substringBefore(serverPort, ":");
        String port = StringUtils.substringAfter(serverPort, ":");
        if (StringUtils.isEmpty(domain) || StringUtils.isEmpty(port)) {
            Assert.hasText(this.getServerPort(), "[Assertion failed] - serverPort must have server_domain:port");
        }

        List<VikaCacheClient> clients = new ArrayList<VikaCacheClient>();
        List<String> ips = null;
        try {
            ips = HostUtils.getIpsByHostName(domain);
            if (CollectionUtils.isEmpty(ips)) {
                logger.error("find ips is empty or null by domain " + domain);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
            logger.error(e.getMessage());
        }

        if (CollectionUtils.isNotEmpty(ips)) {
            for (String ip : ips) {
                String ipPort = ip + ":" + port;
                VikaCacheClient client = existMcqClientMap.get(ipPort);
                if (client == null) {
                    client = new VikaCacheClient();
                    client.setCompressEnable(this.isCompressEnable());
                    client.setMinSpareConnections(this.getMinSpareConnections());
                    client.setMaxSpareConnections(this.getMaxSpareConnections());
                    client.setCompressThreshold(this.getCompressThreshold());
                    client.setPrimitiveAsString(true);
                    client.setServerPort(ip + ":" + port); //
                    client.init();
                    existMcqClientMap.put(ipPort, client);
                }
                clients.add(client);
            }
        }
        return clients;
    }
}
