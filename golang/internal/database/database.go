package database

import (
	"context"
	"database/sql"
	"time"

	"github.com/XSAM/otelsql"
	_ "github.com/jackc/pgx/v5/stdlib"
	semconv "go.opentelemetry.io/otel/semconv/v1.30.0"
)

func Open(ctx context.Context, url string) (*sql.DB, error) {
	return OpenWithMaxConnections(ctx, url, 50)
}

func OpenWithMaxConnections(ctx context.Context, url string, maxConnections int) (*sql.DB, error) {
	db, err := otelsql.Open("pgx", url, otelsql.WithAttributes(semconv.DBSystemNameKey.String("postgresql")))
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(maxConnections)
	db.SetMaxIdleConns(maxConnections)
	db.SetConnMaxIdleTime(5 * time.Minute)
	if _, err := otelsql.RegisterDBStatsMetrics(db); err != nil {
		db.Close()
		return nil, err
	}
	return db, nil
}
