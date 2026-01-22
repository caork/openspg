# Reasoner overview

Reasoner is the subsystem that turns KGDSL queries and rules into executable graph operations over RDG (Resilient Distributed Graph). It is designed as a multi-stage pipeline so each layer can be swapped or extended.

## What it does

- Parses KGDSL into an intermediate representation (IR).
- Validates the IR against the graph schema.
- Plans logical operators and applies optimizations.
- Produces a physical plan that targets an RDG implementation.
- Executes on a concrete RDG engine (local or distributed).

## Key building blocks

- KGDSL grammar and parser: `reasoner/kgdsl-parser`.
- IR and API layer: `reasoner/lube-api`.
- Logical planning: `reasoner/lube-logical`.
- Physical planning and RDG API: `reasoner/lube-physical`.
- Runtime session and graph state: `reasoner/runner/runner-common`.
- Local execution: `reasoner/runner/local-runner`.

## Core concepts

- **KGDSL**: domain-specific language for graph patterns, rules, and actions.
- **Block IR**: unresolved logical blocks emitted by the parser.
- **LogicalOperator**: optimized logical plan tree.
- **PhysicalOperator**: executable plan over RDG.
- **RDG**: physical execution abstraction (pattern scan, expand, filter, join, etc).
- **GraphState**: graph storage abstraction (memory, RocksDB, or graph store backed).

## Where to go next

- [End to end pipeline](pipeline.md)
- [KGDSL language guide](kgdsl.md)
- [RDG execution model](rdg.md)
- [Runtime and sessions](runtime.md)
- [SDK extraction guide](sdk.md)

