package org.acme.helidon.se.dto;

import java.util.List;

public record FruitDTO(Long id, String name, String description, List<StoreFruitPriceDTO> storePrices) {
    public FruitDTO {
        storePrices = storePrices == null ? List.of() : List.copyOf(storePrices);
    }
}
