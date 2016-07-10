package pku.abe.commons.client.balancer;

import java.util.Set;

/**
 * Watch the addresses of hostname, notify EndpointManager if the host addresses change.
 * <p>
 * <p>
 * Now only the endpointManager need knows the change event. If more objects need know, change the
 * watcher to observer-model.
 * </p>
 */

public interface HostAddressWatcher {

    Set<String> register(String hostname, HostAddressListener listener);

    void tryWatchHostAddressImmediately(String hostname);
}
