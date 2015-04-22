package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion.ChangeLogPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/changeLog")
public class ChangeLogPluginEndpoints {

    private final SoyService soyService;
    private final ChangeLogPluginRegion pluginRegion;

    public ChangeLogPluginEndpoints(@Context SoyService soyService, @Context ChangeLogPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response hosts() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ChangeLogPluginRegionInput("", "", "", "", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@FormParam("who") @DefaultValue("") String who,
        @FormParam("what") @DefaultValue("") String what,
        @FormParam("when") @DefaultValue("") String when,
        @FormParam("where") @DefaultValue("") String where,
        @FormParam("why") @DefaultValue("") String why,
        @FormParam("how") @DefaultValue("") String how,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ChangeLogPluginRegionInput(who, what, when, where, why, how, action)));
        return Response.ok(rendered).build();
    }
}
