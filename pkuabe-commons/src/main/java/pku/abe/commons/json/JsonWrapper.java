package pku.abe.commons.json;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import pku.abe.commons.log.ApiLogger;


public class JsonWrapper {

    private JsonNode root;
    private String rowData;


    public JsonNode getRoot() {
        return root;
    }

    private static JsonFactory factory = new JsonFactory();

    public JsonWrapper(String json) {
        if (json != null && !json.trim().isEmpty()) {
            try {
                JsonParser jp = factory.createJsonParser(new StringReader(json));
                ObjectMapper jtm = new ObjectMapper();
                root = jtm.readTree(jp);
            } catch (Exception e) {
                ApiLogger.warn(new StringBuilder(64).append("JSON-PARSER-ERROR json:").append(json).append(" msg:").append(e.getMessage()));
                // TODO
                throw new RuntimeException(e);
            }
        }
        this.rowData = json;
    }

    public JsonWrapper(JsonNode root) {
        this.root = root;
        this.rowData = root.toString();
    }

    public JsonWrapper get(String name) {
        JsonNode node = getJsonNode(name);
        if (node == null) {
            return null;
        }
        return new JsonWrapper(node);
    }

    public JsonWrapper getFieldValue(String name) {
        return this.get(name);
    }

    public long getLong(String name) {
        return getLong(name, 0l);
    }

    public long getLong(String name, long defaultValue) {
        JsonNode node = getJsonNode(name);
        if (node == null)
            return defaultValue;
        else if (node.isNumber())
            return node.getLongValue();
        else
            return str2long(node.getValueAsText(), defaultValue);
    }

    public int getInt(String name) {
        return getInt(name, 0);
    }

    public String getString(String name) {
        JsonNode node = this.getJsonNode(name);
        if (node == null) {
            return null;
        }
        if (node.isNull()) {
            return null;
        }
        return node.getValueAsText();
    }

    public int getInt(String name, int defaultValue) {
        JsonNode node = getJsonNode(name);
        if (node == null)
            return defaultValue;
        else if (node.isNumber())
            return node.getIntValue();
        else
            return str2int(node.getValueAsText(), defaultValue);
    }

    public JsonWrapper getNode(String name) {
        JsonNode root = getJsonNode(name);

        if (root == null) {
            return null;
        }

        return new JsonWrapper(root);
    }

    /**
     * 对FieldName中包括“.”的node做特殊处理 不对“.”对解析处理
     *
     * @param name 待查找的FieldName
     * @return 如果不存在，则node为null，但JsonWrapper不为null
     */
    public JsonWrapper getNodeWithPeriod(String name) {
        JsonNode node = root;
        if (name == null) {
            node = null;
        } else {
            node = node.get(name);
        }
        return new JsonWrapper(node);
    }

    public boolean isEmpty() {
        return (root == null || root.size() == 0);
    }


    public long getValueAsLong() {
        return this.root.getLongValue();
    }

    public String getValueAsText() {
        if (root.isNull()) {
            return null;
        } else {
            return this.root.getValueAsText();
        }
    }

    public int getValueAsInt() {
        return this.root.getIntValue();
    }

    public JsonNode getJsonNode(String name) {
        if (name == null || root == null) return null;

        JsonNode node = root;
        StringTokenizer st = new StringTokenizer(name, ".");
        while (st.hasMoreTokens()) {
            String key = st.nextToken().trim();
            if (key.isEmpty() || (node = node.get(key)) == null) return null;
        }
        return node;
    }

    public String toString() {
        if (root == null) {
            return null;
        }
        if (root.isValueNode()) {
            return root.getValueAsText();
        }
        return root.toString();
    }

    private int str2int(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long str2long(String s, long defaultValue) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    public boolean isValueNode() {
        return root.isValueNode();
    }

    public boolean isContainerNode() {
        return root.isContainerNode();
    }

    public boolean isMissingNode() {
        return root.isMissingNode();
    }

    public boolean isArray() {
        return root.isArray();
    }

    public boolean isObject() {
        return root.isObject();
    }

    public boolean isNumber() {
        return root.isNumber();
    }

    public boolean isIntegralNumber() {
        return root.isIntegralNumber();
    }

    public boolean isFloatingPointNumber() {
        return root.isFloatingPointNumber();
    }

    public boolean isInt() {
        return root.isInt();
    }

    public boolean isLong() {
        return root.isLong();
    }

    public boolean isDouble() {
        return root.isDouble();
    }

    public boolean isBigDecimal() {
        return root.isBigDecimal();
    }

    public boolean isTextual() {
        return root.isTextual();
    }

    public boolean isBoolean() {
        return root.isBoolean();
    }

    public boolean isNull() {
        return root.isNull();
    }

    public String getTextValue() {
        return root.getTextValue();
    }

    public boolean getBooleanValue() {
        return root.getBooleanValue();
    }

    public Number getNumberValue() {
        return root.getNumberValue();
    }

    public int getIntValue() {
        return root.getIntValue();
    }

    public long getLongValue() {
        return root.getLongValue();
    }

    public double getDoubleValue() {
        return root.getDoubleValue();
    }

    public BigDecimal getDecimalValue() {
        return root.getDecimalValue();
    }

    public Iterator<String> getFieldNames() {
        return root.getFieldNames();
    }

    public Iterator<JsonWrapper> getElements() {
        if (root.isArray()) {
            return new JsonIterator(root.getElements());
        } else if (root.isObject()) {
            return new JsonIterator(root.getElements());
        }
        return JsonIterator.NULL_JSON_ITERATOR;
    }

    public String[] getStringArray() {
        if (root.isArray()) {
            int size = root.size();
            String[] result = new String[size];
            for (int i = 0; i < size; i++) {
                JsonNode value = root.get(i);
                result[i] = value.getValueAsText();
            }
            return result;
        } else {
            return new String[] {root.getValueAsText()};
        }
    }

    public JsonWrapper getElement(int index) {
        if (root.isArray()) {
            JsonNode node = root.get(index);
            if (node != null) {
                return new JsonWrapper(node);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public int size() {
        return root.size();
    }

    public static class JsonIterator implements Iterator<JsonWrapper> {
        public static JsonIterator NULL_JSON_ITERATOR = new JsonIterator(null) {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public JsonWrapper next() {
                return null;
            }
        };
        private Iterator<JsonNode> iterator;

        public JsonIterator(Iterator<JsonNode> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public JsonWrapper next() {
            return new JsonWrapper(iterator.next());
        }

        @Override
        public void remove() {

        }

    }

    public JsonBuilder toJsonBuilder() {
        if (this.rowData != null) {
            return new JsonBuilder(this.rowData, true);
        } else if (this.root != null) {
            return new JsonBuilder(this.root.toString(), true);
        } else {
            return new JsonBuilder();
        }
    }

    public static void main(String[] args) {
        try {
            String sassStr =
                    "{\"status\":0,\"kwdlevel\":0,\"zonelevel\":0,\"userlevel\":1,\"usertype\":1,\"is_push_search\":true,\"push_another_search\":false,\"errmsg\":\"\u64cd\u4f5c\u6210\u529f\",\"errno\":1}";
            JsonWrapper js = new JsonWrapper(sassStr);
            for (Iterator<JsonWrapper> it = js.getElements(); it.hasNext();) {
                JsonWrapper wrapper = it.next();
                System.out.println(wrapper.getRoot().toString());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

