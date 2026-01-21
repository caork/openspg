# Module: reasoner/warehouse

## Purpose

Provides warehouse integration and graph loading utilities used by Reasoner.

## Key responsibilities

- Graph loader configuration (`GraphLoaderConfig`).
- Integration points for external storage and compute.

## Key concepts

- `GraphLoaderConfig`: vertex/edge loader configs, edge truncation thresholds, and version config.
- `VertexLoaderConfig` / `EdgeLoaderConfig`: per-type loading rules and property filters.
- `StartVertexConfig`: start-id strategy for traversals.
- `GraphVersionConfig`: multi-version graph settings.

## Cloudext bridge

For graph-store backed execution, `CloudExtGraphState` lives in `reasoner/warehouse/cloudext-warehouse` and queries graph stores through Cloudext adapters.

## When to include

Include this module if you need to load graph data from external warehouses or storage systems.
