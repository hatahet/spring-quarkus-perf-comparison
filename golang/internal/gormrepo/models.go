package gormrepo

type Fruit struct {
	ID          int64        `gorm:"column:id;primaryKey;autoIncrement:false"`
	Name        string       `gorm:"column:name"`
	Description *string      `gorm:"column:description"`
	StorePrices []StorePrice `gorm:"foreignKey:FruitID"`
}

func (Fruit) TableName() string { return "fruits" }

type StorePrice struct {
	FruitID int64   `gorm:"column:fruit_id;primaryKey"`
	StoreID int64   `gorm:"column:store_id;primaryKey"`
	Price   float64 `gorm:"column:price;type:numeric(12,2)"`
	Store   Store   `gorm:"foreignKey:StoreID"`
}

func (StorePrice) TableName() string { return "store_fruit_prices" }

type Store struct {
	ID       int64  `gorm:"column:id;primaryKey"`
	Name     string `gorm:"column:name"`
	Currency string `gorm:"column:currency"`
	Address  string `gorm:"column:address"`
	City     string `gorm:"column:city"`
	Country  string `gorm:"column:country"`
}

func (Store) TableName() string { return "stores" }
