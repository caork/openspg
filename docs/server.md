# Server (APIs and schema services)

The Server subsystem provides the API surface and management services for OpenSPG. It typically hosts schema operations, knowledge graph services, and integration points for applications.

## Responsibilities

- Manage schema definitions and versioning.
- Serve APIs for graph operations and reasoning tasks.
- Coordinate with storage and external services.

## API surface (HTTP)

The HTTP server exposes a set of `/public/v1/*` endpoints, including:

- Schema: `/public/v1/schema/*` (alter/query schema, types, relations).
- Graph: `/public/v1/graph/*` (upsert/delete vertices/edges, PageRank, label discovery).
- Reasoner: `/public/v1/reason/*` (run KGDSL tasks, fetch reasoning schema).
- Builder: `/public/v1/builder/*` (submit/search builder jobs).
- Search/retrieval: `/public/v1/search/*`, `/public/v1/retrieval/*`, `/public/v1/searchEngine/*`.
- Project/tenant: `/public/v1/project/*`, `/public/v1/tenant/*`.
- Misc: `/public/v1/query/*`, `/public/v1/concept/*`, `/public/v1/scheduler/*`, `/public/v1/dataSource/*`.

Controllers live in `server/api/http-server/src/main/java/com/antgroup/openspg/server/api/http/server/openapi`.

## Core modules

- Schema core: type models, constraints, and semantics (`server/core/schema`).
- Reasoner integration: task orchestration (`server/core/reasoner`).
- Scheduler core: job lifecycle management (`server/core/scheduler`).
- Biz layer: managers orchestrating schema/graph/reasoner (`server/biz`).
- Infra layer: persistence and converters (`server/infra`).

## Entry point

- SOFA Boot application: `server/arks/sofaboot/Application`.

## Code location

- `server/` (module root)
