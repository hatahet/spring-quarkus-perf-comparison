package org.acme.helidon.mp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {
    @Column(nullable = false) private String address;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private String country;
    protected Address() { }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
}
