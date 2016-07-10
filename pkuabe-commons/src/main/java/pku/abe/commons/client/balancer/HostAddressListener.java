package pku.abe.commons.client.balancer;

import java.util.Set;

/**
 * listening the hostAddress change event
 */

public interface HostAddressListener {

    /**
     * notify the listener that the hostAddress changed
     *
     * @param hostname
     * @param latestIps
     */
    void onHostAddressChanged(String hostname, Set<String> latestIps);
}
