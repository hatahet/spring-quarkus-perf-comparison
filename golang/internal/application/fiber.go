package application

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/config"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/fiberapi"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/repository"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/telemetry"
)

func RunFiber(db *sql.DB, repo repository.Fruits, started time.Time, prefork bool) error {
	cfg := config.Load()
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	shutdownTelemetry, err := telemetry.Start(ctx, "go-fiber", cfg.OTLPEndpoint)
	if err != nil {
		return fmt.Errorf("initialize telemetry: %w", err)
	}
	defer func() {
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = shutdownTelemetry(shutdownCtx)
	}()

	app := fiberapi.API{Repo: repo, DB: db, ReadinessTimeout: cfg.ReadinessTimeout}.App(prefork)
	errCh := make(chan error, 1)
	go func() { errCh <- app.Listen(fmt.Sprintf(":%d", cfg.Port)) }()
	slog.Info(fmt.Sprintf("go-fiber started in %s on port %d (prefork=%t)",
		time.Since(started).Round(time.Millisecond), cfg.Port, prefork))
	select {
	case <-ctx.Done():
		return app.ShutdownWithTimeout(10 * time.Second)
	case err := <-errCh:
		return err
	}
}
