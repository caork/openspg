# Runtime and sessions

The runtime layer orchestrates parsing, planning, graph loading, and execution.

## Core entry point

- `KGReasonerSession` (in `reasoner/runner/runner-common`).
- Responsibilities:
  - Parse KGDSL with `ParserInterface`.
  - Plan logical and physical operators.
  - Load graph data into a `GraphSession` when needed.
  - Execute physical operators to produce results.

Key methods:
- `plan(query, params)`: returns a list of physical operators.
- `planBlock(blocks, params)`: converts blocks to optimized logical and physical plans.
- `getResult(query, params)`: executes and returns `Row` or `RDG`.

## Local execution

- `LocalReasonerSession` is a concrete `KGReasonerSession` using `LocalRDG`.
- `LocalPropertyGraph` creates `LocalRDG` instances backed by `GraphState`.
- `LocalReasonerRunner` provides a convenience runner around a `LocalReasonerTask`.

## Graph state and session

- `GraphState` provides graph storage for RDG operations.
- `GraphSession` caches loaded graphs per session.
- The default graph name is `Catalog.defaultGraphName`.

`GraphState` implementations:

- `MemGraphState`: in-memory graph for local execution.
- `RocksdbGraphState`: persistent local graph with versioned properties.
- `GraphStoreGraphState`: graph-store backed graph loading via `AbstractGraphLoader`.
- `CloudExtGraphState`: graph-store backed graph loading via Cloudext adapters.

## Parameters and runtime context

- Runtime parameters are passed into `plan` and `getResult`.
- `Constants.START_ALIAS` and `Constants.START_LABEL` influence the start node selection.
- `RunnerUtil` builds the runtime context for rule execution.

Common runtime parameters (`ConfigKey`):

- `KG_REASONER_OUTPUT_GRAPH`: return graph results instead of only rows.
- `KG_REASONER_BINARY_PROPERTY`: toggle binary property parsing.
- `KG_REASONER_MAX_PATH_LIMIT`: cap path expansion size.
- `KG_REASONER_STRICT_MAX_PATH_THRESHOLD`: strict path limit (local runner).
- `KG_REASONER_START_ID_LIST`: provide start vertices for traversal.
- `KG_REASONER_GRAPH_LOAD_CONFIG`: override graph loader configuration.

## Where to look in code

- `reasoner/runner/runner-common/src/main/scala/com/antgroup/openspg/reasoner/session/KGReasonerSession.scala`
- `reasoner/runner/local-runner/src/main/java/com/antgroup/openspg/reasoner/runner/local/impl/LocalReasonerSession.java`
- `reasoner/runner/local-runner/src/main/java/com/antgroup/openspg/reasoner/runner/local/LocalReasonerRunner.java`

