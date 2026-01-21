# RDG execution model

RDG (Resilient Distributed Graph) is the physical execution abstraction used by Reasoner. A concrete RDG implementation executes physical operators produced by the planner.

## RDG interface

The core API is defined in:
- `reasoner/lube-physical/src/main/scala/com/antgroup/openspg/reasoner/lube/physical/rdg/RDG.scala`

Key operations include:
- Pattern expansion: `patternScan`, `expandInto`, `linkedExpand`.
- Relational ops: `select`, `filter`, `orderBy`, `groupBy`, `join`, `union`.
- Schema ops: `addFields`, `dropFields`, `fold`, `unfold`.
- DDL: `ddl` (for graph mutations).
- Execution helpers: `cache`, `limit`, `show`.

## LocalRDG implementation

- `LocalRDG` is an in-process RDG backed by `GraphState`.
- It is used by `LocalReasonerSession` for local execution and tests.
- `LocalPropertyGraph` builds RDG instances from a local graph state.

Local graph state can be memory or RocksDB based, and may be populated via a graph loader class or a `GraphState` instance injected into `LocalReasonerTask`.

## How RDG relates to physical operators

Physical operators (e.g., `PatternScan`, `ExpandInto`, `LinkedExpand`, `Filter`, `Join`, `Aggregate`) map directly to RDG methods. The physical planner wires these operators into a tree that is executed by `KGReasonerSession`.

## Extension points

To integrate a new execution engine:
1. Implement `RDG` for your runtime (Spark, GeaFlow, etc).
2. Implement `PropertyGraph` to construct RDG instances.
3. Wire your RDG into a custom `KGReasonerSession` implementation.
