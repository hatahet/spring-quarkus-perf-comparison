package org.acme.e2e;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class FruitControllerEndToEndTest {
  @Test
  @Order(1)
  void listsOrderedNestedFruitGraph() {
    get("/fruits").then()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("size()", is(10))
        .body("[0].name", is("Apple"))
        .body("[0].storePrices.size()", is(7))
        .body("[0].storePrices[0].price", is(1.29f))
        .body("[0].storePrices[0].store.name", is("Store 1"))
        .body("[0].storePrices[0].store.currency", is("USD"))
        .body("[0].storePrices[0].store.address.address", is("123 Main St"))
        .body("[0].storePrices[0].store.address.city", is("Anytown"))
        .body("[0].storePrices[0].store.address.country", is("USA"))
        .body("[9].name", is("Kiwi"));
  }

  @Test
  @Order(1)
  void findsExactNameAndReturnsNotFound() {
    get("/fruits/Apple").then()
        .statusCode(Status.OK.getStatusCode())
        .body("name", is("Apple"))
        .body("storePrices.size()", is(7));
    get("/fruits/apple").then().statusCode(Status.NOT_FOUND.getStatusCode());
    get("/fruits/Missing").then().statusCode(Status.NOT_FOUND.getStatusCode());
  }

  @Test
  @Order(1)
  void reportsDatabaseReady() {
    get("/q/health/ready").then()
        .statusCode(Status.OK.getStatusCode())
        .body("status", is("UP"));
  }

  @Test
  @Order(2)
  void validatesAndCreatesFruit() {
    given().contentType(ContentType.JSON)
        .body("{\"name\":\"\",\"description\":\"invalid\"}")
        .post("/fruits").then().statusCode(Status.BAD_REQUEST.getStatusCode());

    given().contentType(ContentType.JSON)
        .body("{\"name\":\"Pomelo\",\"description\":\"Large citrus fruit\"}")
        .post("/fruits").then()
        .statusCode(Status.OK.getStatusCode())
        .body("id", greaterThanOrEqualTo(11))
        .body("name", is("Pomelo"));

    get("/fruits").then().body("size()", is(11));
  }
}
