package domain

type Fruit struct {
	ID          *int64       `json:"id"`
	Name        string       `json:"name"`
	Description *string      `json:"description"`
	StorePrices []StorePrice `json:"storePrices"`
}

type StorePrice struct {
	Store Store   `json:"store"`
	Price float64 `json:"price"`
}

type Store struct {
	ID       *int64  `json:"id"`
	Name     string  `json:"name"`
	Currency string  `json:"currency"`
	Address  Address `json:"address"`
}

type Address struct {
	Address string `json:"address"`
	City    string `json:"city"`
	Country string `json:"country"`
}
