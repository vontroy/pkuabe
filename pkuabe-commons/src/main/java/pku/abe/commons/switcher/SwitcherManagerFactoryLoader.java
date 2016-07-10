package pku.abe.commons.switcher;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitcherManagerFactoryLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SwitcherManagerFactoryLoader.class);

    private static class InstanceHolder {

        static SwitcherManagerFactory INSTANCE;

        static {
            try {
                ServiceLoader<SwitcherManagerFactory> loader = ServiceLoader.load(SwitcherManagerFactory.class);
                Iterator<SwitcherManagerFactory> iterator = loader.iterator();
                if (iterator.hasNext()) {
                    INSTANCE = iterator.next();
                    LOG.info("SwitcherManagerFactoryLoader find SwitcherManagerFactory implements " + INSTANCE.getClass().getName());
                } else {
                    INSTANCE = new LocalSwitcherManagerFactory();
                }
            } catch (Exception e) {
                INSTANCE = new LocalSwitcherManagerFactory();
                LOG.error("SwitcherManagerFactoryLoader init error", e);
            }
        }
    }

    public static SwitcherManagerFactory getSwitcherManagerFactory() {
        return InstanceHolder.INSTANCE;
    }
}
