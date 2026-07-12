package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
)

type StoreFruitPrice struct{ ent.Schema }

func (StoreFruitPrice) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").StorageKey("fruit_id").Immutable(),
		field.Int64("store_id"),
		field.Float("price").SchemaType(map[string]string{"postgres": "numeric(12,2)"}),
	}
}

func (StoreFruitPrice) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("store", Store.Type).Ref("fruit_prices").Field("store_id").Unique().Required(),
	}
}

func (StoreFruitPrice) Annotations() []schema.Annotation {
	return []schema.Annotation{entsql.Annotation{Table: "store_fruit_prices"}}
}
