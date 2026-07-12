package httpapi

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"strings"
	"time"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/repository"
	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
)

type API struct {
	Repo             repository.Fruits
	DB               *sql.DB
	ReadinessTimeout time.Duration
}

func (a API) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /fruits", a.list)
	mux.HandleFunc("GET /fruits/{name}", a.get)
	mux.HandleFunc("POST /fruits", a.create)
	mux.HandleFunc("GET /health/live", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
	})
	mux.HandleFunc("GET /health/ready", a.ready)
	return otelhttp.NewHandler(mux, "http.server")
}

func (a API) list(w http.ResponseWriter, r *http.Request) {
	fruits, err := a.Repo.FindAll(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, fruits)
}

func (a API) get(w http.ResponseWriter, r *http.Request) {
	name := r.PathValue("name")
	if strings.TrimSpace(name) == "" {
		http.Error(w, "name is required", http.StatusBadRequest)
		return
	}
	fruit, err := a.Repo.FindByName(r.Context(), name)
	if err != nil {
		serverError(w, err)
		return
	}
	if fruit == nil {
		http.NotFound(w, r)
		return
	}
	writeJSON(w, http.StatusOK, fruit)
}

func (a API) create(w http.ResponseWriter, r *http.Request) {
	var fruit domain.Fruit
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&fruit); err != nil {
		http.Error(w, "invalid request", http.StatusBadRequest)
		return
	}
	if strings.TrimSpace(fruit.Name) == "" {
		http.Error(w, "Name is mandatory", http.StatusBadRequest)
		return
	}
	id, err := a.Repo.Create(r.Context(), fruit)
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, id)
}

func (a API) ready(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), a.ReadinessTimeout)
	defer cancel()
	if err := a.DB.PingContext(ctx); err != nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "DOWN"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(value); err != nil {
		slog.Error("write response", "error", err)
	}
}

func serverError(w http.ResponseWriter, err error) {
	if !errors.Is(err, context.Canceled) {
		slog.Error("request failed", "error", err)
	}
	http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
}
