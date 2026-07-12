package org.acme.micronaut.jooq.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
