# Module: reasoner (parent)

## Purpose

Aggregates all Reasoner submodules, including parsing, planning, and execution.

## Submodules

Core:
- common
- lube-api
- kgdsl-parser
- lube-logical
- lube-physical
- runner/runner-common
- runner/local-runner

Optional:
- udf
- warehouse/*
- catalog/openspg-catalog

## Build structure

- Parent POM: `reasoner/pom.xml`
- The parent defines dependency management and module order.

## SDK notes

The core submodules are enough for KGDSL parsing and local RDG execution.
