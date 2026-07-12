package org.acme.helidon.mp.dto;

public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
