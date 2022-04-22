package io.valuva;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BestSellOrder {
    private String maker;
    private String makePriceUsd;
}
