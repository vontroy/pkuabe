package pku.abe.commons.exception;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import pku.abe.commons.log.ApiLogger;

@Provider
@Component
public class LinkedmeExceptionMapper implements ExceptionMapper<RuntimeException> {
    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(RuntimeException e) {
        LMException apiException;
        if (e instanceof LMException) {
            apiException = (LMException) e;
        } else if (e instanceof WebApplicationException) {
            WebApplicationException appException = (WebApplicationException) e;
            int status = appException.getResponse().getStatus();
            if (status == 405) {
                apiException = new LMException(LMExceptionFactor.LM_METHOD_ERROR);
            } else if (status == 404) {
                apiException = new LMException(LMExceptionFactor.LM_API_NOT_EXIST);
            } else if (status >= 400 && status < 500) {
                ApiLogger.error("Unknow WebApplicationException Status:" + status);
                apiException = new LMException(LMExceptionFactor.LM_ILLEGAL_REQUEST);
            } else if (status == 503) {
                apiException = new LMException(LMExceptionFactor.LM_SERVICE_UNAVAILABLE);
            } else {
                apiException = new LMException(LMExceptionFactor.LM_SYS_ERROR);
                ApiLogger.error(
                        "500_ERROR-WebApplicationException status:" + appException.getResponse().getStatus() + ",msg:" + e.getMessage(), e);
            }
        } else {
            apiException = new LMException(LMExceptionFactor.LM_SYS_ERROR);
            ApiLogger.error("500_ERROR:" + e.getMessage(), e);
        }
        return this.buildResponse(apiException);
    }

    private Response buildResponse(LMException e) {
        int status = e.getFactor().getHttpStatus().value();
        ResponseBuilder builder = Response.status(200).entity(e.formatExceptionInfo());
        return builder.build();
    }
}
