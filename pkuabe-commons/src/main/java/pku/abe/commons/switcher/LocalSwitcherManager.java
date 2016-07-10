package pku.abe.commons.switcher;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalSwitcherManager implements SwitcherManager {


    private ConcurrentHashMap<String, Switcher> switchers = new ConcurrentHashMap<String, Switcher>();

    private static final Logger LOG = LoggerFactory.getLogger(LocalSwitcherManager.class);

    @Override
    public Switcher registerSwitcher(String switcherName, SwitcherChangeListener switcherChangeListener, boolean defaultValue) {
        // 开关数量是可控的，所以可以使用 String.intern
        synchronized (switcherName.intern()) {
            Switcher switcher = switchers.get(switcherName);
            if (switcher == null) {
                switcher = new Switcher(switcherName, defaultValue);
            }
            if (switcherChangeListener != null) {
                switcher.registerListener(switcherChangeListener);
            }
            LOG.info("REGISTER-SWITCHER " + switcherName + " " + defaultValue);
            switchers.put(switcherName, switcher);
            return switcher;
        }
    }

    @Override
    public Switcher registerSwitcher(String switcherName, boolean defaultValue) {
        return this.registerSwitcher(switcherName, null, defaultValue);
    }

    @Override
    public boolean watch(String switcherName, SwitcherChangeListener switcherChangeListener) {
        if (switcherChangeListener == null) {
            throw new IllegalArgumentException("switcherChangeListener is null.");
        }
        Switcher switcher = switchers.get(switcherName);
        if (switcher == null) {
            return false;
        }
        switcher.registerListener(switcherChangeListener);
        return true;
    }

    @Override
    public Switcher getSwitcher(String switcherName) {
        return switchers.get(switcherName);
    }

    @Override
    public Switcher setSwitcher(String switcherName, boolean newValue) {
        Switcher switcher = switchers.get(switcherName);
        if (switcher == null) {
            throw new SwitcherNotFindException("can not find switcher by switcherName:" + switcherName);
        }
        switcher.setValue(newValue);
        return switcher;
    }

    @Override
    public Collection<Switcher> listSwitchers() {
        return switchers.values();
    }

    @Override
    public void clearAll() {
        switchers.clear();
    }

}
