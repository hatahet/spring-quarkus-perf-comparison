package org.acme.helidon.mp.jooq.service;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.helidon.mp.jooq.dto.FruitDTO;
import org.acme.helidon.mp.jooq.repository.FruitRepository;

@ApplicationScoped
public class FruitService {
    private final FruitRepository repository;

    @Inject
    public FruitService(FruitRepository repository) { this.repository = repository; }

    public List<FruitDTO> getAll() { return repository.findAll(); }

    public Optional<FruitDTO> getByName(String name) { return repository.findByName(name); }

    public FruitDTO create(FruitDTO fruit) {
        if (fruit == null || fruit.name() == null || fruit.name().isBlank()) {
            throw new IllegalArgumentException("Name is mandatory");
        }
        return repository.insert(fruit.name(), fruit.description());
    }
}
