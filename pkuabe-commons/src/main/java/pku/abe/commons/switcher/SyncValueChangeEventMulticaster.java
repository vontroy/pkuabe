package pku.abe.commons.switcher;

public class SyncValueChangeEventMulticaster<V, L extends ValueChangeListener<V>> extends AbstractValueChangeEventMulticaster<V, L> {


    @Override
    public void onValueChange(ValueChangeEvent<V> event) {
        for (L listener : this.listeners) {
            listener.onValueChange(event);
        }
    }

}
