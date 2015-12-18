package sk.hts.stash.plugin.satis.hook.rest;

import sk.hts.stash.plugin.satis.hook.SatisNotifier;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api")

@Produces({MediaType.APPLICATION_JSON})
public class ApiConfigurationResource {
    private final SatisNotifier satisNotifier;

    /**
     * Constructor
     *
     * @param satisNotifier - SatisNotifier instance
     */
    public ApiConfigurationResource(SatisNotifier satisNotifier) {
        this.satisNotifier = satisNotifier;
    }

    /**
     * Get hook settings
     *
     * @return json encoded settings
     */
    @GET
    public Response getSettings() {
        ApiConfigurationModel apiModel = new ApiConfigurationModel(satisNotifier.getApiUrl());

        return Response.ok(apiModel).build();
    }

    /**
     * Set hook settings
     *
     * @param apiUrl Satis control panel URL address (directory to api)
     *
     * @return json encoded response
     */
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    public Response setSettings(@FormParam("apiurl") final String apiUrl) {
        satisNotifier.setApiUrl(apiUrl);

        return Response.ok(new ApiConfigurationModel(apiUrl)).build();
    }
}
