package org.acme.helidon.mp.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StoreFruitPriceId implements Serializable {
    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "fruit_id", nullable = false) private Long fruitId;
    protected StoreFruitPriceId() { }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreFruitPriceId that)) return false;
        return Objects.equals(storeId, that.storeId) && Objects.equals(fruitId, that.fruitId);
    }
    @Override public int hashCode() { return Objects.hash(storeId, fruitId); }
}
