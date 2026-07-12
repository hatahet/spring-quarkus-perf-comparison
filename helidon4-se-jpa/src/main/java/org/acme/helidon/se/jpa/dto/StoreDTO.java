package org.acme.helidon.se.jpa.dto;

public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
