# Project overview

OpenSPG is a knowledge graph engine based on the SPG (Semantic-enhanced Programmable Graph) framework. It combines semantic modeling, knowledge construction, and logical reasoning into a programmable graph system.

## Core subsystems

- Schema (SPG-Schema): semantic modeling of entities, predicates, and relations.
- Builder (SPG-Builder): ingestion and construction pipeline for structured and unstructured data.
- Reasoner (SPG-Reasoner): KGDSL parsing, planning, and execution on RDG.
- KNext: programmable framework for composing graph logic and operators.
- Cloudext: adapters for storage, compute, cache, search, and object storage.

## Typical flow

1. Define schema for your domain.
2. Build knowledge from data (Builder).
3. Run reasoning and query logic (Reasoner).
4. Integrate with external infrastructure (Cloudext).

## Where to go next

- For code structure: [Project layout](project-layout.md).
- For reasoning details: [Reasoner overview](reasoner/index.md).

