package pku.abe.web;


import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("sdk")
@Component
public class LMSdkResources {

    @Path("/webinit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String webinit(@QueryParam("linkedme_key") String linkedmeKey, @QueryParam("identity_id") String identityId,
            @Context HttpServletRequest request) {

        return "";

    }

    @Path("system_setup")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String SystemSetup() {

        return "{\"\"}";
    }
}
