package org.acme.helidon.mp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;

import static org.assertj.core.api.Assertions.assertThat;

class FruitHttpIntegrationTest {
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("fruits")
            .withUsername("fruits")
            .withPassword("fruits");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static int port;

    @BeforeAll
    static void startApplication() throws Exception {
        DATABASE.withCopyToContainer(Transferable.of(Files.readAllBytes(schema())),
                "/docker-entrypoint-initdb.d/001-fruits.sql");
        DATABASE.start();
        port = availablePort();
        System.setProperty("server.port", Integer.toString(port));
        System.setProperty("javax.sql.DataSource.fruits.dataSource.url", DATABASE.getJdbcUrl());
        System.setProperty("javax.sql.DataSource.fruits.dataSource.user", DATABASE.getUsername());
        System.setProperty("javax.sql.DataSource.fruits.dataSource.password", DATABASE.getPassword());
        System.setProperty("otel.sdk.disabled", "true");
        Main.main(new String[0]);
    }

    @AfterAll
    static void stopDatabase() {
        DATABASE.stop();
    }

    @Test
    void servesTheCompleteFruitContractAndDatabaseHealth() throws Exception {
        HttpResponse<String> initial = get("/fruits");
        assertThat(initial.statusCode()).isEqualTo(200);
        assertThat(occurrences(initial.body(), "\"description\":" )).isEqualTo(10);

        HttpResponse<String> apple = get("/fruits/Apple");
        assertThat(apple.statusCode()).isEqualTo(200);
        assertThat(apple.body()).contains("\"name\":\"Apple\"", "\"description\":\"Hearty fruit\"");
        assertThat(occurrences(apple.body(), "\"store\":" )).isEqualTo(7);
        assertInOrder(apple.body(), "Store 1", "Store 2", "Store 3", "Store 4", "Store 5", "Store 6", "Store 8");
        assertThat(apple.body()).contains("\"currency\":\"USD\"", "\"address\":\"123 Main St\"",
                "\"city\":\"Anytown\"", "\"country\":\"USA\"", "\"price\":1.29");

        assertThat(get("/fruits/apple").statusCode()).isEqualTo(404);
        assertThat(get("/fruits/DoesNotExist").statusCode()).isEqualTo(404);

        HttpResponse<String> created = post("{\"name\":\"Papaya\",\"description\":\"Orange tropical fruit\"}");
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(created.body()).contains("\"id\":11", "\"name\":\"Papaya\"");
        assertThat(occurrences(get("/fruits").body(), "\"description\":" )).isEqualTo(11);

        assertThat(post("{\"description\":\"No name\"}").statusCode()).isEqualTo(400);
        assertThat(post("{\"name\":\"   \"}").statusCode()).isEqualTo(400);
        assertThat(occurrences(get("/fruits").body(), "\"description\":" )).isEqualTo(11);

        HttpResponse<String> health = get("/health/ready");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("UP", "database");
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String json) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(uri("/fruits"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static Path schema() {
        Path base = Path.of(System.getProperty("basedir", System.getProperty("user.dir")));
        Path schema = base.resolve("../scripts/dbdata/db.sql").normalize();
        if (!Files.isRegularFile(schema)) {
            schema = base.resolve("scripts/dbdata/db.sql").normalize();
        }
        if (!Files.isRegularFile(schema)) {
            throw new IllegalStateException("Cannot locate canonical scripts/dbdata/db.sql from " + base);
        }
        return schema;
    }

    private static int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }

    private static void assertInOrder(String value, String... needles) {
        int position = -1;
        for (String needle : needles) {
            int next = value.indexOf(needle, position + 1);
            assertThat(next).as("position of %s", needle).isGreaterThan(position);
            position = next;
        }
    }
}
