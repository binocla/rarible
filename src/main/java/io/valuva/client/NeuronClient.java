package io.valuva.client;

import com.fasterxml.jackson.databind.node.ValueNode;
import io.valuva.models.NeuronModel;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
@Path("/predict_price")
@Retry
@CircuitBreaker
public interface NeuronClient {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes
    NeuronModel getResult(ValueNode neuronRequest);
}
