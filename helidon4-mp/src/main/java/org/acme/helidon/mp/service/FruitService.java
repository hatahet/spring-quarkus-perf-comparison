package org.acme.helidon.mp.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.helidon.mp.domain.Fruit;
import org.acme.helidon.mp.domain.Store;
import org.acme.helidon.mp.domain.StoreFruitPrice;
import org.acme.helidon.mp.dto.AddressDTO;
import org.acme.helidon.mp.dto.FruitDTO;
import org.acme.helidon.mp.dto.StoreDTO;
import org.acme.helidon.mp.dto.StoreFruitPriceDTO;
import org.acme.helidon.mp.repository.FruitRepository;

@ApplicationScoped
public class FruitService {
    private final FruitRepository repository;

    @Inject
    public FruitService(FruitRepository repository) { this.repository = repository; }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<FruitDTO> getAll() { return repository.findAll().stream().map(FruitService::toDto).toList(); }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<FruitDTO> getByName(String name) { return repository.findByName(name).map(FruitService::toDto); }

    @Transactional
    public FruitDTO create(FruitDTO fruit) {
        if (fruit == null || fruit.name() == null || fruit.name().isBlank()) {
            throw new IllegalArgumentException("Name is mandatory");
        }
        return toDto(repository.insert(fruit.name(), fruit.description()));
    }

    private static FruitDTO toDto(Fruit fruit) {
        return new FruitDTO(fruit.getId(), fruit.getName(), fruit.getDescription(),
                fruit.getStorePrices().stream()
                        .sorted(Comparator.comparing(price -> price.getStore().getId()))
                        .map(FruitService::toDto).toList());
    }

    private static StoreFruitPriceDTO toDto(StoreFruitPrice price) {
        Store store = price.getStore();
        return new StoreFruitPriceDTO(new StoreDTO(store.getId(), store.getName(), store.getCurrency(),
                new AddressDTO(store.getAddress().getAddress(), store.getAddress().getCity(),
                        store.getAddress().getCountry())), price.getPrice().floatValue());
    }
}
