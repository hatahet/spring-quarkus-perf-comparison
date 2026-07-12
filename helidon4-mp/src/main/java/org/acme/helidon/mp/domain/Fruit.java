package org.acme.helidon.mp.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "fruits")
public class Fruit {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fruits_seq")
    @SequenceGenerator(name = "fruits_seq", sequenceName = "fruits_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    @OneToMany(mappedBy = "fruit") private List<StoreFruitPrice> storePrices = new ArrayList<>();
    protected Fruit() { }
    public Fruit(String name, String description) { this.name = name; this.description = description; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<StoreFruitPrice> getStorePrices() { return storePrices; }
}
