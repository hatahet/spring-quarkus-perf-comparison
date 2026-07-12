package sqlrepo

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
)

const selectFruits = `SELECT f.id, f.name, f.description,
       p.price, s.id, s.name, s.currency, s.address, s.city, s.country
FROM fruits f
LEFT JOIN store_fruit_prices p ON p.fruit_id = f.id
LEFT JOIN stores s ON s.id = p.store_id
%s
ORDER BY f.id, s.id`

type Repository struct{ DB *sql.DB }

func (r *Repository) FindAll(ctx context.Context) ([]domain.Fruit, error) {
	rows, err := r.DB.QueryContext(ctx, fmt.Sprintf(selectFruits, ""))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	return group(rows)
}

func (r *Repository) FindByName(ctx context.Context, name string) (*domain.Fruit, error) {
	rows, err := r.DB.QueryContext(ctx, fmt.Sprintf(selectFruits, "WHERE f.name = $1"), name)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	fruits, err := group(rows)
	if err != nil {
		return nil, err
	}
	if len(fruits) == 0 {
		return nil, nil
	}
	return &fruits[0], nil
}

func (r *Repository) Create(ctx context.Context, fruit domain.Fruit) (id int64, err error) {
	tx, err := r.DB.BeginTx(ctx, nil)
	if err != nil {
		return 0, err
	}
	defer func() {
		if err != nil {
			_ = tx.Rollback()
		}
	}()
	err = tx.QueryRowContext(ctx, `INSERT INTO fruits (id, name, description)
VALUES (nextval('fruits_seq'), $1, $2) RETURNING id`, fruit.Name, fruit.Description).Scan(&id)
	if err != nil {
		return 0, err
	}
	err = tx.Commit()
	return id, err
}

func group(rows *sql.Rows) ([]domain.Fruit, error) {
	fruits := make([]domain.Fruit, 0)
	var current *domain.Fruit
	for rows.Next() {
		var id int64
		var name string
		var description sql.NullString
		var price sql.NullFloat64
		var storeID sql.NullInt64
		var storeName, currency, address, city, country sql.NullString
		if err := rows.Scan(&id, &name, &description, &price, &storeID, &storeName, &currency, &address, &city, &country); err != nil {
			return nil, err
		}
		if current == nil || *current.ID != id {
			fruitID := id
			fruit := domain.Fruit{ID: &fruitID, Name: name, StorePrices: make([]domain.StorePrice, 0)}
			if description.Valid {
				d := description.String
				fruit.Description = &d
			}
			fruits = append(fruits, fruit)
			current = &fruits[len(fruits)-1]
		}
		if storeID.Valid {
			sid := storeID.Int64
			current.StorePrices = append(current.StorePrices, domain.StorePrice{Price: price.Float64, Store: domain.Store{
				ID: &sid, Name: storeName.String, Currency: currency.String,
				Address: domain.Address{Address: address.String, City: city.String, Country: country.String},
			}})
		}
	}
	return fruits, rows.Err()
}
