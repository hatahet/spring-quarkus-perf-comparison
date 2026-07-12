package gormrepo

import "testing"

func TestMappingIncludesOrderedPricesAndAddress(t *testing.T) {
	description := "Hearty fruit"
	fruit := mapFruit(Fruit{ID: 1, Name: "Apple", Description: &description, StorePrices: []StorePrice{
		{FruitID: 1, StoreID: 1, Price: 1.29, Store: Store{ID: 1, Name: "Store 1", Currency: "USD", Address: "123 Main St", City: "Anytown", Country: "USA"}},
		{FruitID: 1, StoreID: 2, Price: 2.49, Store: Store{ID: 2, Name: "Store 2"}},
	}})
	if len(fruit.StorePrices) != 2 || fruit.StorePrices[0].Store.Name != "Store 1" {
		t.Fatalf("unexpected prices: %#v", fruit.StorePrices)
	}
	if fruit.StorePrices[0].Store.Address.Address != "123 Main St" {
		t.Fatalf("unexpected address: %#v", fruit.StorePrices[0].Store.Address)
	}
}

func TestFruitWithoutPricesHasNonNilSlice(t *testing.T) {
	fruit := mapFruit(Fruit{ID: 10, Name: "Kiwi"})
	if fruit.StorePrices == nil || len(fruit.StorePrices) != 0 {
		t.Fatalf("unexpected prices: %#v", fruit.StorePrices)
	}
}
