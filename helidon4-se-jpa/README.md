# Helidon 4 SE application-managed JPA

The same Helidon SE HTTP stack as `helidon4-se`, with Hibernate JPA replacing
DB Client. It owns one HikariCP pool and `EntityManagerFactory`, opens an
`EntityManager` per operation, and uses explicit resource-local write
transactions.

```shell
./mvnw clean verify
java -jar target/helidon4-se-jpa.jar
```
