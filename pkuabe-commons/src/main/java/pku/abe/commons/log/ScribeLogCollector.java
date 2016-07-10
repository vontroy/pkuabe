package pku.abe.commons.log;

import org.apache.log4j.Logger;

public class ScribeLogCollector implements LogCollector {

    private static final Logger LOG = Logger.getLogger(ScribeLogCollector.class);
    private static final Logger scribe = Logger.getLogger("openapi_prism");


    @Override
    public void log(String logId, String module, String schema, DType type, Object data) {
        if (type == DType.RAWSTR) {
            logRawStr(logId, module, schema, (String) data);
        } else if (type == DType.JSON) {
            logJson(logId, module, schema, (String) data);
        } else {
            LOG.warn(String.format("Unsupport type(%s, %s, %s, %d)", logId, module, schema, type.value()));
        }
    }

    @Override
    public void log(String module, String schema, String json) {
        log(LogCollector.DEFAULT_LOG_ID, module, schema, DType.JSON, json);
    }

    public void logRawStr(String logId, String module, String schema, String data) {
        log0(logId, module, schema, DType.RAWSTR, data);
    }

    public void logJson(String logId, String module, String schema, String json) {
        log0(logId, module, schema, DType.JSON, json);
    }

    private void log0(String logId, String module, String schema, DType type, String data) {
        if (!scribe.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(logId);
        sb.append(" ").append(module);
        sb.append(" ").append(schema);
        sb.append(" ").append(type.value());
        sb.append(" ").append(data);
        scribe.info(sb.toString());
    }

}
