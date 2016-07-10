package pku.abe.api.lkme.web.sdk;


import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
}
