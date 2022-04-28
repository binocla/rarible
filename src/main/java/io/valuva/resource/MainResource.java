package io.valuva.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.valuva.models.*;
import io.valuva.service.RaribleService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("")
public class MainResource {
    @Inject
    RaribleService raribleService;

    private void parse() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<FinalResultMutable> list1 = objectMapper.readValue(new File("parsed_data.json"), new TypeReference<>() {
        });
        System.out.println(list1.size());
        System.out.println(list1.get(0).getAttributes().size());
        List<FinalResult> list = new ArrayList<>();
        for (FinalResultMutable finalResultMutable : list1) {
            List<Attribute> attributes = new ArrayList<>();
            for (AttributeMutable attributeMutable : finalResultMutable.getAttributes()) {
                attributes.add(new Attribute(attributeMutable.getKey(), attributeMutable.getValue(), attributeMutable.getRarity(), attributeMutable.getRare(), null, null));
            }
            list.add(new FinalResult(finalResultMutable.getCollection(), finalResultMutable.getId(), finalResultMutable.getMakePriceUsd(), finalResultMutable.getRare(), attributes, null, finalResultMutable.getCreators(), finalResultMutable.getBestBidOrder(), finalResultMutable.getAuctions(), finalResultMutable.getTotalStock(), finalResultMutable.getLastSale()));
        }
        System.out.println(list.size());

        Map<String, Long> map = new HashMap<>();
        Map<Attribute, Long> mapOfAttributes = new HashMap<>();
        for (FinalResult finalResultMutable : list) {
            for (Attribute attributeMutable : finalResultMutable.getAttributes()) {
                if (!map.containsKey(attributeMutable.getKey())) {
                    map.put(attributeMutable.getKey(), 1L);
                } else {
                    map.put(attributeMutable.getKey(), map.get(attributeMutable.getKey()) + 1);
                }

                if (!mapOfAttributes.containsKey(attributeMutable)) {
                    mapOfAttributes.put(attributeMutable, 1L);
                } else {
                    mapOfAttributes.put(attributeMutable, mapOfAttributes.get(attributeMutable) + 1);
                }
            }
        }

        for (FinalResult finalResultMutable : list) {
            List<Attribute> newAttributes = new ArrayList<>();
            for (Attribute attribute : finalResultMutable.getAttributes()) {

                double rarity = mapOfAttributes.get(attribute) / (double) map.get(attribute.getKey());
                newAttributes.add(new Attribute(attribute.getKey(), attribute.getValue(), attribute.getRarity(), attribute.getRare(), rarity, (1 - rarity) / rarity));

            }
            finalResultMutable.setAttributes(newAttributes);
        }
        for (FinalResult finalResult : list) {
            double rare = 0.0;
            for (Attribute attribute : finalResult.getAttributes()) {
                rare += attribute.getGlobalRare() * attribute.getGlobalRare();
            }
            finalResult.setGlobalRare(rare);
        }
        objectMapper.writeValue(new File("nft_80k_global.json"), list);

    }

    @Path("/predictPrice")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public NeuronModel predictPrice(@QueryParam("url") @Encoded String url) {
        return raribleService.predictPrice(url);
    }

}