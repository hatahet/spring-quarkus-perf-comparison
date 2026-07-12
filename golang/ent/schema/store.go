package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
)

type Store struct{ ent.Schema }

func (Store) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").Unique().Immutable(),
		field.String("name").NotEmpty().Unique(),
		field.String("currency").NotEmpty(),
		field.String("address").NotEmpty(),
		field.String("city").NotEmpty(),
		field.String("country").NotEmpty(),
	}
}

func (Store) Edges() []ent.Edge {
	return []ent.Edge{edge.To("fruit_prices", StoreFruitPrice.Type)}
}

func (Store) Annotations() []schema.Annotation {
	return []schema.Annotation{entsql.Annotation{Table: "stores"}}
}
