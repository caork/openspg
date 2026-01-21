# Module: reasoner/udf

## Purpose

Provides user-defined function (UDF), aggregate (UDAF), and table function (UDTF) integration for KGDSL expressions.

## Main components

- `UdfMngImpl`: manages UDF/UDAF/UDTF package paths and loading.
- `RuleRunner`: executes rule expressions with UDF support.

## When to include

Include this module if your KGDSL relies on custom functions or aggregates.
