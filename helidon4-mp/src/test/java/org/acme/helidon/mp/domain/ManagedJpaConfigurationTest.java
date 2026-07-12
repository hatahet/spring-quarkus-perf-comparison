package org.acme.helidon.mp.domain;

import java.nio.charset.StandardCharsets;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SequenceGenerator;
import jakarta.transaction.Transactional;
import org.acme.helidon.mp.repository.FruitRepository;
import org.acme.helidon.mp.service.FruitService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedJpaConfigurationTest {
    @Test
    void usesJtaAndCanonicalSequence() throws Exception {
        String persistence = new String(getClass().getResourceAsStream("/META-INF/persistence.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(persistence).contains("transaction-type=\"JTA\"").contains("<jta-data-source>fruits</jta-data-source>");
        var id = Fruit.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).generator()).isEqualTo("fruits_seq");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isOne();
        assertThat(FruitRepository.class.getDeclaredField("entityManager")
                .getAnnotation(PersistenceContext.class).unitName()).isEqualTo("fruits");
        assertThat(FruitService.class.getDeclaredMethod("create", org.acme.helidon.mp.dto.FruitDTO.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }
}
