package org.acme.helidon.mp.jooq.repository;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
public class JooqConfiguration {
    @Produces
    @ApplicationScoped
    DSLContext dslContext(@Named("fruits") DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
