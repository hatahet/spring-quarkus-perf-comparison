package org.acme.helidon.se.jpa.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.acme.helidon.se.jpa.domain.Fruit;
import org.acme.helidon.se.jpa.domain.Store;
import org.acme.helidon.se.jpa.domain.StoreFruitPrice;
import org.acme.helidon.se.jpa.dto.AddressDTO;
import org.acme.helidon.se.jpa.dto.FruitDTO;
import org.acme.helidon.se.jpa.dto.StoreDTO;
import org.acme.helidon.se.jpa.dto.StoreFruitPriceDTO;

public final class FruitRepository {
    private final EntityManagerFactory entityManagerFactory;

    public FruitRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public List<FruitDTO> findAll() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            return em.createQuery("""
                    select distinct f from Fruit f
                    left join fetch f.storePrices p
                    left join fetch p.store s
                    order by f.id
                    """, Fruit.class).getResultList().stream().map(FruitRepository::toDto).toList();
        }
    }

    public Optional<FruitDTO> findByName(String name) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            return em.createQuery("""
                    select distinct f from Fruit f
                    left join fetch f.storePrices p
                    left join fetch p.store s
                    where f.name = :name
                    order by f.id
                    """, Fruit.class).setParameter("name", name).getResultStream().findFirst().map(FruitRepository::toDto);
        }
    }

    public FruitDTO insert(String name, String description) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Fruit fruit = new Fruit(name, description);
            em.persist(fruit);
            em.flush();
            FruitDTO dto = toDto(fruit);
            transaction.commit();
            return dto;
        } catch (RuntimeException e) {
            if (transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private static FruitDTO toDto(Fruit fruit) {
        List<StoreFruitPriceDTO> prices = fruit.getStorePrices().stream()
                .sorted(Comparator.comparing(price -> price.getStore().getId()))
                .map(FruitRepository::toDto)
                .toList();
        return new FruitDTO(fruit.getId(), fruit.getName(), fruit.getDescription(), prices);
    }

    private static StoreFruitPriceDTO toDto(StoreFruitPrice price) {
        Store store = price.getStore();
        AddressDTO address = new AddressDTO(store.getAddress().getAddress(),
                store.getAddress().getCity(), store.getAddress().getCountry());
        return new StoreFruitPriceDTO(new StoreDTO(store.getId(), store.getName(), store.getCurrency(), address),
                price.getPrice().floatValue());
    }
}
