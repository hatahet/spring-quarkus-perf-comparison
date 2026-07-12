package org.acme.helidon.mp.health;

import java.sql.Connection;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
public class DatabaseHealth {
    @ApplicationScoped
    @Liveness
    public static class Live implements HealthCheck {
        @Override public HealthCheckResponse call() { return HealthCheckResponse.up("helidon4-mp"); }
    }

    @ApplicationScoped
    @Readiness
    public static class Ready implements HealthCheck {
        private final DataSource dataSource;
        @Inject public Ready(@Named("fruits") DataSource dataSource) { this.dataSource = dataSource; }
        @Override public HealthCheckResponse call() {
            try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT 1")) {
                return statement.execute() ? HealthCheckResponse.up("database") : HealthCheckResponse.down("database");
            } catch (Exception e) {
                return HealthCheckResponse.named("database").down().withData("error", e.getMessage()).build();
            }
        }
    }
}
