package io.valuva.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
public class AllItems {
    @JsonIgnore
    private Integer totalUniqueProperties;
    @NotNull
    @Positive
    private Integer total;
    private String continuation;
    @NotNull
    private List<Item> items;
}
