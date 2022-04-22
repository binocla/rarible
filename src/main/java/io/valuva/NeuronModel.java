package io.valuva;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeuronModel {
    @NotBlank
    private String collection;
    @NotBlank
    @JsonProperty("nft_id")
    private String nftId;
    @JsonProperty("predicted_price")
    private Float predictedPrice;
}
