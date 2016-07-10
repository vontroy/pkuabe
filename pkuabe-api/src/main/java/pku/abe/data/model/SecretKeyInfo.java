package pku.abe.data.model;

/**
 * Created by vontroy on 7/10/16.
 */
public class SecretKeyInfo extends KeyInfo {
    private AttributeInfo[] attributes;

    public SecretKeyInfo() {
        super(Type.SECRET);
    }

    public AttributeInfo[] getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributeInfo[] attributes) {
        this.attributes = attributes;
    }
}
