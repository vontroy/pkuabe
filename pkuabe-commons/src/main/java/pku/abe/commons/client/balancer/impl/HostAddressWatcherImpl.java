package pku.abe.commons.client.balancer.impl;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pku.abe.commons.client.balancer.HostAddressListener;
import pku.abe.commons.client.balancer.HostAddressWatcher;
import pku.abe.commons.client.balancer.util.ClientBalancerConstant;
import pku.abe.commons.client.balancer.util.ClientBalancerLog;
import pku.abe.commons.client.balancer.util.ClientBalancerUtil;
import pku.abe.commons.client.balancer.util.SystemTimer;


/**
 * Watch the addresses of hostname, notify EndpointManager if the host addresses change.
 */

public class HostAddressWatcherImpl implements HostAddressWatcher {

    private static Map<String, Set<HostAddressListener>> hostAddressChangeListeners =
            new ConcurrentHashMap<String, Set<HostAddressListener>>();
    private static Map<String, Set<String>> oldHostIps = new ConcurrentHashMap<String, Set<String>>();

    private static volatile Thread dnsLookupThread = null;

    private static HostAddressWatcherImpl instance = new HostAddressWatcherImpl();

    public static HostAddressWatcherImpl getInstance() {
        return instance;
    }

    @Override
    public synchronized Set<String> register(String hostname, HostAddressListener listener) {

        Set<HostAddressListener> listeners = hostAddressChangeListeners.get(hostname);
        if (listeners == null) {
            listeners = new HashSet<HostAddressListener>();
            hostAddressChangeListeners.put(hostname, listeners);
        }
        listeners.add(listener);

        Set<String> ips = oldHostIps.get(hostname);
        if (ips == null) {
            ips = ClientBalancerUtil.getAllIps(hostname);
            if (ips == null) {
                ips = new HashSet<String>();
            }
            oldHostIps.put(hostname, ips);
        }

        if (dnsLookupThread == null) {
            startWatch();
        }

        return ips;
    }

    public synchronized void tryWatchHostAddressImmediately(String hostname) {
        ClientBalancerLog.log.info("try watchHostAddress , hostname=" + hostname);
        watchHostAddress(hostname);
    }

    private void startWatch() {
        dnsLookupThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        lookupAllHosts();
                    } catch (Exception e) {
                        ClientBalancerLog.log.error("Error: when lookup hostnames!", e);
                    }
                    ClientBalancerUtil.safeSleep(ClientBalancerConstant.DEFAULT_HOST_ADDRESS_WATCH_INTERVAL);
                }
            }
        };
        dnsLookupThread.start();
    }

    private void lookupAllHosts() {
        long startTime = SystemTimer.currentTimeMillis();

        for (String hostname : hostAddressChangeListeners.keySet()) {
            try {
                watchHostAddress(hostname);
            } catch (Exception e) {
                ClientBalancerLog.log.error("Error: when loopup hostname:" + hostname, e);
            }
        }
        long consumeTime = SystemTimer.currentTimeMillis() - startTime;
        ClientBalancerLog.log.info("Dns lookup, hostnames.size={}, time={}", new Object[] {hostAddressChangeListeners.size(), consumeTime});
    }

    private void watchHostAddress(String hostname) {
        Set<String> latestesIpsFromDns = ClientBalancerUtil.getAllIps(hostname);
        if (latestesIpsFromDns == null || latestesIpsFromDns.size() == 0) {
            return;
        }

        if (!ClientBalancerUtil.isEqualSet(latestesIpsFromDns, oldHostIps.get(hostname))) {
            ClientBalancerLog.log.info("Dns changed, hostname={}, newIps={}, oldIps={}",
                    new Object[] {hostname, latestesIpsFromDns, oldHostIps.get(hostname)});

            for (HostAddressListener listener : hostAddressChangeListeners.get(hostname)) {
                listener.onHostAddressChanged(hostname, latestesIpsFromDns);
            }
            oldHostIps.put(hostname, latestesIpsFromDns);
        } else {
            ClientBalancerLog.log.info("Dns not changed, hostname={}, ips={}", new Object[] {hostname, latestesIpsFromDns});
        }
    }

}
