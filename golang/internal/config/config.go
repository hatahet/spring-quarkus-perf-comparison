package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	Port             int
	DatabaseURL      string
	OTLPEndpoint     string
	ReadinessTimeout time.Duration
}

func Load() Config {
	return Config{
		Port:             envInt("PORT", 8080),
		DatabaseURL:      env("DATABASE_URL", "postgres://fruits:fruits@localhost:5432/fruits?sslmode=disable"),
		OTLPEndpoint:     env("OTEL_EXPORTER_OTLP_ENDPOINT", "localhost:4317"),
		ReadinessTimeout: 2 * time.Second,
	}
}

func env(name, fallback string) string {
	if value := os.Getenv(name); value != "" {
		return value
	}
	return fallback
}

func envInt(name string, fallback int) int {
	value, err := strconv.Atoi(os.Getenv(name))
	if err != nil {
		return fallback
	}
	return value
}
