package httpapi

import (
	"context"
	"database/sql"
	"net/http"
	"net/http/httptest"
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
func (f *fakeRepo) Create(_ context.Context, fruit domain.Fruit) (int64, error) {
	f.created++
	return 11, nil
}

func TestFruitContract(t *testing.T) {
	description := "Hearty fruit"
	id := int64(1)
	repo := &fakeRepo{fruits: []domain.Fruit{{ID: &id, Name: "Apple", Description: &description, StorePrices: []domain.StorePrice{}}}}
	handler := API{Repo: repo, DB: &sql.DB{}}.Handler()

	tests := []struct {
		method, path, body string
		status             int
		contains           string
	}{
		{http.MethodGet, "/fruits", "", 200, `"storePrices":[]`},
		{http.MethodGet, "/fruits/Apple", "", 200, `"name":"Apple"`},
		{http.MethodGet, "/fruits/apple", "", 404, "404 page not found"},
		{http.MethodPost, "/fruits", `{}`, 400, "Name is mandatory"},
		{http.MethodPost, "/fruits", `{"name":"   "}`, 400, "Name is mandatory"},
		{http.MethodPost, "/fruits", `{"name":"Pomelo","description":null}`, 200, "11"},
	}
	for _, tc := range tests {
		t.Run(tc.method+tc.path+tc.body, func(t *testing.T) {
			req := httptest.NewRequest(tc.method, tc.path, strings.NewReader(tc.body))
			response := httptest.NewRecorder()
			handler.ServeHTTP(response, req)
			if response.Code != tc.status {
				t.Fatalf("status = %d, want %d: %s", response.Code, tc.status, response.Body.String())
			}
			if !strings.Contains(response.Body.String(), tc.contains) {
				t.Fatalf("body %q does not contain %q", response.Body.String(), tc.contains)
			}
		})
	}
	if repo.created != 1 {
		t.Fatalf("created = %d, want 1", repo.created)
	}
}

func TestLivenessDoesNotUseDatabase(t *testing.T) {
	handler := API{Repo: &fakeRepo{}, DB: &sql.DB{}}.Handler()
	response := httptest.NewRecorder()
	handler.ServeHTTP(response, httptest.NewRequest(http.MethodGet, "/health/live", nil))
	if response.Code != http.StatusOK {
		t.Fatalf("status = %d", response.Code)
	}
}
