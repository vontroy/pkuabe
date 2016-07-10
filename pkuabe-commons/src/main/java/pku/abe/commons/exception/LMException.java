package pku.abe.commons.exception;

import pku.abe.commons.json.JsonBuilder;

/**
 * Created by qipo on 15/9/29.
 */
public class LMException extends RuntimeException {

    private LMExceptionFactor factor;
    private String description;

    public LMException(String errMsg) {
        this.description = errMsg;
    }

    public LMException(LMExceptionFactor factor) {
        if (factor != null) {
            this.factor = factor;
            this.description = factor.getErrorMessageEn();
        }
    }

    public LMException(LMExceptionFactor factor, Object message) {
        this.factor = factor;
        if (message == null) {
            description = factor.getErrorMessageEn();
        } else {
            description = factor.getErrorMessageEn() + ", " + message.toString();
        }
    }

    public String formatExceptionInfo() {
        JsonBuilder exceptionInfo = new JsonBuilder();
        exceptionInfo.append("err_code", factor.getErrorCode());
        exceptionInfo.append("err_msg", description);
        return exceptionInfo.flip().toString();
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LMExceptionFactor getFactor() {
        return factor;
    }

    public void setFactor(LMExceptionFactor factor) {
        this.factor = factor;
    }
}
