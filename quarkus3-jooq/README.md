# Quarkus 3 with jOOQ

This module implements the fruit API with Quarkus REST and jOOQ's SQL DSL over
the Quarkus-managed Agroal PostgreSQL datasource. It does not use Hibernate ORM
or generate/update the schema; benchmark infrastructure owns schema creation and
seed data.

```shell
./mvnw clean verify
java -jar target/quarkus-app/quarkus-run.jar
```
