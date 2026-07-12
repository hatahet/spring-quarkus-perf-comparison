package repository

import (
	"context"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
)

type Fruits interface {
	FindAll(context.Context) ([]domain.Fruit, error)
	FindByName(context.Context, string) (*domain.Fruit, error)
	Create(context.Context, domain.Fruit) (int64, error)
}
