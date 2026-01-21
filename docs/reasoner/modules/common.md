# Module: reasoner/common

## Purpose

Provides foundational types, constants, graph primitives, and shared utilities used by all Reasoner layers.

## Key contents

- `constants/Constants.java`: runtime keys, special property names, and flags.
- `graph/*`: vertex, edge, property interfaces and implementations.
- `exception/*`: shared exception types (parser, runtime, unsupported operations).
- `types/*` and `utils/*`: scalar types and common helpers.

Notable graph primitives include:

- `IVertex`, `IEdge`, and `IProperty` interfaces.
- Version-aware properties (`IVersionProperty` implementations).
- ID types (`VertexBizId`, `VertexId`) used by loaders and graph state.

## Why it matters

Every higher layer uses these types to represent graph entities and to interpret runtime parameters. If you extract an SDK, this module must be included unchanged.
