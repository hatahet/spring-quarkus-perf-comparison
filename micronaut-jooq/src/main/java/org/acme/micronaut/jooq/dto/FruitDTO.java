package org.acme.micronaut.jooq.dto;

import java.util.List;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Serdeable
public record FruitDTO(Long id,
                       @NotBlank(message = "Name is mandatory") String name,
                       String description,
                       List<StoreFruitPriceDTO> storePrices) {
  public FruitDTO {
    storePrices = storePrices == null ? List.of() : storePrices;
  }
}
