package org.acme.helidon.se;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.health.HealthCheck;
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
import org.acme.helidon.se.repository.FruitRepository;
import org.acme.helidon.se.rest.FruitServiceEndpoint;
import org.acme.helidon.se.service.FruitService;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        long started = System.nanoTime();
        LogConfig.configureRuntime();
        Config config = Config.global();
        DbClient db = DbClient.create(config.get("db"));
        FruitService fruitService = new FruitService(new FruitRepository(db));

        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .mediaContext(MediaContext.builder()
                        .mediaSupportsDiscoverServices(false)
                        .addMediaSupport(JsonbSupport.create(config))
                        .build())
                .addFeature(observeFeature(config, db))
                .routing(routing -> routing.register("/fruits", new FruitServiceEndpoint(fruitService)))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            db.close();
        }, "helidon4-se-shutdown"));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        System.out.println("helidon4-se started in " + elapsedMillis + "ms on port " + server.port());
    }

    private static ObserveFeature observeFeature(Config config, DbClient db) {
        HealthCheck health = DbClientHealthCheck.builder(db)
                .name("database")
                .query()
                .statementName("health")
                .build();
        Tracer tracer = TracerBuilder.create(config.get("tracing")).build();
        return ObserveFeature.builder()
                .addObserver(HealthObserver.builder().details(true).addChecks(new HealthCheck[]{health}).build())
                .addObserver(MetricsObserver.builder()
                        .meterRegistry(MetricsFactory.getInstance(config).globalRegistry())
                        .build())
                .addObserver(TracingObserver.create(tracer))
                .build();
    }
}
