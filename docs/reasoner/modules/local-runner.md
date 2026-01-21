# Module: reasoner/runner/local-runner

## Purpose

Implements an in-process RDG engine and a local runner for KGDSL execution.

## Main components

- `LocalRDG`: RDG implementation backed by `GraphState`.
- `LocalPropertyGraph`: builds `LocalRDG` instances.
- `LocalReasonerSession`: `KGReasonerSession` implementation for local execution.
- `LocalReasonerRunner`: convenience wrapper for running KGDSL tasks.

## Task wiring

`LocalReasonerTask` allows you to provide:

- KGDSL text or pre-planned physical operators.
- A `Catalog` or schema connection info.
- A graph loader class, a `GraphState` instance, or a graph state class name.
- Runtime parameters via `ConfigKey`.

## Usage scenarios

- Local testing of KGDSL.
- Embedding reasoning in a single JVM process.
- Reference implementation for custom RDG engines.

## Example

See `docs/examples/README.md` and `reasoner-examples/src/main/java/com/antgroup/openspg/examples/RcaLocalRunnerExample.java`.
