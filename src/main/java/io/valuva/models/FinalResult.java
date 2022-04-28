package io.valuva.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinalResult {
    private String collection;
    private String id;
    private String makePriceUsd;
    private Double rare;
    private List<Attribute> attributes;
    private Double globalRare;
    private JsonNode creators;
    private JsonNode bestBidOrder;
    private JsonNode auctions;
    private Integer totalStock;
    private JsonNode lastSale;

}
