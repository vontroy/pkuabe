package pku.abe.data.model;


import it.unisa.dia.gas.jpbc.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vontroy on 7/10/16.
 */
public class KeyInfo {
    public enum Type{PUBLIC, MASTER, SECRET}
    protected Type type;
    protected Map<String, Element> components;

    public KeyInfo() {
        this.components = new HashMap<>();
    }

    public KeyInfo( Type type ) {
        this();
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType( Type type ) {
        this.type = type;
    }

    public Map<String, Element> getComponents() {
        return components;
    }
}
