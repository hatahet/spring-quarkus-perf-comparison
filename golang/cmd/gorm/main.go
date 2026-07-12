package main

import (
	"context"
	"log/slog"
	"os"
	"time"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/application"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/config"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/database"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/gormrepo"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func main() {
	started := time.Now()
	db, err := database.Open(context.Background(), config.Load().DatabaseURL)
	if err != nil {
		slog.Error("open database", "error", err)
		os.Exit(1)
	}
	defer db.Close()
	gormDB, err := gorm.Open(postgres.New(postgres.Config{Conn: db}), &gorm.Config{})
	if err != nil {
		slog.Error("open gorm", "error", err)
		os.Exit(1)
	}
	if err := application.Run("go-gorm", db, &gormrepo.Repository{DB: gormDB}, started); err != nil {
		slog.Error("application stopped", "error", err)
		os.Exit(1)
	}
}
