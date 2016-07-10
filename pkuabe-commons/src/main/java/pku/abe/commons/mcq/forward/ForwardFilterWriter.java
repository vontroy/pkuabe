package pku.abe.commons.mcq.forward;

import pku.abe.commons.mcq.BaseWriter;

public class ForwardFilterWriter implements BaseWriter {
    private BaseWriter writer;
    private ForwardFilter filter = new DefaultForwardFilter();

    @Override
    public String getWriteKey() {
        return writer.getWriteKey();
    }

    @Override
    public void writeMsg(String msg) {
        writer.writeMsg(msg);
    }

    @Override
    public void writeMsg(long hashKey, String msg) {
        writer.writeMsg(hashKey, msg);
    }

    @Override
    public void writeMsg(byte[] msg) {
        writer.writeMsg(msg);
    }

    @Override
    public void writeMsg(long hashKey, byte[] msg) {
        writer.writeMsg(hashKey, msg);
    }

    public boolean needForward(long flag) {
        return filter.needForward(flag);
    }

    public BaseWriter getWriter() {
        return writer;
    }

    public ForwardFilter getFilter() {
        return filter;
    }

    public void setWriter(BaseWriter writer) {
        this.writer = writer;
    }

    public void setFilter(ForwardFilter filter) {
        this.filter = filter;
    }
}
