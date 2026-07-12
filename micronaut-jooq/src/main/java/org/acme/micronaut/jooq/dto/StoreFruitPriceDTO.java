package org.acme.micronaut.jooq.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StoreFruitPriceDTO(StoreDTO store, float price) {
}
