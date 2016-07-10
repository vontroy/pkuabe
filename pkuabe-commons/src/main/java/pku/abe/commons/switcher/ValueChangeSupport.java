package pku.abe.commons.switcher;

import org.apache.commons.lang.ObjectUtils;

public class ValueChangeSupport<V> {

    protected ValueChangeEventMulticaster<V, ValueChangeListener<V>> multicaster;

    protected volatile V value;

    /**
     * create time;
     */
    protected long ctime;
    /**
     * last modify time
     */
    protected long mtime;

    public ValueChangeSupport() {
        this(null);
    }

    public ValueChangeSupport(V value) {
        multicaster = new SyncValueChangeEventMulticaster<V, ValueChangeListener<V>>();
        this.value = value;
        this.ctime = System.currentTimeMillis();
        this.mtime = System.currentTimeMillis();
    }

    public V setValue(V value) {
        return this.doSetValue(value, false);
    }

    public V setValueQuiet(V value) {
        return this.doSetValue(value, true);
    }

    private synchronized V doSetValue(V value, boolean quiet) {
        V old = this.value;
        this.value = value;
        if (!quiet) {
            if (!ObjectUtils.equals(old, value)) {
                this.multicaster.onValueChange(new ValueChangeEvent<V>(this, old, value));
            }
        }
        this.mtime = System.currentTimeMillis();
        return old;
    }


    public V getValue() {
        return value;
    }

    public long getCtime() {
        return ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void registerListener(ValueChangeListener<V> listener) {
        this.multicaster.registerListener(listener);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        ValueChangeSupport other = (ValueChangeSupport) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
