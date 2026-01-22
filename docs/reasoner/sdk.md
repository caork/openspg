# Reasoner SDK extraction guide

This guide describes how to package the KGDSL + RDG pipeline into a smaller SDK.

## Minimal module set

Required:
- `reasoner/common`
- `reasoner/lube-api`
- `reasoner/kgdsl-parser`
- `reasoner/lube-logical`
- `reasoner/lube-physical`
- `reasoner/runner/runner-common`
- `reasoner/runner/local-runner` (for local execution)

Optional:
- `reasoner/udf` (UDF and UDAF support)
- `reasoner/warehouse/*` (external loaders and configs)
- `reasoner/catalog/openspg-catalog` (server-backed catalog)

## Required runtime wiring

To run KGDSL end to end, you need:
- A `Catalog` that can provide schema information.
- A `ParserInterface` (KGDSL parser is the default).
- An RDG implementation and a `PropertyGraph` wrapper.
- A `KGReasonerSession` implementation to tie them together.

If you want to load data from external storage, include:

- `reasoner/warehouse/warehouse-common` for graph loading configs.
- `reasoner/warehouse/cloudext-warehouse` for Cloudext-backed graph state.

## Minimal runtime example

- Use `PropertyGraphCatalog` for an in-memory schema.
- Use `LocalReasonerSession` + `MemGraphState` for in-process execution.
- See `docs/examples/README.md` for a runnable example.

## Extension strategy

- Swap the parser if you need another DSL.
- Swap RDG and PropertyGraph for your engine.
- Keep the planning stack unchanged unless you change the IR.

