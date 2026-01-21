# Module: reasoner/udf

## Purpose

Provides user-defined function (UDF), aggregate (UDAF), and table function (UDTF) integration for KGDSL expressions.

## Main components

- `UdfMngImpl`: manages UDF/UDAF/UDTF package paths and loading.
- `RuleRunner`: executes rule expressions with UDF support.

## Built-in functions

The module ships with built-in UDFs and UDAFs under `reasoner/udf/builtin`, including:

- Aggregations: `Count`, `Sum`, `Avg`, `Min`, `Max`, `CountDistinct`.
- String/time helpers: `Concat`, `Lower`, `Upper`, `DateFormat`, `UnixTimestamp`.
- Graph helpers: `GraphItemExists`, `RepeatEdgeLength`, `KeepShortestPath`.
- Geo helpers: `GeoDistance`, `GeoWithin`, `GeoIntersects`.

## When to include

Include this module if your KGDSL relies on custom functions or aggregates.
