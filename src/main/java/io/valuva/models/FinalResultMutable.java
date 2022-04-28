package io.valuva.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalResultMutable {
    private String collection;
    private String id;
    private String makePriceUsd;
    private Double rare;
    private List<AttributeMutable> attributes;
    private JsonNode creators;
    private JsonNode bestBidOrder;
    private JsonNode auctions;
    private Integer totalStock;
    private JsonNode lastSale;
}
