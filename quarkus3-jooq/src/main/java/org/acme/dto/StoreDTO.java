package org.acme.dto;

public record StoreDTO(Long id, String name, String currency, AddressDTO address) {
}
