package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
)

type Fruit struct{ ent.Schema }

func (Fruit) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").Unique().Immutable(),
		field.String("name").NotEmpty().Unique(),
		field.String("description").Optional().Nillable(),
	}
}

func (Fruit) Annotations() []schema.Annotation {
	return []schema.Annotation{entsql.Annotation{Table: "fruits"}}
}
