package io.valuva.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllCollections {
    @NotNull
    @Positive
    private Integer total;
    private Integer remaining;
    private String continuation;
    @NotNull
    private List<Collection> collections;
}
