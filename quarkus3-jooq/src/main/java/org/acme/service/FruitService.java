package org.acme.service;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.dto.FruitDTO;
import org.acme.repository.FruitRepository;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class FruitService {
  private final FruitRepository repository;

  public FruitService(FruitRepository repository) {
    this.repository = repository;
  }

  @WithSpan("FruitService.getAllFruits")
  public List<FruitDTO> getAllFruits() {
    return repository.findAll();
  }

  @WithSpan("FruitService.getFruitByName")
  public Optional<FruitDTO> getFruitByName(@SpanAttribute("arg.name") String name) {
    return repository.findByName(name);
  }

  @WithSpan("FruitService.createFruit")
  public FruitDTO createFruit(@SpanAttribute("arg.fruit") FruitDTO fruit) {
    return repository.create(fruit);
  }
}
