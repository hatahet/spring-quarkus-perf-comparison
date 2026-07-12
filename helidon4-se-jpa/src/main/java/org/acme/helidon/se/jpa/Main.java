package org.acme.helidon.se.jpa;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.jsonb.JsonbSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import io.helidon.webserver.observe.metrics.MetricsObserver;
import io.helidon.webserver.observe.tracing.TracingObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.acme.helidon.se.jpa.repository.FruitRepository;
import org.acme.helidon.se.jpa.rest.FruitServiceEndpoint;
import org.acme.helidon.se.jpa.service.FruitService;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        long started = System.nanoTime();
        LogConfig.configureRuntime();
        Config config = Config.global();
        Tracer tracer = TracerBuilder.create(config.get("tracing")).build();
        HikariDataSource hikari = dataSource(config.get("db"));
        DataSource instrumented = JdbcTelemetry.create(GlobalOpenTelemetry.get()).wrap(hikari);
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.nonJtaDataSource", instrumented);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("fruits", properties);
        FruitService service = new FruitService(new FruitRepository(emf));

        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .mediaContext(MediaContext.builder().mediaSupportsDiscoverServices(false)
                        .addMediaSupport(JsonbSupport.create(config)).build())
                .addFeature(observeFeature(config, tracer, instrumented))
                .routing(routing -> routing.register("/fruits", new FruitServiceEndpoint(service)))
                .build().start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            emf.close();
            hikari.close();
        }, "helidon4-se-jpa-shutdown"));
        System.out.println("helidon4-se-jpa started in "
                + ((System.nanoTime() - started) / 1_000_000) + "ms on port " + server.port());
    }

    private static HikariDataSource dataSource(Config config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.get("url").asString().get());
        hikari.setUsername(config.get("username").asString().get());
        hikari.setPassword(config.get("password").asString().get());
        hikari.setPoolName(config.get("pool-name").asString().orElse("helidon4-se-jpa"));
        hikari.setMaximumPoolSize(config.get("maximum-pool-size").asInt().orElse(50));
        hikari.setInitializationFailTimeout(config.get("initialization-fail-timeout").asLong().orElse(-1L));
        hikari.setConnectionTimeout(config.get("connection-timeout").asLong().orElse(2_000L));
        return new HikariDataSource(hikari);
    }

    private static ObserveFeature observeFeature(Config config, Tracer tracer, DataSource dataSource) {
        return ObserveFeature.builder()
                .addObserver(HealthObserver.builder().details(true)
                        .addCheck(() -> databaseHealth(dataSource), HealthCheckType.READINESS).build())
                .addObserver(MetricsObserver.builder()
                        .meterRegistry(MetricsFactory.getInstance(config).globalRegistry()).build())
                .addObserver(TracingObserver.create(tracer))
                .build();
    }

    private static HealthCheckResponse databaseHealth(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT 1")) {
            return HealthCheckResponse.builder().status(statement.execute()).detail("database", "up").build();
        } catch (Exception e) {
            return HealthCheckResponse.builder().status(false).detail("error", e.getMessage()).build();
        }
    }
}
