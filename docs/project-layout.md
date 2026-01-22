# Project layout

Top-level directories and their roles:

- `builder/`: knowledge construction pipelines, processors, and runners.
- `cloudext/`: adapter interfaces and implementations (graph store, search, cache, object storage, compute).
- `common/`: shared utilities and constants.
- `reasoner/`: KGDSL parser, planning layers, RDG execution, graph state, and runners.
- `reasoner-examples/`: runnable Java examples that depend on the Reasoner runtime.
- `server/`: APIs, schema services, graph services, and job scheduling.
- `lib/`, `dev/`: supporting libraries and developer tooling.

Builder structure:

- `builder/model`: pipeline config, record model, and enums.
- `builder/core`: logical/physical plan, processors, and strategies.
- `builder/runner`: execution engines (local runner implemented).

Cloudext structure:

- `cloudext/interface`: adapter APIs for graph store, search engine, cache, object storage, compute.
- `cloudext/impl`: concrete drivers (Neo4j, TuGraph, Elasticsearch, Redis, MinIO, OSS).

Reasoner structure:

- `reasoner/kgdsl-parser`: ANTLR grammar and parser.
- `reasoner/lube-api`: IR, expressions, patterns, and catalog APIs.
- `reasoner/lube-logical`: logical planning and optimization.
- `reasoner/lube-physical`: physical planning and RDG interface.
- `reasoner/runner`: runtime sessions and runners.
- `reasoner/warehouse`: graph loading configs and cloudext bridge.

Server structure:

- `server/api`: HTTP server/controllers and HTTP client facades.
- `server/core`: schema, reasoner, and scheduler core services.
- `server/biz`: domain managers and orchestration logic.
- `server/infra`: persistence layer and converters.
- `server/arks`: SOFA Boot entry point and server bootstrap.

Entry points for reasoning:

- KGDSL parser: `reasoner/kgdsl-parser`.
- Planning: `reasoner/lube-logical`, `reasoner/lube-physical`.
- Runtime session: `reasoner/runner/runner-common`.
- Local execution: `reasoner/runner/local-runner`.
- Examples: `reasoner-examples`.

Entry points for builder:

- Local CLI runner: `builder/runner/local/LocalBuilderMain`.
- Local execution engine: `builder/runner/local/LocalBuilderRunner`.

