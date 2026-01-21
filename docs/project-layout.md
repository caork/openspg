# Project layout

Top-level directories and their roles:

- `builder/`: knowledge construction operators and pipelines.
- `cloudext/`: adapter interfaces and implementations (graph store, search, cache, object storage, compute).
- `common/`: shared utilities.
- `reasoner/`: KGDSL parser, planning layers, RDG execution, and runners.
- `reasoner-examples/`: runnable Java examples that depend on the Reasoner runtime.
- `server/`: APIs, schema services, and runtime services.
- `lib/`, `dev/`: supporting libraries and developer tooling.

Entry points for reasoning:
- KGDSL parser: `reasoner/kgdsl-parser`.
- Planning: `reasoner/lube-logical` and `reasoner/lube-physical`.
- Runtime session: `reasoner/runner/runner-common`.
- Local execution: `reasoner/runner/local-runner`.
- Examples: `reasoner-examples`.
