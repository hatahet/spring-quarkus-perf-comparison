package org.acme.helidon.mp.repository;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.acme.helidon.mp.domain.Fruit;

@ApplicationScoped
public class FruitRepository {
    @PersistenceContext(unitName = "fruits")
    private EntityManager entityManager;

    public List<Fruit> findAll() {
        return entityManager.createQuery("""
                select distinct f from Fruit f
                left join fetch f.storePrices p
                left join fetch p.store s
                order by f.id
                """, Fruit.class).getResultList();
    }

    public Optional<Fruit> findByName(String name) {
        return entityManager.createQuery("""
                select distinct f from Fruit f
                left join fetch f.storePrices p
                left join fetch p.store s
                where f.name = :name
                order by f.id
                """, Fruit.class).setParameter("name", name).getResultStream().findFirst();
    }

    public Fruit insert(String name, String description) {
        Fruit fruit = new Fruit(name, description);
        entityManager.persist(fruit);
        entityManager.flush();
        return fruit;
    }
}
