package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion.ConfigPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 */
@Singleton
@Path("/ui/config")
public class ConfigPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ConfigPluginRegion pluginRegion;

    public ConfigPluginEndpoints(@Context SoyService soyService, @Context ConfigPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response services(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            Optional.of(new ConfigPluginRegionInput("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false, true, false, "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("aClusterKey") @DefaultValue("") String aClusterKey,
        @FormParam("aCluster") @DefaultValue("") String aCluster,
        @FormParam("aHostKey") @DefaultValue("") String aHostKey,
        @FormParam("aHost") @DefaultValue("") String aHost,
        @FormParam("aServiceKey") @DefaultValue("") String aServiceKey,
        @FormParam("aService") @DefaultValue("") String aService,
        @FormParam("aInstance") @DefaultValue("") String aInstance,
        @FormParam("aReleaseKey") @DefaultValue("") String aReleaseKey,
        @FormParam("aRelease") @DefaultValue("") String aRelease,
        @FormParam("bClusterKey") @DefaultValue("") String bClusterKey,
        @FormParam("bCluster") @DefaultValue("") String bCluster,
        @FormParam("bHostKey") @DefaultValue("") String bHostKey,
        @FormParam("bHost") @DefaultValue("") String bHost,
        @FormParam("bServiceKey") @DefaultValue("") String bServiceKey,
        @FormParam("bService") @DefaultValue("") String bService,
        @FormParam("bInstance") @DefaultValue("") String bInstance,
        @FormParam("bReleaseKey") @DefaultValue("") String bReleaseKey,
        @FormParam("bRelease") @DefaultValue("") String bRelease,
        @FormParam("property") @DefaultValue("") String property,
        @FormParam("value") @DefaultValue("") String value,
        @FormParam("overridden") @DefaultValue("false") boolean overridden,
        @FormParam("service") @DefaultValue("true") boolean service,
        @FormParam("health") @DefaultValue("false") boolean health,
        @FormParam("action") @DefaultValue("") String action) throws Exception {

        ConfigPluginRegionInput configPluginRegionInput = new ConfigPluginRegionInput(
            aClusterKey, aCluster, aHostKey, aHost, aServiceKey, aService, aInstance, aReleaseKey, aRelease, bClusterKey,
            bCluster, bHostKey, bHost, bServiceKey, bService, bInstance, bReleaseKey, bRelease,
            property, value, overridden, service, health, action);
        if (action.equals("export")) {
            String export = pluginRegion.export(configPluginRegionInput);
            return Response.ok(export, MediaType.TEXT_PLAIN_TYPE).build();
        } else {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                Optional.of(configPluginRegionInput));
            return Response.ok(rendered, MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/modify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyConfigs(@Context HttpServletRequest httpRequest, ModifyRequest modifyRequest) {
        Map<String, Map<String, String>> propertyMap = modifyRequest.getUpdates();
        try {
            pluginRegion.modified(httpRequest.getRemoteUser(), propertyMap);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed while setting properties:" + propertyMap);
            return Response.serverError().build();
        }
    }

    public static class ModifyRequest {

        private Map<String, Map<String, String>> updates;

        public Map<String, Map<String, String>> getUpdates() {
            return updates;
        }

        public void setUpdates(Map<String, Map<String, String>> updates) {
            this.updates = updates;
        }
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadConfigFile(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

        saveFile(fileInputStream);
        String output = "You config was uploaded";
        return Response.status(200).entity(output).build();
    }

    private void saveFile(InputStream uploadedInputStream) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.close();

            System.out.println(new String(outputStream.toByteArray()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}