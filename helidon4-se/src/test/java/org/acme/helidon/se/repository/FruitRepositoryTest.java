package org.acme.helidon.se.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbStatementGet;
import io.helidon.dbclient.DbTransaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FruitRepositoryTest {
    @Test
    void groupsJoinedRowsAndKeepsFruitsWithoutPrices() {
        List<DbRow> rows = List.of(
                row(1L, "Apple", "Hearty fruit", 1L, "Store 1", "USD", "123 Main St",
                        "Anytown", "USA", new BigDecimal("1.29")),
                row(1L, "Apple", "Hearty fruit", 2L, "Store 2", "EUR", "456 Main St",
                        "Paris", "France", new BigDecimal("2.49")),
                row(2L, "Pear", "Juicy fruit", null, null, null, null, null, null, null));

        var fruits = FruitRepository.group(rows);

        assertThat(fruits).hasSize(2);
        assertThat(fruits.get(0).name()).isEqualTo("Apple");
        assertThat(fruits.get(0).storePrices()).extracting(price -> price.store().name())
                .containsExactly("Store 1", "Store 2");
        assertThat(fruits.get(1).name()).isEqualTo("Pear");
        assertThat(fruits.get(1).storePrices()).isEmpty();
    }

    @Test
    void bindsNamedParametersAndCommitsReturnedSequenceId() {
        DbClient client = mock(DbClient.class);
        DbTransaction transaction = mock(DbTransaction.class);
        DbStatementGet statement = mock(DbStatementGet.class);
        DbRow returned = mock(DbRow.class);
        DbColumn id = mock(DbColumn.class);
        when(client.transaction()).thenReturn(transaction);
        when(transaction.createNamedGet("insert-fruit")).thenReturn(statement);
        when(statement.addParam(anyString(), anyString())).thenReturn(statement);
        when(statement.execute()).thenReturn(Optional.of(returned));
        when(returned.column("id")).thenReturn(id);
        when(id.get(Long.class)).thenReturn(11L);

        var created = new FruitRepository(client).insert("Papaya", "Orange tropical fruit");

        assertThat(created.id()).isEqualTo(11L);
        verify(statement).addParam("name", "Papaya");
        verify(statement).addParam("description", "Orange tropical fruit");
        var order = inOrder(statement, transaction);
        order.verify(statement).execute();
        order.verify(transaction).commit();
    }

    private static DbRow row(Long fruitId, String fruitName, String fruitDescription,
                             Long storeId, String storeName, String currency, String address,
                             String city, String country, BigDecimal price) {
        Map<String, Object> values = new HashMap<>();
        values.put("fruit_id", fruitId);
        values.put("fruit_name", fruitName);
        values.put("fruit_description", fruitDescription);
        values.put("store_id", storeId);
        values.put("store_name", storeName);
        values.put("currency", currency);
        values.put("address", address);
        values.put("city", city);
        values.put("country", country);
        values.put("price", price);
        DbRow row = mock(DbRow.class);
        when(row.column(anyString())).thenAnswer(invocation -> column(values.get(invocation.getArgument(0))));
        return row;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DbColumn column(Object value) {
        DbColumn column = mock(DbColumn.class);
        when(column.get()).thenReturn(value);
        when(column.get((Class) Long.class)).thenReturn(value);
        when(column.get((Class) String.class)).thenReturn(value);
        when(column.get((Class) BigDecimal.class)).thenReturn(value);
        when(column.getLong()).thenReturn(value instanceof Number number ? number.longValue() : 0L);
        when(column.getString()).thenReturn(value == null ? null : value.toString());
        return column;
    }
}
