# Micronaut with jOOQ

Micronaut 4 HTTP/Netty implementation of the fruit API using jOOQ, PostgreSQL, and a
Micronaut-managed HikariCP pool. It uses the external shared schema and never runs migrations.

Build from the repository root:

```shell
./mvnw -pl micronaut-jooq clean package -DskipTests
```

Run with PostgreSQL listening on `localhost:5432`:

```shell
java -jar target/micronaut-jooq.jar
```

The API is available at `http://localhost:8080/fruits` and health at
`http://localhost:8080/health`.
