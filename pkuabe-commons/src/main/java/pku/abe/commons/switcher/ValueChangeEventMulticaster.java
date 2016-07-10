package pku.abe.commons.switcher;

import java.util.List;

public interface ValueChangeEventMulticaster<V, L extends ValueChangeListener<V>> {

    public void registerListener(L listener);

    public void onValueChange(ValueChangeEvent<V> event);

    public List<L> getListeners();

}
