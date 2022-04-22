package io.valuva;

import io.smallrye.common.constraint.Nullable;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("")
public class MainResource {
    @Inject
    RaribleService raribleService;

    @Path("/collections")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AllCollections getAllCollections(@QueryParam("blockchains") @Nullable String blockchains,
                                            @QueryParam("continuation") @Nullable String continuation,
                                            @QueryParam("size") @Nullable @Positive Integer size) {
        return raribleService.getAllCollections(blockchains, continuation, size);
    }

    @Path("/itemsByCollection")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AllItems getItemsByCollections(@QueryParam("collection") @NotBlank @Encoded String collection,
                                          @QueryParam("continuation") @Nullable String continuation,
                                          @QueryParam("size") @Nullable @Positive Integer size) {
        return raribleService.getItemsByCollection(collection, continuation, size);
    }

    @Path("/allItems")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FinalResult> getAllItems(@QueryParam("sizeOfCollections") @DefaultValue("50") @Positive Integer sizeOfCollections,
                                      @QueryParam("sizeOfItems") @DefaultValue("50") @Positive Integer sizeOfItems) {
        return raribleService.getAllItems(sizeOfCollections, sizeOfItems);
    }
    @Path("/predictPrice")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public NeuronModel predictPrice(@QueryParam("url") @Encoded String url) {
        return raribleService.predictPrice(url);
    }
}