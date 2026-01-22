# Module: reasoner/kgdsl-parser

## Purpose

Parses KGDSL into the Lube IR (`Block` and expression or pattern objects).

## Main components

- `KGDSL.g4`: ANTLR grammar defining KGDSL syntax.
- `OpenSPGDslParser`: `ParserInterface` implementation.
- Pattern and expression parsers: `parser/pattern/*` and `parser/expr/*`.
- Error handling: `ErrorHandlerStrategy`.

The grammar includes `Define` blocks, `GraphStructure`, `Rule`, and `Action`, plus graph mutation actions such as `createEdgeInstance` and `createNodeInstance`.

## Inputs and outputs

- Input: KGDSL text string.
- Output: `Block` (or a list of blocks for multi-statement input).

## Integration points

- Output blocks are validated by `Validator` and planned by `LogicalPlanner`.
- If you replace KGDSL, provide another `ParserInterface` implementation.

