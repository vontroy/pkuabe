package pku.abe.commons.mcq.forward;

import java.util.List;
import java.util.Map;

import pku.abe.commons.log.ApiLogger;

public class MsgForwarder {

    private Map<String, List<ForwardFilterWriter>> writerListMap;

    public boolean forward(ForwardMsg forwardMsg) {
        if (forwardMsg == null || forwardMsg.getType() == null || forwardMsg.getMsg() == null) {
            return false;
        }

        return forward(forwardMsg.getType(), forwardMsg.getMsg(), forwardMsg.getFlag());
    }

    private boolean forward(String type, String msg, long flag) {
        if (writerListMap == null || writerListMap.size() == 0) {
            return false;
        }

        List<ForwardFilterWriter> writerList = writerListMap.get(type);
        if (writerList == null || writerList.size() == 0) {
            return false;
        }

        for (ForwardFilterWriter writer : writerList) {
            if (writer == null || !writer.needForward(flag)) {
                continue;
            }

            try {
                writer.writeMsg(msg);
            } catch (Exception e) {
                ApiLogger.error("forward false, type=" + type + ", flag=" + flag + ", writeKey:" + writer.getWriteKey() + ", msg=" + msg,
                        e);
            }
        }

        return true;
    }

    public Map<String, List<ForwardFilterWriter>> getWriterListMap() {
        return writerListMap;
    }

    public void setWriterListMap(Map<String, List<ForwardFilterWriter>> writerListMap) {
        this.writerListMap = writerListMap;
    }

}
