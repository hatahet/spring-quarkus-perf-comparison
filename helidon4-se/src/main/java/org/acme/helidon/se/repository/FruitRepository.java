package org.acme.helidon.se.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbTransaction;
import org.acme.helidon.se.dto.AddressDTO;
import org.acme.helidon.se.dto.FruitDTO;
import org.acme.helidon.se.dto.StoreDTO;
import org.acme.helidon.se.dto.StoreFruitPriceDTO;

public class FruitRepository {
    private final DbClient db;

    public FruitRepository(DbClient db) {
        this.db = db;
    }

    public List<FruitDTO> findAll() {
        return group(db.execute().createNamedQuery("list-fruits").execute().toList());
    }

    public Optional<FruitDTO> findByName(String name) {
        List<FruitDTO> fruits = group(db.execute().createNamedQuery("fruit-by-name")
                .addParam("name", name)
                .execute()
                .toList());
        return fruits.stream().findFirst();
    }

    public FruitDTO insert(String name, String description) {
        DbTransaction transaction = db.transaction();
        try {
            DbRow row = transaction.createNamedGet("insert-fruit")
                    .addParam("name", name)
                    .addParam("description", description)
                    .execute()
                    .orElseThrow(() -> new IllegalStateException("INSERT did not return an id"));
            transaction.commit();
            return new FruitDTO(row.column("id").get(Long.class), name, description, List.of());
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    public boolean isReady() {
        return db.execute().createNamedGet("health").execute().isPresent();
    }

    static List<FruitDTO> group(List<DbRow> rows) {
        Map<Long, FruitAccumulator> fruits = new LinkedHashMap<>();
        for (DbRow row : rows) {
            long fruitId = row.column("fruit_id").getLong();
            FruitAccumulator fruit = fruits.computeIfAbsent(fruitId,
                    ignored -> new FruitAccumulator(fruitId,
                            row.column("fruit_name").getString(),
                            row.column("fruit_description").get(String.class)));
            Optional<Long> storeId = Optional.ofNullable(row.column("store_id").get(Long.class));
            if (storeId.isPresent()) {
                StoreDTO store = new StoreDTO(storeId.get(),
                        row.column("store_name").getString(),
                        row.column("currency").getString(),
                        new AddressDTO(row.column("address").getString(),
                                row.column("city").getString(),
                                row.column("country").getString()));
                fruit.prices.add(new StoreFruitPriceDTO(store, row.column("price").get(BigDecimal.class).floatValue()));
            }
        }
        return fruits.values().stream().map(FruitAccumulator::toDto).toList();
    }

    private static final class FruitAccumulator {
        private final long id;
        private final String name;
        private final String description;
        private final List<StoreFruitPriceDTO> prices = new ArrayList<>();

        private FruitAccumulator(long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        private FruitDTO toDto() {
            return new FruitDTO(id, name, description, prices);
        }
    }
}
