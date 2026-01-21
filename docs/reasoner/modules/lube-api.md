# Module: reasoner/lube-api

## Purpose

Defines the IR and public APIs that connect parsing, planning, and execution.

## Main concepts

- **Block IR**: unresolved logical plan nodes such as `MatchBlock`, `DDLBlock`, and `TableResultBlock`.
- **Expressions**: scalar expressions, aggregations, and rule expressions (`common/expr`).
- **Patterns**: node and edge patterns used by KGDSL (`common/pattern`).
- **Catalog**: schema lookups and graph metadata (`catalog/*`).
- **ParserInterface**: the parser contract implemented by KGDSL parser.

Catalog-related types include:

- `PropertyGraphSchema` and `SemanticPropertyGraph` for schema access.
- `ConceptGraph` for concept hierarchy and semantic traversal.
- `AbstractConnection` for remote schema sources.

## Key files

- `block/Block.scala`
- `parser/ParserInterface.scala`
- `catalog/impl/PropertyGraphCatalog.scala` (simple in-memory schema)

## How it is used

- The KGDSL parser returns `Block` objects defined here.
- The logical planner consumes these blocks and resolves them using a `Catalog`.
- The physical planner and RDG layer rely on IR structures for execution.
