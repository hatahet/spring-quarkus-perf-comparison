package main

import (
	"context"
	"flag"
	"log/slog"
	"os"
	"runtime"
	"runtime/debug"
	"strconv"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/application"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/config"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/database"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/sqlrepo"
)

func main() {
	started := time.Now()
	prefork := flag.Bool("prefork", envBool("FIBER_PREFORK", false), "enable Fiber prefork mode")
	flag.Parse()
	workers := runtime.GOMAXPROCS(0)
	if *prefork && fiber.IsChild() {
		runtime.GOMAXPROCS(1)
		if memoryLimit := debug.SetMemoryLimit(-1); memoryLimit > 0 && memoryLimit < 1<<62 {
			debug.SetMemoryLimit(memoryLimit / int64(workers))
		}
	}
	maxConnections := 50
	if *prefork && workers > 1 {
		maxConnections = (maxConnections + workers - 1) / workers
	}
	db, err := database.OpenWithMaxConnections(context.Background(), config.Load().DatabaseURL, maxConnections)
	if err != nil {
		slog.Error("open database", "error", err)
		os.Exit(1)
	}
	defer db.Close()
	if err := application.RunFiber(db, &sqlrepo.Repository{DB: db}, started, *prefork); err != nil {
		slog.Error("application stopped", "error", err)
		os.Exit(1)
	}
}

func envBool(name string, fallback bool) bool {
	value := os.Getenv(name)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}
