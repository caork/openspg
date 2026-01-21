# Reasoner pipeline

This section explains how KGDSL is processed from text to execution.

## Step 1: Parse KGDSL

- Entry class: `OpenSPGDslParser` in `reasoner/kgdsl-parser`.
- Output: `Block` (unresolved logical plan).
- Grammar: `reasoner/kgdsl-parser/src/main/antlr4/com/antgroup/openspg/reasoner/KGDSL.g4`.

## Step 2: Validate and bind schema

- Validation: `Validator` in `reasoner/lube-logical`.
- Schema access: `Catalog` and `SemanticPropertyGraph` in `reasoner/lube-api`.
- Result: a validated DAG of `Block` nodes with resolved types and fields.

## Step 3: Logical planning

- Planner: `LogicalPlanner` in `reasoner/lube-logical`.
- Output: `LogicalOperator` tree (plan for query semantics).
- Optimizer: `LogicalOptimizer` (rule rewrites and simplifications).

## Step 4: Physical planning

- Planner: `PhysicalPlanner` in `reasoner/lube-physical`.
- Output: `PhysicalOperator` tree targeting RDG operations.

## Step 5: Graph loading

- `KGReasonerSession` maintains a `GraphSession` and a default graph name.
- If the graph is not loaded, `LoaderUtil.getLoaderConfig` builds a loader config
  from the optimized logical plan, and `loadGraph` is invoked.
- The loaded graph is registered into the runtime session.

## Step 6: Execution

- Physical operators execute against a concrete RDG implementation.
- Results are returned as `Row` (tabular) or `RDG` (graph).

## Control flow (simplified)

```
KGReasonerSession.getResult(query)
  -> ParserInterface.parseMultipleStatement
  -> Validator.validate
  -> LogicalPlanner.plan
  -> LogicalOptimizer.optimize
  -> PhysicalPlanner.plan
  -> loadGraph (if needed)
  -> PhysicalOperator.rdg or .row
```

## Key extension points

- Replace `ParserInterface` to support another DSL.
- Replace `Catalog` to bind schema from a different source.
- Replace RDG implementation for a new execution engine.

