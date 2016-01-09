package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion.ReleasesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/releases")
public class ReleasesPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ReleasesPluginRegion pluginRegion;

    private final ResponseHelper responseHelper = ResponseHelper.INSTANCE;

    public ReleasesPluginEndpoints(@Context SoyService soyService, @Context ReleasesPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response releases(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ReleasesPluginRegionInput("", "", "", "", "", "", ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("releases", e);
            return responseHelper.errorResponse("releases failed", e);
        }
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("version") @DefaultValue("") String version,
        @FormParam("repository") @DefaultValue("") String repository,
        @FormParam("email") @DefaultValue("") String email,
        @FormParam("action") @DefaultValue("") String action) {

        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ReleasesPluginRegionInput(key, name, description, version, repository, email, action));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("action", e);
            return responseHelper.errorResponse("action failed", e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/changelog")
    public Response changelog(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        try {
            return Response.ok(pluginRegion.renderChangelog(releaseKey)).build();
        } catch (Exception e) {
            LOG.error("changelog", e);
            return responseHelper.errorResponse("changelog failed", e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/scm")
    public Response scm(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        try {
            return Response.ok(pluginRegion.renderScm(releaseKey)).build();
        } catch (Exception e) {
            LOG.error("scm", e);
            return responseHelper.errorResponse("scm failed", e);
        }
    }

}
