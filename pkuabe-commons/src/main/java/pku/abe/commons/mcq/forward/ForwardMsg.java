package pku.abe.commons.mcq.forward;

public class ForwardMsg {
    private String type;
    private String msg;
    private long flag; // some writer need flag

    public ForwardMsg(String type, String msg) {
        this.type = type;
        this.msg = msg;
    }

    public String getType() {
        return type;
    }

    public String getMsg() {
        return msg;
    }

    public long getFlag() {
        return flag;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setFlag(long flag) {
        this.flag = flag;
    }

}
