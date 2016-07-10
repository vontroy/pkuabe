package pku.abe.commons.switcher;

public class Switcher extends ValueChangeSupport<Boolean> {

    public static final Switcher OPEN_SWITCHER = new Switcher("OPEN_SWITCHER", true) {

        @Override
        public Boolean setValue(Boolean value) {
            throw new UnsupportedOperationException("this switcher is readonly.");
        }

        @Override
        public Boolean setValueQuiet(Boolean value) {
            throw new UnsupportedOperationException("this switcher is readonly.");
        }

    };

    public static final Switcher CLOSE_SWITCHER = new Switcher("CLOSE_SWITCHER", false) {

        @Override
        public Boolean setValue(Boolean value) {
            throw new UnsupportedOperationException("this switcher is readonly.");
        }

        @Override
        public Boolean setValueQuiet(Boolean value) {
            throw new UnsupportedOperationException("this switcher is readonly.");
        }

    };

    private String name;


    public Switcher(String name, boolean value) {
        super(value);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean booleanValue() {
        // 如果为null，默认为开状态
        return this.value == null ? true : this.value.booleanValue();
    }

    public boolean isOpen() {
        return this.booleanValue();
    }

    public boolean isClose() {
        return !this.booleanValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Switcher other = (Switcher) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Switcher [name=");
        builder.append(name);
        builder.append(", value=");
        builder.append(value);
        builder.append("]");
        return builder.toString();
    }

}
