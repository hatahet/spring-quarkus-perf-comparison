package entrepo

import (
	"testing"

	generated "github.com/quarkusio/spring-quarkus-perf-comparison/golang/ent"
)

func TestMappingIncludesPricesAndAddress(t *testing.T) {
	description := "Hearty fruit"
	model := &generated.Fruit{ID: 1, Name: "Apple", Description: &description}
	prices := []*generated.StoreFruitPrice{
		{Price: 1.29, StoreID: 1, Edges: generated.StoreFruitPriceEdges{Store: &generated.Store{ID: 1, Name: "Store 1", Currency: "USD", Address: "123 Main St", City: "Anytown", Country: "USA"}}},
		{Price: 2.49, StoreID: 2, Edges: generated.StoreFruitPriceEdges{Store: &generated.Store{ID: 2, Name: "Store 2"}}},
	}
	fruit := mapFruit(model, prices)
	if len(fruit.StorePrices) != 2 || fruit.StorePrices[0].Store.Name != "Store 1" {
		t.Fatalf("unexpected prices: %#v", fruit.StorePrices)
	}
	if fruit.StorePrices[0].Store.Address.Address != "123 Main St" {
		t.Fatalf("unexpected address: %#v", fruit.StorePrices[0].Store.Address)
	}
}

func TestFruitWithoutPricesHasNonNilSlice(t *testing.T) {
	fruit := mapFruit(&generated.Fruit{ID: 10, Name: "Kiwi"}, nil)
	if fruit.StorePrices == nil || len(fruit.StorePrices) != 0 {
		t.Fatalf("unexpected prices: %#v", fruit.StorePrices)
	}
}
