package io.valuva;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Attribute {
    @NotBlank
    private final String key;
    @NotBlank
    private final String value;
    private final Double rarity;
    private final Double rare;
}
