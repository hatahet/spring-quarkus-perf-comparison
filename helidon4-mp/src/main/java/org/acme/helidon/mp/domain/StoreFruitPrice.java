package org.acme.helidon.mp.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_fruit_prices")
public class StoreFruitPrice {
    @EmbeddedId private StoreFruitPriceId id;
    @MapsId("storeId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false) private Store store;
    @MapsId("fruitId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fruit_id", nullable = false) private Fruit fruit;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    protected StoreFruitPrice() { }
    public Store getStore() { return store; }
    public BigDecimal getPrice() { return price; }
}
