package entrepo

import (
	"context"
	"database/sql"

	"entgo.io/ent/dialect"
	entsql "entgo.io/ent/dialect/sql"
	generated "github.com/quarkusio/spring-quarkus-perf-comparison/golang/ent"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/ent/fruit"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/ent/storefruitprice"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
)

// Repository uses Ent's generated query and mutation builders against the
// externally managed benchmark schema. Schema migration is intentionally not
// invoked by the application.
type Repository struct {
	DB     *sql.DB
	Client *generated.Client
}

func New(db *sql.DB) *Repository {
	driver := entsql.OpenDB(dialect.Postgres, db)
	return &Repository{DB: db, Client: generated.NewClient(generated.Driver(driver))}
}

func (r *Repository) FindAll(ctx context.Context) ([]domain.Fruit, error) {
	models, err := r.Client.Fruit.Query().Order(fruit.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	prices, err := r.Client.StoreFruitPrice.Query().
		Order(storefruitprice.ByID(), storefruitprice.ByStoreID()).WithStore().All(ctx)
	if err != nil {
		return nil, err
	}
	return mapFruits(models, groupPrices(prices)), nil
}

func (r *Repository) FindByName(ctx context.Context, name string) (*domain.Fruit, error) {
	model, err := r.Client.Fruit.Query().Where(fruit.NameEQ(name)).Only(ctx)
	if generated.IsNotFound(err) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	prices, err := r.Client.StoreFruitPrice.Query().Where(storefruitprice.IDEQ(model.ID)).
		Order(storefruitprice.ByStoreID()).WithStore().All(ctx)
	if err != nil {
		return nil, err
	}
	mapped := mapFruit(model, prices)
	return &mapped, nil
}

func (r *Repository) Create(ctx context.Context, input domain.Fruit) (int64, error) {
	tx, err := r.DB.BeginTx(ctx, nil)
	if err != nil {
		return 0, err
	}
	defer tx.Rollback()

	var id int64
	if err := tx.QueryRowContext(ctx, "SELECT nextval('fruits_seq')").Scan(&id); err != nil {
		return 0, err
	}
	driver := entsql.NewDriver(dialect.Postgres, entsql.Conn{ExecQuerier: tx})
	client := generated.NewClient(generated.Driver(driver))
	if _, err := client.Fruit.Create().SetID(id).SetName(input.Name).
		SetNillableDescription(input.Description).Save(ctx); err != nil {
		return 0, err
	}
	if err := tx.Commit(); err != nil {
		return 0, err
	}
	return id, nil
}

func groupPrices(prices []*generated.StoreFruitPrice) map[int64][]*generated.StoreFruitPrice {
	grouped := make(map[int64][]*generated.StoreFruitPrice)
	for _, price := range prices {
		grouped[price.ID] = append(grouped[price.ID], price)
	}
	return grouped
}

func mapFruits(models []*generated.Fruit, prices map[int64][]*generated.StoreFruitPrice) []domain.Fruit {
	result := make([]domain.Fruit, 0, len(models))
	for _, model := range models {
		result = append(result, mapFruit(model, prices[model.ID]))
	}
	return result
}

func mapFruit(model *generated.Fruit, prices []*generated.StoreFruitPrice) domain.Fruit {
	id := model.ID
	result := domain.Fruit{ID: &id, Name: model.Name, Description: model.Description,
		StorePrices: make([]domain.StorePrice, 0, len(prices))}
	for _, price := range prices {
		store := price.Edges.Store
		if store == nil {
			continue
		}
		storeID := store.ID
		result.StorePrices = append(result.StorePrices, domain.StorePrice{Price: price.Price, Store: domain.Store{
			ID: &storeID, Name: store.Name, Currency: store.Currency,
			Address: domain.Address{Address: store.Address, City: store.City, Country: store.Country},
		}})
	}
	return result
}
