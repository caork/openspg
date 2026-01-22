# OpenSPG Documentation

## Start here

- If you are new: read the overview and project layout first.
- If you are focused on reasoning: jump to the Reasoner section (KGDSL and RDG).
- If you want to extract an SDK: read the Reasoner SDK notes and module map.

## Layered architecture (whitepaper -> code)

- **SPG-Schema**: types, predicates, constraints, and semantics (`server/core/schema`).
- **SPG-Engine**: KGDSL parsing + logical/physical planning + RDG execution (`reasoner/*`).
- **SPG-Controller**: APIs and orchestration for schema, builder, and reasoning (`server/api`, `server/biz`).
- **SPG-Programming**: KGDSL rules + Builder pipelines as programmable knowledge operators.
- **SPG-LLM**: LLM-based extraction nodes in Builder (`builder/core/logical/*LLM*`).
- **Cloud adaptation**: storage/search/cache/object storage adapters (`cloudext/*`).

This repo focuses on the engine, schema, and programmable construction layers, and exposes them through the server and adapter modules.

## Data flow (high level)

1. Schema definitions are authored and stored via server APIs.
2. Builder pipelines read sources, extract/normalize, map to SPG records, and write to storage.
3. Reasoner loads graph data, parses KGDSL, plans, and executes RDG operations.
4. Cloudext adapters translate SPG schema/records into backend-specific representations.

## How KGDSL runs (DSL -> execution)

This section is an engineer-focused walkthrough of what happens when you submit a KGDSL string.

### Pipeline overview

At runtime, KGDSL goes through the classic compiler pipeline:

1. **Parse**: text → AST/IR (`Block`).
2. **Bind + validate**: resolve labels/fields against schema (`Catalog`).
3. **Logical plan**: build semantic operators (`LogicalOperator`) + optimize.
4. **Physical plan**: compile to executable operators (`PhysicalOperator`) targeting RDG.
5. **Load graph**: ensure required vertex/edge types are available in `GraphState`.
6. **Execute**: run physical operators over an RDG implementation and return results.

### Diagram (end-to-end)

Mermaid (if your Markdown renderer supports it):

```mermaid
flowchart TD
  A[KGDSL text] --> B[ParserInterface<br/>OpenSPGDslParser]
  B --> C[List[Block] IR<br/>reasoner/lube-api]
  C --> D[Validator<br/>reasoner/lube-logical]
  D --> E[LogicalPlanner -> LogicalOperator]
  E --> F[LogicalOptimizer]
  F --> G[PhysicalPlanner -> PhysicalOperator]
  G --> H[KGReasonerSession]
  H --> I[Graph loading<br/>GraphLoaderConfig/LoaderUtil]
  I --> J[GraphState<br/>Mem/RocksDB/GraphStore/CloudExt]
  H --> K[RDG execution<br/>LocalRDG or engine RDG]
  K --> L[Result<br/>Row or RDG]
```

ASCII fallback:

```
KGDSL text
  -> OpenSPGDslParser (ParserInterface)
  -> Block IR (lube-api)
  -> Validator + Catalog binding (lube-logical)
  -> LogicalOperator + optimizations (lube-logical)
  -> PhysicalOperator targeting RDG (lube-physical)
  -> ensure graph loaded (GraphLoaderConfig/GraphState)
  -> run RDG ops (LocalRDG / engine RDG)
  -> Row (tabular) or RDG (graph) result
```

### What each stage produces

- **Parser output: `Block`**
  - Defined in `reasoner/lube-api`.
  - Represents an unresolved plan-like IR produced from KGDSL grammar.
  - Entry grammar: `reasoner/kgdsl-parser/src/main/antlr4/com/antgroup/openspg/reasoner/KGDSL.g4`.
- **Logical plan: `LogicalOperator`**
  - Built by `LogicalPlanner` and rewritten by `LogicalOptimizer` in `reasoner/lube-logical`.
  - Captures “what the query means” (pattern match, expand, filter, aggregate, etc).
- **Physical plan: `PhysicalOperator[T <: RDG[T]]`**
  - Built by `PhysicalPlanner` in `reasoner/lube-physical`.
  - Captures “how to execute” on a concrete RDG engine (local or distributed).
- **Execution: `RDG` + `GraphState`**
  - `RDG` is the execution interface (`patternScan`, `expandInto`, `filter`, `join`, `groupBy`, `ddl`, ...).
  - `GraphState` is the backing graph storage and lookup abstraction.

### Where “schema binding” happens

KGDSL is schema-bound: node labels, edge labels, and property references must resolve against a `Catalog`.

- `Catalog` lives in `reasoner/lube-api` and provides schema metadata.
- Validation and binding happen in `Validator` (`reasoner/lube-logical`).
- In local mode you can use `PropertyGraphCatalog` (in-memory schema); in server mode you can use `OpenSPGCatalog` (server-backed).

### How graph loading works

Planning determines which vertex/edge types and properties are needed. The runtime then ensures those are available in a `GraphState`:

- In-memory: `MemGraphState` (fast local execution).
- Persistent local: `RocksdbGraphState` (version-aware properties).
- Graph-store backed: `GraphStoreGraphState` (via `AbstractGraphLoader`) or `CloudExtGraphState` (via Cloudext graph-store adapters).

This is why the “compile” (planning) phase and the “data” (loading/execution) phase are separated: planning is schema-driven, execution is storage-driven.

### Mental model: what actually runs

The key orchestrator is `KGReasonerSession` (`reasoner/runner/runner-common`), which wires together:

- `ParserInterface` → `Block` IR
- `Validator` / `LogicalPlanner` / `LogicalOptimizer`
- `PhysicalPlanner` → `PhysicalOperator`
- `GraphSession` + `GraphState` for data access
- `RDG` implementation to execute operators

A simplified pseudocode view:

```text
blocks = parser.parseMultipleStatement(dsl)
validated = Validator.validate(blocks, catalog)
logical = LogicalPlanner.plan(validated)
optimized = LogicalOptimizer.optimize(logical)
physical = PhysicalPlanner.plan(optimized)
ensureGraphLoaded(physical, graphState)
return execute(physical)  // Row or RDG
```

### Reading guide (start here in code)

- Grammar: `reasoner/kgdsl-parser/src/main/antlr4/com/antgroup/openspg/reasoner/KGDSL.g4`
- Parser: `reasoner/kgdsl-parser/src/main/scala/com/antgroup/openspg/reasoner/parser/OpenSPGDslParser.scala`
- IR types: `reasoner/lube-api/src/main/scala/com/antgroup/openspg/reasoner/lube/block`
- Validation/planning/optimizer: `reasoner/lube-logical/src/main/scala/com/antgroup/openspg/reasoner/lube/logical`
- Physical planning + RDG interface: `reasoner/lube-physical/src/main/scala/com/antgroup/openspg/reasoner/lube/physical`
- Runtime session: `reasoner/runner/runner-common/src/main/scala/com/antgroup/openspg/reasoner/session/KGReasonerSession.scala`
- Local runner (reference execution): `reasoner/runner/local-runner`

### Extension points (what you replace to embed this elsewhere)

- **New DSL**: implement `ParserInterface`.
- **New schema source**: implement/extend `Catalog`.
- **New engine**: implement `RDG` + `PropertyGraph`, then wire a session that uses them.

## Overview

- [Project overview](overview.md)
- [Project layout](project-layout.md)

## Reasoner (KGDSL + RDG)

- [Reasoner overview](reasoner/index.md)
- [End to end pipeline](reasoner/pipeline.md)
- [KGDSL language guide](reasoner/kgdsl.md)
- [RDG execution model](reasoner/rdg.md)
- [Runtime and sessions](reasoner/runtime.md)
- [SDK extraction guide](reasoner/sdk.md)
- [Module map](reasoner/modules.md)
- [Examples](examples/README.md)

## Other subsystems

- [Builder (knowledge construction)](builder.md)
- [Server (APIs and schema)](server.md)
- [Cloudext (adapters)](cloudext.md)

## Notes

- The root README files give a short product overview; these docs go deeper into architecture and code paths.

