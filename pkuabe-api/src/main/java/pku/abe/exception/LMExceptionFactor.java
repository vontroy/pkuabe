package pku.abe.exception;

import java.io.Serializable;

/**
 * Created by qipo on 15/9/29.
 */
public class LMExceptionFactor implements Serializable {

    private static final long serialVersionUID = 425760202285010132L;

    private int errorCode;
    private String errorMessageEn;
    private String errorMessageCn;


    public LMExceptionFactor(int errorCode, String errorMessageEn, String errorMessageCn) {
        this.errorCode = errorCode;
        this.errorMessageEn = errorMessageEn;
        this.errorMessageCn = errorMessageCn;
    }

    /**
     * 系统错误
     */

    public static final LMExceptionFactor LM_SYS_ERROR = new LMExceptionFactor(20000, "system error", "系统错误");

    /**
     * 缺少参数
     */

    public static final LMExceptionFactor LM_SHORT_PARAMETER = new LMExceptionFactor(20001, "short parameter", "缺少参数");

    /**
     * 非法的参数值
     */
    public static final LMExceptionFactor LM_ILLEGAL_PARAMETER_VALUE = new LMExceptionFactor(20002, "illegal parameter value", "非法的参数值");

    /**
     * 数据库操作失败
     */
    public static final LMExceptionFactor LM_FAILURE_DB_OP = new LMExceptionFactor(20003, "failure on database operation", "数据库操作失败");

    /**
     * 协议不支持
     */
    public static final LMExceptionFactor LM_UNSUPPORTED_PROTOCOL = new LMExceptionFactor(20004, "unsupported protocol!", "协议不支持");

    /**
     * 请求过于频繁
     */
    public static final LMExceptionFactor LM_FREQUENCY_REQUEST = new LMExceptionFactor(20005, "frequency request", "请求过于频繁");

    /**
     * 请求过期
     */
    public static final LMExceptionFactor LM_EXPIRED_REQUEST = new LMExceptionFactor(20006, "request is expired", "过期的请求");

    /**
     * 无短连接
     */
    public static final LMExceptionFactor LM_NOT_SHORT_URL = new LMExceptionFactor(20007, "don't have the short url", "无短连接");

    /**
     * get and set
     */

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessageEn() {
        return errorMessageEn;
    }

    public void setErrorMessageEn(String errorMessageEn) {
        this.errorMessageEn = errorMessageEn;
    }

    public String getErrorMessageCn() {
        return errorMessageCn;
    }

    public void setErrorMessageCn(String errorMessageCn) {
        this.errorMessageCn = errorMessageCn;
    }
}
