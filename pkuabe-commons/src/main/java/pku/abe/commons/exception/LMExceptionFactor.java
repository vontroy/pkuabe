package pku.abe.commons.exception;

import org.apache.commons.httpclient.HttpsURL;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * Created by qipo on 15/9/29.
 */
public class LMExceptionFactor implements Serializable {

    private static final long serialVersionUID = 425760202285010132L;

    private HttpStatus httpStatus;
    private int errorCode;
    private String errorMessageEn;
    private String errorMessageCn;

    public LMExceptionFactor(HttpStatus httpStatus, int errorCode, String errorMessageEn, String errorMessageCn) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessageEn = errorMessageEn;
        this.errorMessageCn = errorMessageCn;
    }

    /**
     * 系统错误,默认
     */
    public static final LMExceptionFactor LM_SYS_ERROR =
            new LMExceptionFactor(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "system error!", "系统错误!");

    /**
     * 服务不可用
     */
    public static final LMExceptionFactor LM_SERVICE_UNAVAILABLE =
            new LMExceptionFactor(HttpStatus.SERVICE_UNAVAILABLE, 50300, "service unavailable!", "服务不可用!");

    /**
     * 请求过于频繁
     */
    public static final LMExceptionFactor LM_FREQUENCY_REQUEST =
            new LMExceptionFactor(HttpStatus.SERVICE_UNAVAILABLE, 50301, "frequency request", "请求过于频繁");

    /**
     * 任务过多，系统繁忙
     */
    public static final LMExceptionFactor LM_SYSTEM_BUSY =
            new LMExceptionFactor(HttpStatus.SERVICE_UNAVAILABLE, 50302, "too many pending tasks, system is busy!", "任务过多，系统繁忙!");

    /**
     * 数据库操作失败
     */
    public static final LMExceptionFactor LM_FAILURE_DB_OP =
            new LMExceptionFactor(HttpStatus.SERVICE_UNAVAILABLE, 50303, "operation database failed", "数据库操作失败");

    /**
     * 创建uuid失败
     */
    public static final LMExceptionFactor LM_UUID_ERROR =
            new LMExceptionFactor(HttpStatus.SERVICE_UNAVAILABLE, 50304, "create uuid failed!", "发号失败!");



    /**
     * 缺少参数
     */

    public static final LMExceptionFactor LM_MISSING_PARAM =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40000, "some params is missing", "缺少参数");

    /**
     * 非法的参数值
     */
    public static final LMExceptionFactor LM_ILLEGAL_PARAM_VALUE =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40001, "illegal param value", "非法的参数值");

    /**
     * 非法请求
     */
    public static final LMExceptionFactor LM_ILLEGAL_REQUEST =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40002, "Illegal Request!", "非法请求!");

    /**
     * 短链接不存在
     */
    public static final LMExceptionFactor LM_NOT_SHORT_URL =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40003, "don't have the short url", "无短连接");

    /**
     * Email已存在
     */
    public static final LMExceptionFactor LM_USER_EMAIL_ALREADY_REGISTERED =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40004, "email already exist", "邮箱已被注册");

    /**
     * Email不存在
     */
    public static final LMExceptionFactor LM_USER_EMAIL_DOESNOT_EXIST =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40005, "email does not exist", "账号不存在");

    /**
     * 密码错误
     */
    public static final LMExceptionFactor LM_USER_WRONG_PWD =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40006, "wrong password", "密码错误");

    /**
     * getDeepLinks 时间区间错误
     */
    public static final LMExceptionFactor LM_WRONG_DATE_DURATION = new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40007, "Invalid date duration!", "时间区间不合法!");

    /**
     * 重置密码失败
     */
    public static final LMExceptionFactor LM_RESET_PWD_FAILED =
            new LMExceptionFactor(HttpStatus.BAD_REQUEST, 40008, "reset password failed", "重置密码失败");

    /**
     * 接口不存在
     */
    public static final LMExceptionFactor LM_API_NOT_EXIST = new LMExceptionFactor(HttpStatus.NOT_FOUND, 40400, "Api not found!", "接口不存在!");

    /**
     * 接口认证失败
     */
    public static final LMExceptionFactor LM_AUTH_FAILED = new LMExceptionFactor(HttpStatus.UNAUTHORIZED, 40100, "Auth failed!", "接口认证失败!");

    /**
     * 协议不支持
     */
    public static final LMExceptionFactor LM_UNSUPPORTED_PROTOCOL =
            new LMExceptionFactor(HttpStatus.UNAUTHORIZED, 40101, "unsupported protocol!", "协议不支持");

    /**
     * 请求过期
     */
    public static final LMExceptionFactor LM_EXPIRED_REQUEST =
            new LMExceptionFactor(HttpStatus.UNAUTHORIZED, 40102, "request is expired", "过期的请求");

    /**
     * ip限制
     */
    public static final LMExceptionFactor LM_IP_LIMIT = new LMExceptionFactor(HttpStatus.FORBIDDEN, 40103, "IP limit!", "IP限制，不能请求该资源!");


    /**
     * Http 方法错误
     */
    public static final LMExceptionFactor LM_METHOD_ERROR = new LMExceptionFactor(HttpStatus.METHOD_NOT_ALLOWED, 40500,
            "HTTP METHOD is not suported for this request!", "请求的HTTP METHOD不支持!");

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



    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
