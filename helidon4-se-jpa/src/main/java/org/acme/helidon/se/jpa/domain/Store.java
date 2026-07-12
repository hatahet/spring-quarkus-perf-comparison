package org.acme.helidon.se.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String currency;
    @Embedded
    private Address address;

    protected Store() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public Address getAddress() { return address; }
}
