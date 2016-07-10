package pku.abe.commons.client.balancer;

import pku.abe.commons.client.balancer.impl.EndpointBalancerConfig;

public interface EndpointManager<R> {

    void init(EndpointPool<R> endpointPool, EndpointBalancerConfig config);

    public EndpointBalancerConfig getConfig();

}
