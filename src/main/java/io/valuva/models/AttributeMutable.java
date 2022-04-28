package io.valuva.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeMutable {
    @NotBlank
    String key;
    @NotBlank
    String value;
    Double rarity;
    Double rare;
}
