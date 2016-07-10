package pku.abe.commons.json;

import org.codehaus.jackson.JsonNode;

import java.util.Iterator;

public class AdapIterator implements Iterator<JsonNode> {

    private Iterator<JsonNode> iter;

    public AdapIterator(Iterator<JsonNode> iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public JsonNode next() {
        JsonNode node = iter.next();
        return node;
    }

    @Override
    public void remove() {
        iter.remove();
    }
}
