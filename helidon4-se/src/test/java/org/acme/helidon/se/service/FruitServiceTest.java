package org.acme.helidon.se.service;

import java.util.List;

import org.acme.helidon.se.dto.FruitDTO;
import org.acme.helidon.se.repository.FruitRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FruitServiceTest {
    private final FruitRepository repository = mock(FruitRepository.class);
    private final FruitService service = new FruitService(repository);

    @Test
    void rejectsMissingNameWithoutInsertion() {
        assertThatThrownBy(() -> service.create(new FruitDTO(null, null, "bad", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(new FruitDTO(null, "  ", "bad", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
