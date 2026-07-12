package main

import (
	"context"
	"log/slog"
	"os"
	"time"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/application"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/config"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/database"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/sqlrepo"
)

func main() {
	started := time.Now()
	db, err := database.Open(context.Background(), config.Load().DatabaseURL)
	if err != nil {
		slog.Error("open database", "error", err)
		os.Exit(1)
	}
	defer db.Close()
	if err := application.Run("go-sql", db, &sqlrepo.Repository{DB: db}, started); err != nil {
		slog.Error("application stopped", "error", err)
		os.Exit(1)
	}
}
