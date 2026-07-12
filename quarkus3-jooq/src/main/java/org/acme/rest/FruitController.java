package org.acme.rest;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.dto.FruitDTO;
import org.acme.service.FruitService;

@Path("/fruits")
@Produces(MediaType.APPLICATION_JSON)
public class FruitController {
  private final FruitService service;

  public FruitController(FruitService service) {
    this.service = service;
  }

  @GET
  public List<FruitDTO> getAll() {
    return service.getAllFruits();
  }

  @GET
  @Path("/{name}")
  public Response getFruit(@PathParam("name") String name) {
    if (name == null || name.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return service.getFruitByName(name)
        .map(fruit -> Response.ok(fruit).build())
        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public FruitDTO addFruit(@Valid FruitDTO fruit) {
    return service.createFruit(fruit);
  }
}
