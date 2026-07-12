package org.acme.helidon.mp.jooq.dto;

public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
