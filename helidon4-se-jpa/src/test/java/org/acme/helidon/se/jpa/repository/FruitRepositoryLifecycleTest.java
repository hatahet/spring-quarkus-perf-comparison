package org.acme.helidon.se.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FruitRepositoryLifecycleTest {
    private final EntityManagerFactory emf = mock(EntityManagerFactory.class);
    private final EntityManager em = mock(EntityManager.class);
    private final EntityTransaction transaction = mock(EntityTransaction.class);

    @Test
    void commitsAndClosesEntityManager() {
        when(emf.createEntityManager()).thenReturn(em);
        when(em.getTransaction()).thenReturn(transaction);

        new FruitRepository(emf).insert("Grapefruit", "Summer fruit");

        var order = inOrder(transaction, em);
        order.verify(transaction).begin();
        order.verify(em).persist(org.mockito.ArgumentMatchers.any());
        order.verify(em).flush();
        order.verify(transaction).commit();
        order.verify(em).close();
    }

    @Test
    void rollsBackAndClosesEntityManagerOnFailure() {
        when(emf.createEntityManager()).thenReturn(em);
        when(em.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        doThrow(new IllegalStateException("insert failed")).when(em).flush();

        assertThatThrownBy(() -> new FruitRepository(emf).insert("Grapefruit", "Summer fruit"))
                .isInstanceOf(IllegalStateException.class);

        verify(transaction).rollback();
        verify(em).close();
    }
}
