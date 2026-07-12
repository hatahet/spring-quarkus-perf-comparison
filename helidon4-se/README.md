# Helidon 4 SE DB Client

Helidon SE HTTP/JSON-B application using Helidon DB Client, PostgreSQL JDBC,
and HikariCP. The benchmark infrastructure owns schema creation and seed data.

```shell
./mvnw clean verify
java -jar target/helidon4-se.jar
```

Health and metrics are available below `/observe`; the fruit API is `/fruits`.
