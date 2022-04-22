package io.valuva;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class Collection {
    @NotBlank
    private String id;
    @NotNull
    private Meta meta;

}
