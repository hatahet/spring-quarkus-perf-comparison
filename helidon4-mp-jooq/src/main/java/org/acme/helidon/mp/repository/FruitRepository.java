package org.acme.helidon.mp.jooq.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.helidon.mp.jooq.dto.AddressDTO;
import org.acme.helidon.mp.jooq.dto.FruitDTO;
import org.acme.helidon.mp.jooq.dto.StoreDTO;
import org.acme.helidon.mp.jooq.dto.StoreFruitPriceDTO;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;

@ApplicationScoped
public class FruitRepository {
    private static final Table<?> FRUITS = table(name("fruits")).as("f");
    private static final Table<?> PRICES = table(name("store_fruit_prices")).as("p");
    private static final Table<?> STORES = table(name("stores")).as("s");
    private static final Field<Long> FRUIT_ID = field(name("f", "id"), Long.class);
    private static final Field<String> FRUIT_NAME = field(name("f", "name"), String.class);
    private static final Field<String> DESCRIPTION = field(name("f", "description"), String.class);
    private static final Field<Long> PRICE_FRUIT_ID = field(name("p", "fruit_id"), Long.class);
    private static final Field<Long> PRICE_STORE_ID = field(name("p", "store_id"), Long.class);
    private static final Field<BigDecimal> PRICE = field(name("p", "price"), BigDecimal.class);
    private static final Field<Long> STORE_ID = field(name("s", "id"), Long.class);
    private static final Field<String> STORE_NAME = field(name("s", "name"), String.class);
    private static final Field<String> CURRENCY = field(name("s", "currency"), String.class);
    private static final Field<String> ADDRESS = field(name("s", "address"), String.class);
    private static final Field<String> CITY = field(name("s", "city"), String.class);
    private static final Field<String> COUNTRY = field(name("s", "country"), String.class);

    private final DSLContext dsl;

    @Inject
    public FruitRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<FruitDTO> findAll() {
        return map(selectFruits().orderBy(FRUIT_ID, STORE_ID).fetch());
    }

    public Optional<FruitDTO> findByName(String fruitName) {
        return map(selectFruits().where(FRUIT_NAME.eq(fruitName)).orderBy(STORE_ID).fetch())
                .stream().findFirst();
    }

    public FruitDTO insert(String fruitName, String description) {
        return dsl.transactionResult(configuration -> {
            DSLContext tx = configuration.dsl();
            Long id = tx.select(field("nextval('fruits_seq')", Long.class)).fetchOne(0, Long.class);
            tx.insertInto(table(name("fruits")), field(name("id")), field(name("name")),
                            field(name("description")))
                    .values(id, fruitName, description)
                    .execute();
            return new FruitDTO(id, fruitName, description, List.of());
        });
    }

    private org.jooq.SelectOnConditionStep<? extends Record> selectFruits() {
        return dsl.select(FRUIT_ID, FRUIT_NAME, DESCRIPTION, PRICE, STORE_ID, STORE_NAME,
                        CURRENCY, ADDRESS, CITY, COUNTRY)
                .from(FRUITS)
                .leftJoin(PRICES).on(PRICE_FRUIT_ID.eq(FRUIT_ID))
                .leftJoin(STORES).on(STORE_ID.eq(PRICE_STORE_ID));
    }

    static List<FruitDTO> map(Result<? extends Record> rows) {
        LinkedHashMap<Long, FruitAccumulator> fruits = new LinkedHashMap<>();
        for (Record row : rows) {
            Long fruitId = row.get(FRUIT_ID);
            FruitAccumulator fruit = fruits.computeIfAbsent(fruitId,
                    ignored -> new FruitAccumulator(fruitId, row.get(FRUIT_NAME), row.get(DESCRIPTION)));
            Long storeId = row.get(STORE_ID);
            if (storeId != null) {
                StoreDTO store = new StoreDTO(storeId, row.get(STORE_NAME), row.get(CURRENCY),
                        new AddressDTO(row.get(ADDRESS), row.get(CITY), row.get(COUNTRY)));
                fruit.prices.add(new StoreFruitPriceDTO(store, row.get(PRICE).floatValue()));
            }
        }
        return fruits.values().stream().map(FruitAccumulator::toDto).toList();
    }

    private static final class FruitAccumulator {
        private final Long id;
        private final String name;
        private final String description;
        private final List<StoreFruitPriceDTO> prices = new ArrayList<>();

        private FruitAccumulator(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        private FruitDTO toDto() {
            return new FruitDTO(id, name, description, prices);
        }
    }
}
