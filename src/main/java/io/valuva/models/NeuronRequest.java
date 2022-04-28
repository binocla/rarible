package io.valuva.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NeuronRequest {
    private String collection;
    private String id;
    private Double rare;
    private List<Attribute> attributes;
}
