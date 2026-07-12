package gormrepo

import (
	"context"
	"errors"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
	"gorm.io/gorm"
)

type Repository struct{ DB *gorm.DB }

func (r *Repository) query(ctx context.Context) *gorm.DB {
	return r.DB.WithContext(ctx).
		Preload("StorePrices", func(db *gorm.DB) *gorm.DB { return db.Order("store_id") }).
		Preload("StorePrices.Store")
}

func (r *Repository) FindAll(ctx context.Context) ([]domain.Fruit, error) {
	var models []Fruit
	if err := r.query(ctx).Order("id").Find(&models).Error; err != nil {
		return nil, err
	}
	return mapFruits(models), nil
}

func (r *Repository) FindByName(ctx context.Context, name string) (*domain.Fruit, error) {
	var model Fruit
	err := r.query(ctx).Where("name = ?", name).First(&model).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	fruit := mapFruit(model)
	return &fruit, nil
}

func (r *Repository) Create(ctx context.Context, input domain.Fruit) (int64, error) {
	var id int64
	err := r.DB.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		if err := tx.Raw("SELECT nextval('fruits_seq')").Scan(&id).Error; err != nil {
			return err
		}
		return tx.Create(&Fruit{ID: id, Name: input.Name, Description: input.Description}).Error
	})
	return id, err
}

func mapFruits(models []Fruit) []domain.Fruit {
	result := make([]domain.Fruit, 0, len(models))
	for _, model := range models {
		result = append(result, mapFruit(model))
	}
	return result
}

func mapFruit(model Fruit) domain.Fruit {
	id := model.ID
	result := domain.Fruit{ID: &id, Name: model.Name, Description: model.Description, StorePrices: make([]domain.StorePrice, 0, len(model.StorePrices))}
	for _, price := range model.StorePrices {
		sid := price.Store.ID
		result.StorePrices = append(result.StorePrices, domain.StorePrice{Price: price.Price, Store: domain.Store{
			ID: &sid, Name: price.Store.Name, Currency: price.Store.Currency,
			Address: domain.Address{Address: price.Store.Address, City: price.Store.City, Country: price.Store.Country},
		}})
	}
	return result
}
