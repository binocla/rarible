package io.valuva;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class Item {
    @NotBlank
    private String collection;
    @NotBlank
    private String id;
    @NotNull
    private Meta meta;
    @NotNull
    @JsonIgnore
    private Integer uniqueProperties;
    private Double rare;
    private BestSellOrder bestSellOrder;
}
