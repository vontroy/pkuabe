package pku.abe.commons.switcher;

public class LocalSwitcherManagerFactory implements SwitcherManagerFactory {

    private SwitcherManager switcherManager;

    public LocalSwitcherManagerFactory() {
        this.switcherManager = new LocalSwitcherManager();
    }

    @Override
    public SwitcherManager getSwitcherManager() {
        return switcherManager;
    }

}
