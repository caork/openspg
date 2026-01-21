# Graph Exploration Tutorial (Seed Accounts)

This tutorial walks through a richer OpenSPG Reasoner example that starts from a few seed accounts and explores the wider graph by following transfers and optionally pulling related context (devices, IPs, transactions, merchants, and cities).

## Scenario story

You are investigating potential fraud rings in a payment network. A small set of accounts has already been flagged as high risk, and you want to quickly see:

- Which other accounts are connected by chains of transfers.
- Whether those connected accounts share devices or IPs.
- Where the money was spent (merchants and cities).

The goal is not to compute a final score, but to **expand the context** around the risky seeds and return a compact path-based view for analysts.

## Files used

- DSL: `reasoner-examples/src/main/resources/kgdsl/graph_exploration.kgdsl`
- Runner: `reasoner-examples/src/main/java/com/antgroup/openspg/examples/GraphExplorationLocalRunnerExample.java`

## How to run

From the repo root:

```bash
mvn -pl reasoner-examples -am exec:java \
  -Dexec.mainClass=com.antgroup.openspg.examples.GraphExplorationLocalRunnerExample
```

You should see a `LocalReasonerResult` printed with rows that include the seed account, peer account, optional context columns, and the full traversal `__path__`.

## Data model (mini schema)

Nodes:

- `Account` (id, name, riskLevel)
- `Person` (id, name)
- `Device` (id, type)
- `Ip` (id, ip)
- `Transaction` (id, amount, channel)
- `Merchant` (id, name, category)
- `City` (id, name)

Edges:

- `ownedBy` (Account -> Person)
- `loginWith` (Account -> Device)
- `bindIp` (Device -> Ip)
- `transfer` (Account -> Account)
- `makeTxn` (Account -> Transaction)
- `toMerchant` (Transaction -> Merchant)
- `locatedIn` (Merchant -> City)

The Java loader creates a small in-memory graph that represents these relationships.

## What the KGDSL does

The DSL is designed to explore the graph starting from `__start__` accounts:

```kgdsl
GraphStructure {
  Seed [Account, __start__='true']
  Peer [Account]
  ...
  Seed->Peer [transfer] repeat(1,3) as transferPath
  Seed->Owner [ownedBy]
  Seed->SeedDevice [loginWith, __optional__='true'] as seedDevice
  Peer->PeerDevice [loginWith, __optional__='true'] as peerDevice
  ...
}
Rule {
  R1: Seed.riskLevel == 'HIGH'
  R2: group(Seed).keep_shortest_path(transferPath)
}
Action {
  get(Seed.id, Peer.id, Owner.name, SeedDevice.id, PeerDevice.id,
      SeedIp.ip, PeerIp.ip, SeedTxn.amount, PeerTxn.amount,
      SeedMerchant.name, PeerMerchant.name, SeedCity.name, PeerCity.name,
      __path__)
}
```

Key ideas:

- `Seed` is bound to the start accounts (`__start__='true'`).
- `transfer` edges can expand 1 to 3 hops to reach `Peer` accounts.
- Context edges (devices, IPs, transactions, merchants, cities) are optional, so missing data does not drop a row.
- `keep_shortest_path` reduces duplicate traversal results by keeping only the shortest transfer path per seed.
- `__path__` returns the traversal path for inspection.

## Reading the output

Each row corresponds to one seed account and a reachable peer account:

- `Seed.id` and `Peer.id` identify the expanded account pair.
- Optional columns (device, IP, merchant, city) may be `null` if the edge is missing.
- `__path__` is a serialized list of vertex/edge steps that led to the peer.

This output is well-suited for a "triage" view where analysts want fast context around a risky seed.

## Try variations

1) Increase search depth:
   - Change `repeat(1,3)` to `repeat(1,5)`.
2) Focus on a specific channel:
   - Add `R3: SeedTxn.channel == 'WEB'` to the `Rule` block.
3) Broaden seeds:
   - Add more start IDs in `GraphExplorationLocalRunnerExample`.

If you want this to run on a real dataset, replace the in-memory graph loader with a loader that reads from your data source.
