package io.valuva;

import com.fasterxml.jackson.databind.node.ValueNode;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
@Path("/predict_price")
public interface NeuronClient {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes
    NeuronModel getResult(ValueNode neuronRequest);
}
