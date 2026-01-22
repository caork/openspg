# Builder (knowledge construction)

The Builder subsystem focuses on converting data into knowledge graph structures. It provides operator frameworks that transform structured or unstructured data into SPG-compliant graph data, and supports iterative updates as data evolves.

## Responsibilities

- Ingest structured and unstructured data.
- Normalize entities and attributes using configurable operators.
- Produce graph-aligned outputs compatible with Reasoner and storage backends.

## Pipeline model

Builder pipelines are a DAG of nodes and edges (`builder/model/pipeline`). Each node has a type and config, and the runner converts the DAG into a logical plan and then a physical plan.

Common node types (see `NodeTypeEnum`):

- Sources: `CSV_SOURCE`, `STRING_SOURCE`.
- Pre/processing: `PARAGRAPH_SPLIT`, `BUILDER_INDEX`, `PYTHON`.
- Extraction: `USER_DEFINED_EXTRACT`, `LLM_BASED_EXTRACT`, `LLM_NL_EXTRACT`.
- Mapping: `SPG_TYPE_MAPPINGS`, `RELATION_MAPPING`.
- Post-processing: `VECTORIZER_PROCESSOR`, `EXTRACT_POST_PROCESSOR`, `CHECK`, `REASON`.
- Sinks: `GRAPH_SINK`, `NEO4J_SINK`.

## Execution flow

1. `LogicalPlan.parse` converts the pipeline DAG into logical nodes.
2. `PhysicalPlan.plan` maps logical nodes to processors.
3. A `BuilderRunner` (local or distributed) wires source/sink implementations.
4. `BuilderExecutor` evaluates batches of `BaseRecord` through processors.

The local runner uses `SourceReaderFactory` and `SinkWriterFactory` to create concrete readers/writers, and executes on a thread pool (`LocalBuilderRunner`).

## Linking, fusing, and predicting

Builder provides strategy hooks for entity/linking/fusing/predicting:

- Linking: `RecordLinking` / `PropertyLinking` strategies.
- Fusing: `EntityFusing` / `SubjectFusing` strategies.
- Predicting: `RecordPredicting` / `PropertyPredicting` strategies.

These are configured through pipeline configs (see `builder/model/pipeline/config/*`).

## Reasoning during build

When `enableLeadTo` is true in `BuilderContext`, a `ReasonProcessor` runs after writing to graph storage. It uses concept semantics (`DynamicTaxonomySemantic`, `TripleSemantic`) to perform inductive and causal reasoning and writes derived records back to the graph store.

## Where it fits

Builder typically runs before Reasoner. It creates or enriches graph data that Reasoner later queries and infers over.

## Code location

- `builder/model` (pipeline config and record model)
- `builder/core` (logical/physical plans, processors, strategies)
- `builder/runner` (execution engines and local CLI)

