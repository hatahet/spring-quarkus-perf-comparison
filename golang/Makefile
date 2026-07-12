.PHONY: download vet test build clean

GO ?= go
GO_ENV ?=

download:
	$(GO_ENV) $(GO) mod download

vet:
	$(GO_ENV) $(GO) vet ./...

test:
	$(GO_ENV) $(GO) test -race ./...

build:
	mkdir -p target
	$(GO_ENV) CGO_ENABLED=0 $(GO) build -trimpath -o target/go-sql ./cmd/sql
	$(GO_ENV) CGO_ENABLED=0 $(GO) build -trimpath -o target/go-gorm ./cmd/gorm
	$(GO_ENV) CGO_ENABLED=0 $(GO) build -trimpath -o target/go-ent ./cmd/ent
	$(GO_ENV) CGO_ENABLED=0 $(GO) build -trimpath -o target/go-fiber ./cmd/fiber

clean:
	rm -rf target
