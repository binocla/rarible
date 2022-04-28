package io.valuva.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attribute {
    @NotBlank
    String key;
    @NotBlank
    String value;
    Double rarity;
    Double rare;
    Double globalRarity;
    Double globalRare;
}
