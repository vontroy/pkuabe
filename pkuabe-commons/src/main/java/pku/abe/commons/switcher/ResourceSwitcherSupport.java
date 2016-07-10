package pku.abe.commons.switcher;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSwitcherSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceSwitcherSupport.class);

    /**
     * 资源开关统一命名规则
     */
    public static String SWITCHER_PREFIX = "resource.";

    private String resourceType;
    private String host;
    private int port;

    private Switcher rs;
    private Switcher ws;
    private boolean throwSwitcherException;

    private SwitcherManager swManager;

    private AtomicLong counter = new AtomicLong(0);

    public ResourceSwitcherSupport(String resourceType, String host, int port) {
        this(resourceType, host, port, SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager());
    }

    public ResourceSwitcherSupport(String resourceType, String host, int port, SwitcherManager swManager) {
        super();
        this.resourceType = resourceType;
        this.host = host;
        this.port = port;
        this.swManager = swManager;
        if (swManager == null) {
            rs = Switcher.OPEN_SWITCHER;
            ws = Switcher.OPEN_SWITCHER;
        } else {
            rs = swManager.registerSwitcher(buildName(resourceType, host, port, true), true);
            ws = swManager.registerSwitcher(buildName(resourceType, host, port, false), true);
        }
    }

    private static String buildName(String resourceType, String host, int port, boolean reader) {
        StringBuilder builder = new StringBuilder();
        builder.append(SWITCHER_PREFIX).append(resourceType).append(".").append(host).append(":").append(port).append(":")
                .append(reader ? "read" : "write");
        return builder.toString();
    }

    public static interface Callback<T> {
        T call();
    }

    public Switcher getSwitcher(boolean isReadOp) {
        return isReadOp ? this.rs : this.ws;
    }

    public Switcher getReadSwitcher() {
        return this.rs;
    }

    public Switcher getWriteSwitcher() {
        return this.ws;
    }

    public SwitcherManager getSwitcherMananger() {
        return this.swManager;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setThrowSwitcherException(boolean throwSwitcherException) {
        this.throwSwitcherException = throwSwitcherException;
    }

    public <T> T throwOrReturnValue(boolean isReadOp, T defaultValue, Callback<T> callback) {
        Switcher switcher = this.getSwitcher(isReadOp);
        if (switcher.isClose()) {
            if (this.throwSwitcherException) {
                throw new SwitcherCloseException("resource is close.");
            } else {
                if (isReadOp) {
                    long count = counter.getAndDecrement();
                    if (count % 100 == 0) {
                        LOG.warn("SWITCHER-CLOSE read " + this.host + ":" + this.port + " return default. total:" + count);
                    }
                } else {
                    LOG.warn("SWITCHER-CLOSE write " + this.host + ":" + this.port + " return default.");
                }
                return defaultValue;
            }
        }
        return callback.call();
    }

}
