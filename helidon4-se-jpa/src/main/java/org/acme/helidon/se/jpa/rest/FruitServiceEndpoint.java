package org.acme.helidon.se.jpa.rest;

import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.acme.helidon.se.jpa.dto.FruitDTO;
import org.acme.helidon.se.jpa.service.FruitService;

public final class FruitServiceEndpoint implements HttpService {
    private final FruitService service;
    public FruitServiceEndpoint(FruitService service) { this.service = service; }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/", (req, res) -> res.send(service.getAll()))
                .get("/{name}", this::getByName)
                .post("/", Handler.create(FruitDTO.class, this::create));
    }

    private void getByName(ServerRequest request, ServerResponse response) {
        String name = request.path().pathParameters().get("name");
        if (name == null || name.isBlank()) {
            response.status(Status.BAD_REQUEST_400).send();
            return;
        }
        service.getByName(name).ifPresentOrElse(response::send,
                () -> response.status(Status.NOT_FOUND_404).send());
    }

    private void create(FruitDTO fruit, ServerResponse response) {
        try {
            response.send(service.create(fruit));
        } catch (IllegalArgumentException e) {
            response.status(Status.BAD_REQUEST_400).send();
        }
    }
}
