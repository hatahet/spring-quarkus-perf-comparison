package org.acme.micronaut.jooq.controller;

import java.util.List;

import org.acme.micronaut.jooq.dto.FruitDTO;
import org.acme.micronaut.jooq.service.FruitService;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;

@Validated
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/fruits")
public class FruitController {
  private final FruitService service;

  public FruitController(FruitService service) {
    this.service = service;
  }

  @Get
  public List<FruitDTO> getAll() {
    return service.getAllFruits();
  }

  @Get("/{name}")
  public HttpResponse<FruitDTO> getFruit(@Nullable String name) {
    if (name == null || name.isBlank()) {
      return HttpResponse.badRequest();
    }
    return service.getFruitByName(name)
        .map(HttpResponse::ok)
        .orElseGet(HttpResponse::notFound);
  }

  @Post
  public FruitDTO addFruit(@Body @Valid FruitDTO fruit) {
    return service.createFruit(fruit);
  }
}
