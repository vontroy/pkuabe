package pku.abe.data.model;

import javax.management.Attribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vontroy on 7/10/16.
 */
public class AttributeInfo {
    private String name;
    private String value;

    public AttributeInfo( String name, String value ) {
        this.name = name;
        this.value = value;
    }

    public static Map<Integer, Integer> attributesMatching(AttributeInfo[] attributes, Map<Integer, String> rho){

        Map<Integer, Integer> setI= new HashMap<Integer,Integer>();

        for (int i = 0; i < attributes.length; i++) {
            for (Map.Entry<Integer, String> entry : rho.entrySet()) {
                if (entry.getValue().equals(attributes[i].toString())) {
                    setI.put(entry.getKey(),i);
                }
            }
        }

        return setI;
    }
}
