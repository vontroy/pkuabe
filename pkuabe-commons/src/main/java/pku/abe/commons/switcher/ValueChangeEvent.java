package pku.abe.commons.switcher;

import java.util.EventObject;

public class ValueChangeEvent<V> extends EventObject {
    private static final long serialVersionUID = 7043177976212532849L;

    private final V oldValue;
    private final V newValue;

    public ValueChangeEvent(Object source, V oldValue, V newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public V getOldValue() {
        return oldValue;
    }

    public V getNewValue() {
        return newValue;
    }


}
