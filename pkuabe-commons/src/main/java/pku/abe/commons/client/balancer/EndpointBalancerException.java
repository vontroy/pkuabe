package pku.abe.commons.client.balancer;


public class EndpointBalancerException extends RuntimeException {

    private static final long serialVersionUID = -1375051485462043421L;

    public EndpointBalancerException(String message) {
        super(message);
    }

    public EndpointBalancerException(Throwable throwable) {
        super(throwable);
    }

    public EndpointBalancerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
