package org.acme.helidon.se.dto;

public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
