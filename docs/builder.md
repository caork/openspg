# Builder (knowledge construction)

The Builder subsystem focuses on converting data into knowledge graph structures. It provides operator frameworks that transform structured or unstructured data into SPG-compliant graph data, and supports iterative updates as data evolves.

## Responsibilities

- Ingest structured and unstructured data.
- Normalize entities and attributes using configurable operators.
- Produce graph-aligned outputs compatible with Reasoner and storage backends.

## Where it fits

Builder typically runs before Reasoner. It creates or enriches graph data that Reasoner later queries and infers over.

## Code location

- `builder/` (module root)
- `builder/runner` (runtime execution for construction pipelines)

