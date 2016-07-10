package pku.abe.commons.switcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractValueChangeEventMulticaster<V, L extends ValueChangeListener<V>>
        implements
            ValueChangeEventMulticaster<V, L> {

    protected List<L> listeners;

    public AbstractValueChangeEventMulticaster() {
        listeners = new LinkedList<L>();
    }

    @Override
    public synchronized void registerListener(L listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    @Override
    public List<L> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }


}
