package fiberapi

import (
	"context"
	"database/sql"
	"io"
	"net/http"
	"strings"
	"testing"

	"github.com/quarkusio/spring-quarkus-perf-comparison/golang/internal/domain"
)

type fakeRepo struct {
	fruits  []domain.Fruit
	created int
}

func (f *fakeRepo) FindAll(context.Context) ([]domain.Fruit, error) { return f.fruits, nil }
func (f *fakeRepo) FindByName(_ context.Context, name string) (*domain.Fruit, error) {
	for i := range f.fruits {
		if f.fruits[i].Name == name {
			return &f.fruits[i], nil
		}
	}
	return nil, nil
}
func (f *fakeRepo) Create(_ context.Context, _ domain.Fruit) (int64, error) {
	f.created++
	return 11, nil
}

func TestFruitContract(t *testing.T) {
	description := "Hearty fruit"
	id := int64(1)
	repo := &fakeRepo{fruits: []domain.Fruit{{ID: &id, Name: "Apple", Description: &description, StorePrices: []domain.StorePrice{}}}}
	app := API{Repo: repo, DB: &sql.DB{}}.App(false)
	tests := []struct {
		method, path, body string
		status             int
		contains           string
	}{
		{http.MethodGet, "/fruits", "", 200, `"storePrices":[]`},
		{http.MethodGet, "/fruits/Apple", "", 200, `"name":"Apple"`},
		{http.MethodGet, "/fruits/apple", "", 404, "Not Found"},
		{http.MethodPost, "/fruits", `{}`, 400, "Name is mandatory"},
		{http.MethodPost, "/fruits", `{"name":"   "}`, 400, "Name is mandatory"},
		{http.MethodPost, "/fruits", `{"name":"Pomelo","description":null}`, 200, "11"},
	}
	for _, tc := range tests {
		t.Run(tc.method+tc.path+tc.body, func(t *testing.T) {
			req, err := http.NewRequest(tc.method, tc.path, strings.NewReader(tc.body))
			if err != nil {
				t.Fatal(err)
			}
			if tc.body != "" {
				req.Header.Set("Content-Type", "application/json")
			}
			response, err := app.Test(req)
			if err != nil {
				t.Fatal(err)
			}
			defer response.Body.Close()
			body, err := io.ReadAll(response.Body)
			if err != nil {
				t.Fatal(err)
			}
			if response.StatusCode != tc.status || !strings.Contains(string(body), tc.contains) {
				t.Fatalf("status=%d body=%q, want status=%d containing %q", response.StatusCode, body, tc.status, tc.contains)
			}
		})
	}
	if repo.created != 1 {
		t.Fatalf("created=%d, want 1", repo.created)
	}
}

func TestLiveness(t *testing.T) {
	app := API{Repo: &fakeRepo{}, DB: &sql.DB{}}.App(false)
	response, err := app.Test(newRequest(t, http.MethodGet, "/health/live", ""))
	if err != nil {
		t.Fatal(err)
	}
	if response.StatusCode != http.StatusOK {
		t.Fatalf("status=%d", response.StatusCode)
	}
}

func newRequest(t *testing.T, method, path, body string) *http.Request {
	t.Helper()
	req, err := http.NewRequest(method, path, strings.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	return req
}
