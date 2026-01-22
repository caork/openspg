# Module: reasoner/lube-physical

## Purpose

Plans logical operators into physical operators and defines the RDG execution interface.

## Main components

- `PhysicalPlanner`: converts logical operators to physical operators.
- `operators/*`: physical operator definitions (PatternScan, Filter, Join, etc).
- `rdg/RDG.scala`: RDG interface.
- `rdg/Row.scala`: row output for tabular results.
- `PropertyGraph`: engine-specific RDG construction.

Physical operators include `PatternScan`, `ExpandInto`, `LinkedExpand`, `Filter`, `Join`, `Aggregate`, `OrderBy`, `Fold`, `Unfold`, `DDL`, and `Union`.

## Inputs and outputs

- Input: `LogicalOperator` tree.
- Output: `PhysicalOperator` tree executable on RDG.

## Integration points

- RDG implementations must satisfy the `RDG` interface.
- Physical operators map directly onto RDG methods.

