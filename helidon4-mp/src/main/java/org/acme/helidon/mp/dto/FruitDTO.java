package org.acme.helidon.mp.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record FruitDTO(Long id, @NotBlank(message = "Name is mandatory") String name,
                       String description, List<StoreFruitPriceDTO> storePrices) {
    public FruitDTO {
        storePrices = storePrices == null ? List.of() : List.copyOf(storePrices);
    }
}
