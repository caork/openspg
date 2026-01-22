# Project overview

OpenSPG is a knowledge graph engine based on the SPG (Semantic-enhanced Programmable Graph) framework. It combines semantic modeling, knowledge construction, and logical reasoning into a programmable graph system.

## What SPG means in this repo

SPG is the semantic layer that sits on top of a property graph. In this codebase, the semantic layer shows up as:

- Typed schema objects and constraints (`server/core/schema/model/type`, `server/core/schema/model/constraint`).
- Predicate semantics and rule metadata (`server/core/schema/model/semantic`).
- Built-in predicate functions like `belongTo`, `leadTo`, `inverseOf` (`SystemPredicateEnum`).
- A programmable rule/query language (KGDSL) with a planning stack and RDG execution.

The whitepaper outlines a layered architecture; OpenSPG implements the Schema + Builder + Reasoner parts of that stack and connects them through APIs and adapters.

## Core subsystems

- Schema (SPG-Schema): semantic modeling of entities, events, concepts, standards, and predicates.
- Builder (SPG-Builder): pipeline framework that converts raw data into SPG records.
- Reasoner (SPG-Reasoner): KGDSL parsing, planning, and RDG execution.
- Server: APIs for schema, graph, reasoning, builder jobs, and search.
- Cloudext: adapters for graph storage, search, cache, and object storage.

## Typical flow

1. Define a schema (types, properties, relations, semantics).
2. Build knowledge from data using Builder pipelines.
3. Run KGDSL reasoning or queries on an RDG implementation.
4. Persist and serve through graph store + search adapters.

## Where to go next

- For code structure: [Project layout](project-layout.md).
- For reasoning details: [Reasoner overview](reasoner/index.md).

