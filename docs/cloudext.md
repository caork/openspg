# Cloudext (adapters)

Cloudext provides abstraction layers and implementations that integrate OpenSPG with external infrastructure.

## Responsibilities

- Storage adapters (graph stores, object storage).
- Compute adapters (distributed engines).
- Search and cache adapters.

## Interface modules

Cloudext interfaces define the contract for external systems:

- Graph store: schema + data operations (`cloudext/interface/graph-store`).
- Search engine: index schema + data operations (`cloudext/interface/search-engine`).
- Cache: `cloudext/interface/cache`.
- Object storage: `cloudext/interface/object-storage`.
- Computing engine: `cloudext/interface/computing-engine`.

There are adapter helpers that translate SPG schema/records to backend-specific models, such as:

- `SPGSchema2LPGService` and `SPGRecord2LPGService` (graph store).
- `SPGSchema2IdxService` and `SPGRecord2IdxService` (search engine).

## Built-in implementations

Provided implementations live in `cloudext/impl`:

- Graph store: Neo4j, TuGraph.
- Search engine: Elasticsearch, Neo4j.
- Cache: Redis.
- Object storage: MinIO, OSS.

## Code location

- `cloudext/` (module root)
