package org.acme.helidon.se.service;

import java.util.List;
import java.util.Optional;

import org.acme.helidon.se.dto.FruitDTO;
import org.acme.helidon.se.repository.FruitRepository;

public final class FruitService {
    private final FruitRepository repository;

    public FruitService(FruitRepository repository) {
        this.repository = repository;
    }

    public List<FruitDTO> getAll() {
        return repository.findAll();
    }

    public Optional<FruitDTO> getByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return repository.findByName(name);
    }

    public FruitDTO create(FruitDTO fruit) {
        if (fruit == null || fruit.name() == null || fruit.name().isBlank()) {
            throw new IllegalArgumentException("Name is mandatory");
        }
        return repository.insert(fruit.name(), fruit.description());
    }
}
