package pku.abe.commons.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;

public class TraceableFutureTask<V> extends FutureTask<V> {
    private static SwitcherManager swManager;
    private static Switcher cancalEnableSwitcher;

    static {
        swManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
        cancalEnableSwitcher = swManager.registerSwitcher("feature.futuretask.timeoutcancel.enable", false);
    }

    public TraceableFutureTask(Callable<V> callable) {
        super(callable);
    }

    public TraceableFutureTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    public V get(long timeout, TimeUnit unit, boolean cancelTask) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (TimeoutException e) {
            if (cancalEnableSwitcher.isOpen() && cancelTask) {
                super.cancel(true);
            }
            throw e;
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get(timeout, unit, true);
    }

}
