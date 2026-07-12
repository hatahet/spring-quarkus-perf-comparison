package org.acme.micronaut.jooq.service;

import java.util.List;
import java.util.Optional;

import org.acme.micronaut.jooq.dto.FruitDTO;
import org.acme.micronaut.jooq.repository.FruitRepository;

import jakarta.inject.Singleton;

@Singleton
public class FruitService {
  private final FruitRepository repository;

  public FruitService(FruitRepository repository) {
    this.repository = repository;
  }

  public List<FruitDTO> getAllFruits() {
    return repository.findAll();
  }

  public Optional<FruitDTO> getFruitByName(String name) {
    return repository.findByName(name);
  }

  public FruitDTO createFruit(FruitDTO fruit) {
    return repository.create(fruit);
  }
}
