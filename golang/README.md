# Go implementations

This module contains four applications with the same API and PostgreSQL schema:

- `go-sql` uses `database/sql`, pgx, ordered joins, and explicit transactions.
- `go-gorm` uses GORM ordered preloads and GORM-managed transactions.
- `go-ent` uses Ent-generated models, eager-loaded edges, and mutations.
- `go-fiber` uses Fiber with the `database/sql` repository. Fiber prefork is
  selectable with `--prefork=true|false` or `FIBER_PREFORK`; it defaults off.

All expose `GET /fruits`, `GET /fruits/{name}`, `POST /fruits`, `GET /health/live`, and database-backed `GET /health/ready`. They export traces and metrics to OTLP gRPC at `localhost:4317` with service names matching their executable (`go-fiber` uses the same name in both modes).

Go 1.26.5 is recommended (the module language floor is 1.25). Build and test with:

```bash
make download
make vet test build
../scripts/stress.sh target/go-sql
../scripts/1strequest.sh "target/go-gorm" 3
../scripts/stress.sh target/go-ent
../scripts/stress.sh target/go-fiber
../scripts/1strequest.sh "target/go-fiber --prefork=true" 3
```

The static Linux artifacts are `target/go-sql`, `target/go-gorm`, `target/go-ent`,
and `target/go-fiber`. `DATABASE_URL`, `PORT`, `OTEL_EXPORTER_OTLP_ENDPOINT`, and
`FIBER_PREFORK` can override their defaults. The applications never create or migrate the schema.

Prefork starts one Fiber worker per `GOMAXPROCS`. The configured 50-connection
budget is divided across workers so enabling prefork does not multiply the
application's total database connection budget. Each child is reduced to one
Go scheduler thread and receives an equal share of `GOMEMLIMIT`, avoiding CPU
oversubscription and multiplication of the configured memory budget.
