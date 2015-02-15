package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.StatusPluginRegion;
import com.jivesoftware.os.upena.deployable.region.StatusPluginRegion.StatusPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
@Path("/ui/status")
public class StatusPluginEndpoints {

    private final SoyService soyService;
    private final StatusPluginRegion pluginRegion;

    public StatusPluginEndpoints(@Context SoyService soyService, @Context StatusPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response filter(@QueryParam("cluster") @DefaultValue("") String cluster,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("service") @DefaultValue("") String service) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new StatusPluginRegionInput(cluster, host, service)));
        return Response.ok(rendered).build();
    }

}
