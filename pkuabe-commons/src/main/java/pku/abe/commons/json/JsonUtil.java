package pku.abe.commons.json;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.util.ApiUtil;
import pku.abe.commons.util.Util;

public class JsonUtil {

    public static String getJsonTextValue(JsonNode node, String defaultValue) {
        return node != null ? node.getTextValue() : defaultValue;
    }

    public static boolean getJsonBooleanValue(JsonNode node, boolean defaultValue) {
        return node != null ? node.getBooleanValue() : defaultValue;
    }

    public static long getJsonLongValue(JsonNode node, long defaultValue) {
        return node != null ? node.getLongValue() : defaultValue;
    }

    public static int getJsonIntValue(JsonNode node, int defaultValue) {
        return node != null ? node.getIntValue() : defaultValue;
    }

    @SuppressWarnings("deprecation")
    public static Date getJsonDateValue(JsonNode node, Date defaultValue) {
        return node != null ? ApiUtil.parseDate(node.getTextValue(), defaultValue) : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getJsonMultiValues(JsonNode node) {
        if (node == null) {
            return Collections.EMPTY_LIST;
        }
        List<String> results = new ArrayList<String>();
        Iterator<JsonNode> values = node.getElements();
        while (values.hasNext()) {
            String value = values.next().toString();
            if (value.trim().length() > 0) {
                results.add(value.trim());
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public static List<Long> getJsonMultiStrsLong(JsonNode node) {
        if (node == null) {
            return Collections.EMPTY_LIST;
        }
        List<Long> results = new ArrayList<Long>();
        Iterator<JsonNode> values = node.getElements();
        while (values.hasNext()) {
            long value = Util.convertLong(values.next().getTextValue());
            if (value > 0) {
                results.add(value);
            }
        }
        return results;
    }

    /**
     * get Long array with node
     *
     * @param node
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<Long> getJsonMultiValuesLong(JsonNode node) {
        if (node == null) {
            return Collections.EMPTY_LIST;
        }
        List<Long> results = new ArrayList<Long>();
        Iterator<JsonNode> values = node.getElements();
        while (values.hasNext()) {
            JsonNode value = values.next();
            results.add(value.getLongValue());
        }
        return results;
    }

    /**
     * xml 1.0: 0-31,127控制字符为非法内容，转为空格
     *
     * @param value
     * @return
     */
    public static String toJsonStr(String value) {
        if (value == null) return null;

        StringBuilder buf = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    buf.append("\\\"");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '\f':
                    buf.append("\\f");
                    break;
                case '\b':
                    buf.append("\\b");
                    break;

                default:
                    if (c < 32 || c == 127) {
                        buf.append(" ");
                    } else {
                        buf.append(c);
                    }
            }
        }
        return buf.toString();
    }

    /**
     * 获取存储JsonBuilder的iterable对应的json串
     *
     * @param iterable
     * @return
     */
    public static String toJson4Iterable(Iterable<JsonBuilder> iterable) {
        StringBuilder sb = new StringBuilder();

        if (iterable != null) {
            sb.append("[");
            int i = 0;
            for (JsonBuilder s : iterable) {
                if (i++ > 0) {
                    sb.append(",");
                }
                sb.append(s.toString());
            }
            sb.append("]");
        } else {
            sb.append("[]");
        }

        return sb.toString();
    }

    public static boolean isValidJsonObject(String json) {
        return isValidJsonObject(json, false);
    }

    // TODO 优化效率
    public static boolean isValidJsonObject(String json, boolean allowBlank) {
        if (StringUtils.isBlank(json)) {
            return allowBlank;
        }
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }
        try {
            JsonWrapper node = new JsonWrapper(json);
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidJsonArray(String json) {
        return isValidJsonArray(json, false);
    }

    public static boolean isValidJsonArray(String json, boolean allowBlank) {
        if (StringUtils.isBlank(json)) {
            return allowBlank;
        }
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return false;
        }
        try {
            JsonWrapper node = new JsonWrapper(json);
            return node.isArray();
        } catch (Exception e) {
            return false;
        }
    }

    public static String toJson(long[] ids) {
        String str = "[]";
        if (ids != null && ids.length > 0) {
            int iMax = ids.length - 1;
            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0;; i++) {
                b.append('"').append(ids[i]).append('"');
                if (i == iMax) {
                    b.append(']').toString();
                    break;
                }
                b.append(", ");
            }
            str = b.toString();
        }
        return str;
    }

    /**
     * toJson Object need implements Jsonable
     *
     * @param values
     * @return
     */
    public static String toJson(Jsonable[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(values[i].toJson());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String mapToJson(Map<Object, Object> map) {
        if (map == null) {
            return "{}";
        }

        try {
            StringWriter out = new StringWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(out, map);
            return out.toString();
        } catch (IOException e) {
            ApiLogger.error("Error: map=" + map, e);
            return "{}";
        }
    }

    public static <T, M> String toMapJson(Map<T, M> map) {
        if (CollectionUtils.isEmpty(map)) {
            return "{}";
        }
        JsonBuilder json = new JsonBuilder();
        for (Entry<T, M> entry : map.entrySet()) {
            M value = entry.getValue();
            if (value instanceof Jsonable) {
                json.appendJsonValue(entry.getKey().toString(), ((Jsonable) value).toJson());
            } else {
                json.append(entry.getKey().toString(), value.toString());
            }
        }
        return json.flip().toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> jsonToMap(String json) {
        if (json == null) {
            return new HashMap<Object, Object>();
        }

        try {
            StringReader in = new StringReader(json);
            JsonParser jp = new JsonFactory().createJsonParser(in);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jp, Map.class);
        } catch (Exception e) {
            ApiLogger.error("Error: json=" + json, e);
            return new HashMap<Object, Object>();
        }
    }
}
