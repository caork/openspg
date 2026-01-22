# Module: reasoner/runner/runner-common

## Purpose

Provides the runtime pipeline and shared execution utilities for RDG-based reasoning.

## Main components

- `KGReasonerSession`: end-to-end execution pipeline.
- `GraphState`: storage abstraction for graph data.
- `rdg/common/*`: shared RDG operations (join, aggregate, add or drop fields).
- `utils/*` and `recorder/*`: execution helpers and tracing.

Other key packages:

- `graphstate/*`: memory and RocksDB implementations.
- `loader/*`: graph loader helpers and start-id tracking.
- `progress/*`: progress reporting hooks for long-running jobs.

## How it is used

- `KGReasonerSession` orchestrates parsing, planning, and execution.
- `GraphState` is used by `LocalRDG` to access graph data.
- Physical operators are executed to produce `Row` or `RDG` results.

