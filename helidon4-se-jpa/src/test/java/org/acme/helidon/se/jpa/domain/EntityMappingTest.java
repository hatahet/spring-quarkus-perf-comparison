package org.acme.helidon.se.jpa.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMappingTest {
    @Test
    void fruitUsesCanonicalSequence() throws Exception {
        var id = Fruit.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).generator()).isEqualTo("fruits_seq");
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo("fruits_seq");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isOne();
    }
}
