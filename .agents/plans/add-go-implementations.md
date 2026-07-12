# Add Go SQL and GORM Implementations

## Summary

Add one shared Go module under `golang/` containing two independently runnable applications:

- `go-sql`: standard `database/sql`, using pgx’s PostgreSQL driver.
- `go-gorm`: GORM over the same instrumented `database/sql` connection pool.

Both implementations will expose the existing fruit API, PostgreSQL health checks, and OpenTelemetry instrumentation. They will participate fully in CI, smoke tests, and performance-lab automation.

Use Go 1.26.5 by default, with a benchmark `--go-version` override. GORM is the closest mainstream Go analogue to Hibernate and EF Core; Ent and Bun are excluded because Ent adds code-generation machinery while Bun is intentionally closer to hand-written SQL. Relevant upstream projects are [Go releases](https://go.dev/doc/devel/release), [pgx](https://github.com/jackc/pgx), [GORM](https://gorm.io/docs/), and [OpenTelemetry SQL instrumentation](https://github.com/XSAM/otelsql).

## Implementation Changes

### Shared application behavior

- Create `cmd/sql` and `cmd/gorm`, with shared internal packages for configuration, DTOs, validation, HTTP handlers, repository interfaces, health, telemetry, and graceful lifecycle management.
- Preserve the existing API contract:
  - Ordered `GET /fruits` results with nested, ordered store prices.
  - Case-sensitive `GET /fruits/{name}`, returning 404 when absent.
  - `POST /fruits` returns 200 and a sequence-generated ID.
  - Missing or blank names return 400 without inserting.
  - Preserve existing JSON names, nesting, nullable descriptions, price representation, and non-null `storePrices` arrays.
- Serve on port 8080 by default, with `PORT` and `DATABASE_URL` overrides. Use the existing fruits PostgreSQL URL and credentials when unset.
- Use the external schema and seed script exclusively; neither application creates or migrates the database.
- Configure the tuned branch with 50 maximum open and idle connections. The eventual `ootb` sync removes explicit pool tuning.
- Add:
  - `/health/live`, independent of PostgreSQL.
  - `/health/ready`, backed by a bounded `PingContext`, returning 503 when unavailable.
- Use `log/slog`, standard server timeouts, SIGINT/SIGTERM shutdown, and deterministic startup messages:
  - `go-sql started in … on port 8080`
  - `go-gorm started in … on port 8080`

### Persistence implementations

- `go-sql`:
  - Register pgx through `database/sql` and wrap it with `otelsql`.
  - Use ordered `LEFT JOIN` queries for list and name lookup.
  - Group nullable joined rows into the shared nested DTO graph, including fruits without prices.
  - Use PostgreSQL `$1` parameters and exact name comparison.
  - Create fruits inside `sql.Tx` with `nextval('fruits_seq')` and `INSERT … RETURNING`, committing or rolling back explicitly.

- `go-gorm`:
  - Define GORM models and table/column tags for `fruits`, `stores`, and `store_fruit_prices`, including the composite price key and embedded address columns.
  - Open GORM over the same instrumented `*sql.DB`; do not call `AutoMigrate`.
  - Use idiomatic ordered `Preload` operations for the price and store graph, mapping models into shared DTOs before returning.
  - Use `Where("name = ?", name)` for case-sensitive lookup.
  - Perform creation in a GORM transaction: obtain the next sequence value, assign it to the model, and call `Create`.
  - Retain GORM’s normal transaction and query behavior; do not enable prepared-statement caching or flatten reads into hand-written joins.

### Telemetry and builds

- Use OpenTelemetry SDK/exporters 1.44.x, `otelhttp` and runtime instrumentation 0.69.x, and `otelsql` 0.42.x.
- Export OTLP traces and metrics over gRPC, defaulting to `localhost:4317`; use W3C propagation and parent-based 10% trace sampling.
- Instrument HTTP requests, SQL calls, pool statistics, and Go runtime metrics. Use distinct service names `go-sql` and `go-gorm`.
- Pin pgx v5.10.x, GORM v1.31.x, and its PostgreSQL driver v1.6.x in `go.mod`/`go.sum`.
- Set the module language floor to Go 1.25 while recommending Go 1.26.5 through the toolchain directive. Benchmark builds force the selected local toolchain so `--go-version` is authoritative.
- Add Make targets for dependency download, vet, race-enabled tests, and static Linux builds. Produce:
  - `golang/target/go-sql`
  - `golang/target/go-gorm`
- Build with `CGO_ENABLED=0` and `-trimpath`, retaining symbols for profiling.

## Public Interfaces and Automation

| Interface | Addition |
|---|---|
| Source module | `golang/` |
| Applications | `go-sql`, `go-gorm` |
| Benchmark runtimes | `go-sql`, `go-gorm` |
| Benchmark option | `--go-version <X.Y.Z>` |
| Default Go version | `1.26.5` |
| Artifacts | `golang/target/go-sql`, `golang/target/go-gorm` |

- Extend the performance lab with both runtimes in its allow-list and default runtime set.
- Add Go version/home state, exact semantic-version validation, and an `ensure-go` helper supporting Linux/macOS on amd64/arm64. Downloaded toolchains must use official archives and verified SHA-256 checksums.
- Generalize runtime definitions to carry dependency, version, build, artifact-directory, and launch commands. Preserve Maven defaults, use `dotnet restore` for .NET, and `go mod download` for Go.
- Launch cached Go binaries rather than source-tree artifacts. Give each Go runtime an isolated build cache during timed builds so building one does not artificially warm the other.
- Translate the configured JVM memory value to `GOMEMLIMIT`, defaulting to `512MiB`, and set `GOMAXPROCS` to the benchmark’s allocated application CPU count. Document that `GOMEMLIMIT` is a soft runtime limit.
- Generalize `scripts/stress.sh` and `scripts/1strequest.sh` so executables are recognized as Go rather than treated as .NET; preserve all existing uncommitted script edits.
- Add a Go CI job using Go 1.26.5 that runs vet, race tests, both builds, stress tests, and first-request smoke tests.
- Add Dependabot `gomod` entries for `/golang` on both `main` and `ootb`.
- Update the root, Go-module, and performance-lab documentation with implementation distinctions, runtime IDs, artifact paths, telemetry names, version/memory behavior, and commands.
- Do not add the Go module to the root Maven aggregator; clarify that Maven aggregation covers only Java modules.

## Test Plan

- Add shared handler/service tests covering JSON shape, ordering, validation, status codes, repository errors, and readiness-up/down behavior.
- Test `go-sql` with mocked SQL for joined-row grouping, fruits without prices, exact parameters, sequence-returned IDs, commit, and rollback.
- Test `go-gorm` mappings and generated SQL, ordered preloads, composite keys, explicit sequence use, commit, and rollback.
- Add PostgreSQL 17 integration suites using Testcontainers Go 0.43.x and the canonical `scripts/dbdata/db.sql`, resetting the database between repository suites.
- Run the same contract against both applications:
  - Ten seeded fruits and the complete ordered Apple/store graph.
  - Found, missing, and case-mismatched lookups.
  - Successful creation with ID 11 and subsequent list size 11.
  - Missing/blank name rejection without insertion.
  - Readiness up with PostgreSQL and down after database failure.
- Extend performance-lab shell tests for defaults, allow-list entries, help output, `--go-version`, minimum/exact-version validation, memory conversion, and unknown runtime rejection.
- Verify:
  - `go vet ./...`
  - `go test -race ./...`
  - Both static builds.
  - Performance-lab shell tests.
  - Both artifacts through stress and first-request scripts.
  - One startup/RSS/load performance-lab iteration for both runtimes, with no connection or timeout errors and distinct telemetry service names.

## Assumptions and Boundaries

- The shared Go code is limited to the two Go applications; no application code is shared with Java or .NET.
- Standard `net/http` is used for both variants so persistence remains the primary variable.
- The SQL variant uses `database/sql`, not pgx’s native API; the ORM variant uses GORM rather than Ent, Bun, or sqlc.
- Only JVM-comparable host binaries are added: no container image, alternate web framework, native variant, migration tool, or profiler endpoint.
- Current dirty-worktree changes, including the Helidon work and existing stress-script edits, remain user-owned and must be merged around rather than overwritten.
