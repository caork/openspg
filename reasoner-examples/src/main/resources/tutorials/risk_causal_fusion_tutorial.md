# Risk + Causal Fusion Tutorial (Comprehensive KGDSL)

This tutorial is a single, end-to-end example that exercises a large portion of KGDSL and showcases two
of the most common OpenSPG scenarios from the whitepaper: **risk mining knowledge graphs** and
**enterprise causal knowledge graphs**. It fuses them into one runnable example.

## Scenario story

A payment platform is dealing with suspicious transactions and merchant refund spikes. Analysts need a
fast way to:

1) Classify **fraudster persons** based on devices, risky apps, and blacklisted merchants.
2) Detect **device rings** and materialize a risk signal node for evidence.
3) Link **operational events** (logistics strikes) to downstream merchant impact events.
4) Explore the **transfer network** from seed accounts with optional context (devices, IPs, merchants,
events, and derived signals).

This example mirrors the whitepaper’s two typical cases: risk mining graphs (users/devices/transactions) and
enterprise causal graphs (event chains like "logistics strike" leading to "refund spike").

## Files used

- DSL: `reasoner-examples/src/main/resources/kgdsl/risk_causal_fusion.kgdsl`
- Runner: `reasoner-examples/src/main/java/com/antgroup/openspg/examples/RiskCausalFusionLocalRunnerExample.java`
- GraphML data: `reasoner-examples/src/main/resources/graphml/risk_causal_fusion.graphml`

## How to run

From the repo root:

```bash
mvn -pl reasoner-examples -am exec:java \
  -Dexec.mainClass=com.antgroup.openspg.examples.RiskCausalFusionLocalRunnerExample
```

You should see a `LocalReasonerResult` with rows that include:
- Seed account and peer account from the transfer expansion.
- Owner/device/app context.
- Fraudster tag, device-ring signal, merchant events, and causal event links.
- The traversal `__path__`.

## Graph model (mini schema)

Nodes:

- `Person`, `Account`, `Device`, `App`, `Ip`, `Transaction`, `Merchant`, `City`, `Company`, `Event`
- `RiskSignal` (derived)
- `TaxonomyOfRiskUser` concept instance `Fraudster`

Edges:

- `owns` (Person -> Account)
- `usesDevice` (Person -> Device)
- `installed` (Device -> App)
- `bindIp` (Device -> Ip)
- `initiated` (Account -> Transaction)
- `toMerchant` (Transaction -> Merchant)
- `locatedIn` (Merchant -> City)
- `operatedBy` (Merchant -> Company)
- `hasEvent` (Company/Merchant -> Event)
- `transfer` (Account -> Account)
- `belongTo` (Person -> TaxonomyOfRiskUser/Fraudster) — inferred
- `evidenceOf` (Device -> RiskSignal) — inferred
- `leadTo` (Event -> Event) — inferred

## How the Java example wires it together

The runner in `RiskCausalFusionLocalRunnerExample` does four things:

1) **Load the KGDSL**
It reads `risk_causal_fusion.kgdsl` from resources.

2) **Infer and register the schema**
It calls `GraphMLLocalGraphLoader.buildCatalog(..., dsl)` to infer node/edge properties from the
GraphML file and add edge types declared in `Define` blocks.

3) **Seed the query**
It sets `startIdList` to two `Account` nodes so the exploration starts from those seeds.

4) **Load GraphML data**
`RiskCausalFusionGraphMLLoader` loads `risk_causal_fusion.graphml` into the in-memory graph.

The example also keeps `spg.reasoner.lube.subquery.enable=false` so the `Define` blocks run in order and
materialize derived edges before the final exploration query.

## What the KGDSL does

The DSL file contains three `Define` blocks and one exploration query.

### 1) Fraudster classification (risk mining)

```kgdsl
Define (p:Person)-[b:belongTo]->(o:TaxonomyOfRiskUser/`Fraudster`) {
  ...
}
```

Highlights:
- Uses line-style `GraphStructure`.
- Aggregates with `group(...)` and conditional `if(...)`.
- Uses `like`, `in`, `rule_value`, and arithmetic.
- Materializes `belongTo` edges to the `Fraudster` concept.

### 2) Device ring signal (create node + edge)

```kgdsl
Define (d:Device)-[e:evidenceOf]->(sig:RiskSignal) {
  ...
  Action {
    sig = createNodeInstance(...)
    createEdgeInstance(...)
  }
}
```

Highlights:
- Demonstrates `createNodeInstance` and edge materialization.
- Computes `personCnt` to score the signal.
- Uses an `__optional__` edge in the `GraphStructure` to bind the `sig` alias even though the edge
does not exist yet (it is created by the action).

### 3) Event causality (enterprise causal graph)

```kgdsl
Define (e1:Event)-[l:leadTo]->(e2:Event) {
  GraphStructure {
    m [Merchant]
    c [Company]
    e1 [Event]
    e2 [Event]
    m->c [operatedBy, __optional__='true'] as opEdge
    m->c [suppliedBy, __optional__='true'] as supEdge
    c->e1 [hasEvent]
    m->e2 [hasEvent, __optional__='true'] as impactEdge
  }
  ...
}
```

Highlights:
- Uses line syntax plus `__optional__` edges to bind optional paths.
- Materializes causal `leadTo` edges between events.

### 4) Exploration query

The last block explores transfer paths and joins optional context:

- `repeat(1,3)` for transfer expansion.
- `keep_shortest_path` to prune paths.
- `transferPath.edges().constraint(...)` for path-level filtering.
- `exist(...)` and `rule_value(...)` for computed fields.

## Try variations

1) Raise the fraud threshold by changing `blackTxnAmt >= 800` to `>= 1500`.
2) Make `APP-2` risky in GraphML and observe how device ring signals change.
3) Add a new `transfer` edge and see how `keep_shortest_path` alters `__path__`.
