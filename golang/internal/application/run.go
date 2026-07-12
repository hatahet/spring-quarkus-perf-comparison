package application

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/config"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/httpapi"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/repository"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/telemetry"
)

func Run(service string, db *sql.DB, repo repository.Fruits, started time.Time) error {
	cfg := config.Load()
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	shutdownTelemetry, err := telemetry.Start(ctx, service, cfg.OTLPEndpoint)
	if err != nil {
		return fmt.Errorf("initialize telemetry: %w", err)
	}
	defer func() {
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = shutdownTelemetry(shutdownCtx)
	}()

	server := &http.Server{
		Addr:              fmt.Sprintf(":%d", cfg.Port),
		Handler:           httpapi.API{Repo: repo, DB: db, ReadinessTimeout: cfg.ReadinessTimeout}.Handler(),
		ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 15 * time.Second, WriteTimeout: 30 * time.Second, IdleTimeout: 60 * time.Second,
	}
	errCh := make(chan error, 1)
	go func() { errCh <- server.ListenAndServe() }()
	slog.Info(fmt.Sprintf("%s started in %s on port %d", service, time.Since(started).Round(time.Millisecond), cfg.Port))
	select {
	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		return server.Shutdown(shutdownCtx)
	case err := <-errCh:
		if err == http.ErrServerClosed {
			return nil
		}
		return err
	}
}
