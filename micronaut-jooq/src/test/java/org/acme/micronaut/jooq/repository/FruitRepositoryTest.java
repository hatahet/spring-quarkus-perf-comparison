package org.acme.micronaut.jooq.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.math.BigDecimal;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class FruitRepositoryTest {
  private static final Field<Long> FRUIT_ID = field(name("f", "id"), Long.class);
  private static final Field<String> FRUIT_NAME = field(name("f", "name"), String.class);
  private static final Field<String> DESCRIPTION = field(name("f", "description"), String.class);
  private static final Field<BigDecimal> PRICE = field(name("p", "price"), BigDecimal.class);
  private static final Field<Long> STORE_ID = field(name("s", "id"), Long.class);
  private static final Field<String> STORE_NAME = field(name("s", "name"), String.class);
  private static final Field<String> CURRENCY = field(name("s", "currency"), String.class);
  private static final Field<String> ADDRESS = field(name("s", "address"), String.class);
  private static final Field<String> CITY = field(name("s", "city"), String.class);
  private static final Field<String> COUNTRY = field(name("s", "country"), String.class);
  private static final Field<?>[] FIELDS = {FRUIT_ID, FRUIT_NAME, DESCRIPTION, PRICE, STORE_ID,
      STORE_NAME, CURRENCY, ADDRESS, CITY, COUNTRY};

  @Test
  void groupsOrderedRowsAndKeepsFruitsWithoutPrices() {
    DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
    Result<Record> rows = dsl.newResult(FIELDS);
    rows.add(row(dsl, 1L, "Apple", "Hearty fruit", new BigDecimal("1.29"), 1L, "Store 1"));
    rows.add(row(dsl, 1L, "Apple", "Hearty fruit", new BigDecimal("1.49"), 2L, "Store 2"));
    rows.add(row(dsl, 2L, "Banana", "Yellow fruit", null, null, null));

    var fruits = FruitRepository.map(rows);

    assertThat(fruits).extracting(fruit -> fruit.name()).containsExactly("Apple", "Banana");
    assertThat(fruits.getFirst().storePrices()).hasSize(2);
    assertThat(fruits.getFirst().storePrices().getFirst().price()).isEqualTo(1.29f);
    assertThat(fruits.getFirst().storePrices().getFirst().store().address().city()).isEqualTo("Anytown");
    assertThat(fruits.get(1).storePrices()).isEmpty();
  }

  private static Record row(DSLContext dsl, Long fruitId, String fruitName, String description,
                            BigDecimal price, Long storeId, String storeName) {
    Record row = dsl.newRecord(FIELDS);
    row.set(FRUIT_ID, fruitId);
    row.set(FRUIT_NAME, fruitName);
    row.set(DESCRIPTION, description);
    row.set(PRICE, price);
    row.set(STORE_ID, storeId);
    row.set(STORE_NAME, storeName);
    row.set(CURRENCY, storeId == null ? null : "USD");
    row.set(ADDRESS, storeId == null ? null : "123 Main St");
    row.set(CITY, storeId == null ? null : "Anytown");
    row.set(COUNTRY, storeId == null ? null : "USA");
    return row;
  }
}
