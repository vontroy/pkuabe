package pku.abe.commons.client.balancer;

import java.util.Map;
import java.util.Set;

public interface EndpointHolder<R> {

    Map<String, Integer> getEndpointCounter();

    void addEndpoint(Endpoint<R> endpoint);

    void removeEndpoint(Endpoint<R> endpoint);

    String getHostname();

    int getPort();

    Set<Endpoint<R>> getEndpoints(String ipAddress);
}
