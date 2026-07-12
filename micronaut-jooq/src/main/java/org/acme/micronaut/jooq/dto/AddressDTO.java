package org.acme.micronaut.jooq.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record AddressDTO(String address, String city, String country) {
}
