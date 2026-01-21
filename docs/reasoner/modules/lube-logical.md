# Module: reasoner/lube-logical

## Purpose

Transforms unresolved `Block` IR into `LogicalOperator` trees and applies logical validation and optimization.

## Main components

- `LogicalPlanner`: builds a logical plan from `Block` IR.
- `Validator`: checks dependency and schema consistency.
- `LogicalOptimizer`: applies rule-based rewrites.
- `operators/*`: logical operator definitions.

## Inputs and outputs

- Input: `Block` plus `Catalog` and parsing context.
- Output: `LogicalOperator` list (optimized logical plan).

## Integration points

- Used by `KGReasonerSession` during `plan` and `getResult`.
- If you keep KGDSL and the IR, this module can be reused without changes.

