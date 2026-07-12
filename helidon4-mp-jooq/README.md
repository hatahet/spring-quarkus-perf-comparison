# Helidon 4 MicroProfile with jOOQ

Helidon MP application using Jakarta REST, CDI, MicroProfile Health and
Telemetry, a named HikariCP data source, and jOOQ's SQL DSL. Reads use ordered
left joins and writes use a jOOQ-managed JDBC transaction and `fruits_seq`.

```shell
./mvnw clean verify
java -jar target/helidon4-mp-jooq.jar
```
