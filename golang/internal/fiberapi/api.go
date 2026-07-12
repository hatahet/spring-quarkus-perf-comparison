package fiberapi

import (
	"context"
	"database/sql"
	"errors"
	"log/slog"
	"strings"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/repository"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/codes"
	"go.opentelemetry.io/otel/trace"
)

type API struct {
	Repo             repository.Fruits
	DB               *sql.DB
	ReadinessTimeout time.Duration
}

func (a API) App(prefork bool) *fiber.App {
	app := fiber.New(fiber.Config{
		AppName:               "go-fiber",
		Prefork:               prefork,
		DisableStartupMessage: true,
		ReadTimeout:           15 * time.Second,
		WriteTimeout:          30 * time.Second,
		IdleTimeout:           60 * time.Second,
		ErrorHandler: func(c *fiber.Ctx, err error) error {
			code := fiber.StatusInternalServerError
			if fiberErr, ok := err.(*fiber.Error); ok {
				code = fiberErr.Code
			}
			if code >= 500 && !errors.Is(err, context.Canceled) {
				slog.Error("request failed", "error", err)
			}
			return c.Status(code).SendString(err.Error())
		},
	})
	app.Use(tracing)
	app.Get("/fruits", a.list)
	app.Get("/fruits/:name", a.get)
	app.Post("/fruits", a.create)
	app.Get("/health/live", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{"status": "UP"})
	})
	app.Get("/health/ready", a.ready)
	return app
}

func tracing(c *fiber.Ctx) error {
	ctx, span := otel.Tracer("go-fiber/http").Start(c.UserContext(), string(c.Method())+" "+c.Path(),
		trace.WithSpanKind(trace.SpanKindServer))
	c.SetUserContext(ctx)
	err := c.Next()
	if err != nil {
		span.RecordError(err)
		span.SetStatus(codes.Error, err.Error())
	}
	span.End()
	return err
}

func (a API) list(c *fiber.Ctx) error {
	fruits, err := a.Repo.FindAll(c.UserContext())
	if err != nil {
		return err
	}
	return c.JSON(fruits)
}

func (a API) get(c *fiber.Ctx) error {
	name := c.Params("name")
	if strings.TrimSpace(name) == "" {
		return fiber.NewError(fiber.StatusBadRequest, "Name is mandatory")
	}
	fruit, err := a.Repo.FindByName(c.UserContext(), name)
	if err != nil {
		return err
	}
	if fruit == nil {
		return fiber.ErrNotFound
	}
	return c.JSON(fruit)
}

func (a API) create(c *fiber.Ctx) error {
	var fruit domain.Fruit
	if err := c.BodyParser(&fruit); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request")
	}
	if strings.TrimSpace(fruit.Name) == "" {
		return fiber.NewError(fiber.StatusBadRequest, "Name is mandatory")
	}
	id, err := a.Repo.Create(c.UserContext(), fruit)
	if err != nil {
		return err
	}
	return c.JSON(id)
}

func (a API) ready(c *fiber.Ctx) error {
	ctx, cancel := context.WithTimeout(c.UserContext(), a.ReadinessTimeout)
	defer cancel()
	if err := a.DB.PingContext(ctx); err != nil {
		return c.Status(fiber.StatusServiceUnavailable).JSON(fiber.Map{"status": "DOWN"})
	}
	return c.JSON(fiber.Map{"status": "UP"})
}
