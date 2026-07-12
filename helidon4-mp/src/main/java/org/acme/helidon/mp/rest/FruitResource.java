package org.acme.helidon.mp.rest;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.helidon.mp.dto.FruitDTO;
import org.acme.helidon.mp.service.FruitService;

@Path("/fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {
    private final FruitService service;
    @Inject public FruitResource(FruitService service) { this.service = service; }

    @GET
    public List<FruitDTO> getAll() { return service.getAll(); }

    @GET @Path("/{name}")
    public FruitDTO getByName(@PathParam("name") String name) {
        if (name == null || name.isBlank()) throw new BadRequestException("Name is mandatory");
        return service.getByName(name).orElseThrow(NotFoundException::new);
    }

    @POST
    public FruitDTO create(@Valid FruitDTO fruit) {
        try {
            return service.create(fruit);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
