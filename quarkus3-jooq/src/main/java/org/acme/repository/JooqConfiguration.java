package org.acme.repository;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
public class JooqConfiguration {
  @Produces
  @ApplicationScoped
  DSLContext dslContext(DataSource dataSource) {
    return DSL.using(dataSource, SQLDialect.POSTGRES);
  }
}
