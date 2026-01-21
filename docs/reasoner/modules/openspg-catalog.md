# Module: reasoner/catalog/openspg-catalog

## Purpose

Provides a `Catalog` implementation backed by the OpenSPG server schema APIs.

## Main components

- `OpenSPGCatalog`: loads project schema via HTTP and builds `SemanticPropertyGraph`.
- Uses `server/api` client facades for schema and concept retrieval.

## When to include

Include this module if your SDK or service should fetch schema from an OpenSPG server instead of using an in-memory schema.
