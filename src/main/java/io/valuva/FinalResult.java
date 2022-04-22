package io.valuva;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinalResult {
    private String collection;
    private String id;
    private String makePriceUsd;
    private Double rare;
    private List<Attribute> attributes;
}
