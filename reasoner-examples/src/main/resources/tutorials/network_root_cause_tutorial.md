# Network Root Cause Tutorial (Call Failures)

This tutorial builds a more complex SPG Reasoner example for telecom-style root cause analysis. It starts from user call issues, checks base-station success rate, walks to the nearest failing network node, and links to the most plausible event. It also considers SIM arrears and user speed tests as alternative causes.

## Why this example (whitepaper link)

The SPG whitepaper highlights multi-entity risk reasoning such as **AI phone call victim alert** and cross-entity association (devices, domains, networks, events) for risk detection. This tutorial borrows that pattern and applies it to a telecom operations scenario, using the same idea of linking entity networks to event networks and filtering with rules.

## Scenario story

Customer support receives multiple complaints about call failures. For each reported issue, you want to:

1) Check if the serving base station has a poor success rate.
2) Traverse the backhaul chain to the nearest node with signal loss.
3) Link the call issue to the most relevant event.
4) If the SIM is in arrears, prefer that event instead.
5) If the base station is healthy but the user speed test is poor, attribute it to a local low-speed event.

## Files used

- DSL: `reasoner-examples/src/main/resources/kgdsl/network_root_cause.kgdsl`
- Runner: `reasoner-examples/src/main/java/com/antgroup/openspg/examples/NetworkRootCauseLocalRunnerExample.java`
- GraphML data: `reasoner-examples/src/main/resources/graphml/network_root_cause.graphml`

## How to run

From the repo root:

```bash
mvn -pl reasoner-examples -am exec:java \
  -Dexec.mainClass=com.antgroup.openspg.examples.NetworkRootCauseLocalRunnerExample
```

## Graph model (mini schema)

Nodes:

- `User`, `CallIssue`, `SimCard`, `BaseStation`, `NetworkNode`, `SpeedTest`, `Event`

Edges:

- `reportedBy` (CallIssue -> User)
- `at` (CallIssue -> BaseStation)
- `usesSim` (User -> SimCard)
- `speedTest` (User -> SpeedTest)
- `connectedTo` (BaseStation -> NetworkNode)
- `hasEvent` (NetworkNode/SimCard/SpeedTest -> Event)
- `rootCause` (CallIssue -> Event) — inferred by the rules

## How the Java example wires it together

The runner in `NetworkRootCauseLocalRunnerExample` does four things:

1) **Load the KGDSL**  
It reads `network_root_cause.kgdsl` from resources, so you can edit the DSL without touching Java.

2) **Infer and register the schema**  
It calls `GraphMLLocalGraphLoader.buildCatalog(..., dsl)` to infer node and edge properties from the
GraphML file and also register edge types declared in `Define` blocks. This lets the Reasoner validate
fields like `bs.successRate` or `ev.eventType` without a manual schema map.

3) **Seed the query**  
It sets the `startIdList` with the three `CallIssue` nodes. These are the starting points for the rules and
the final exploration query. In the DSL, only the last `GraphStructure` uses `__start__='true'` because
the local runner applies the start IDs to the last statement only.

4) **Load GraphML data**  
The `NetworkRootCauseGraphMLLoader` reads `network_root_cause.graphml` and builds the in-memory graph.
This keeps the example runnable without any external data source while making the dataset editable.

Key pieces to notice in the code:

- `task.setDsl(...)` binds the KGDSL to the runner.
- `task.setStartIdList(...)` controls which call issues are analyzed.
- This example keeps `spg.reasoner.lube.subquery.enable` off so the `Define` blocks run in order and
  materialize `rootCause` edges before the final query.
- `params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "true")` returns inferred edges like `rootCause`.

## Root cause rules (what the DSL does)

The DSL contains three `Define` blocks that infer `rootCause` edges:

1) **SIM arrears**  
If `sim.status == 'ARREARS'`, link the call issue to the `SIM_ARREARS` event.

2) **Backhaul outage**  
If the base station success rate is low and the nearest network node has significant signal loss, link to a high-severity `BACKHAUL_OUTAGE` event.

3) **Local low speed**  
If the SIM is OK, the base station is healthy, but the user speed test is low, link to a `LOW_SPEED` event.

Finally, a `get(...)` query returns the inferred root cause plus exploration context (base station KPI, SIM status, speed test, nearest network node, and the traversal path).

## What `network_root_cause.kgdsl` does (annotated)

At a high level, the file does two jobs:

1) **Infer `rootCause` edges** using three `Define` blocks.  
2) **Query the graph** and return the inferred root cause plus context.

Breakdown:

- **Define 1 (SIM arrears)**  
  Matches `CallIssue -> User -> SimCard -> Event` and creates `rootCause` when the SIM is in arrears.

- **Define 2 (Backhaul outage)**  
  Matches `CallIssue -> BaseStation -> (connectedTo)^1..3 -> NetworkNode -> Event`.  
  Applies KPI thresholds (`successRate`, `signalLossRate`, `severity`) and keeps the shortest path.

- **Define 3 (Low speed)**  
  Matches `CallIssue -> User -> SpeedTest -> Event`, with base-station health checks.  
  Creates `rootCause` for `LOW_SPEED` when access throughput is poor.

- **Final query**  
  Starts from seeded `CallIssue` nodes (`__start__='true'`), joins the inferred `rootCause`, and returns:
  issue type, SIM status, base-station KPIs, speed test, network node metrics, event type, and `__path__`.

## Reading the output

Each row corresponds to one `CallIssue` and its inferred `rootCause` event:

- `ci.issueType` shows the user-reported symptom.
- `bs.successRate` and `n.signalLossRate` explain network-side health.
- `sim.status` shows billing / arrears status.
- `st.downMbps` reveals local access issues.
- `ev.eventType` is the inferred root cause event.
- `__path__` shows the traversal path for the backhaul chain.

### Example edge output (graph mode)

If `kg.reasoner.output.graph=true`, the runner prints inferred edges instead of rows. Example:

```
edge_list_start
  (0) Edge(s=CallIssue_-2010113363008216754,p=CallIssue_rootCause_Event,o=Event_6782456717300958980,direction=OUT,version=0,property={
    "__to_id__":"EV-3",
    "__to_id_type__":"Event",
    "__from_id__":"CI-3",
    "__from_id_type__":"CallIssue",
    "process":{
      "hitRule":[
        {"ruleName":"R2","ruleValue":"bs.successRate >= 0.98","hitValue":"bs.successRate=0.98"},
        {"ruleName":"R3","ruleValue":"st.downMbps < 1.0","hitValue":"st.downMbps=0.4"},
        {"ruleName":"R4","ruleValue":"ev.eventType == \"LOW_SPEED\"","hitValue":"ev.eventType=LOW_SPEED"},
        {"ruleName":"R1","ruleValue":"sim.status != \"ARREARS\"","hitValue":"sim.status=OK"}
      ],
      "failedRule":[],
      "targetId":"EV-3",
      "targetLabel":"Event",
      "startNode":{"bizId":"CI-3","label":"CallIssue"}
    }
  })
edge_list_end
```

How to read it:

- **CI-3 -> EV-3**: `CallIssue` CI-3 is linked to `Event` EV-3.
- **process.hitRule**: the low-speed rule fired (SIM OK, base station healthy, speed test low).
- **failedRule** is empty: no rule in that branch failed.

## Try variations

1) Change the success-rate threshold in the DSL (e.g. `0.9` -> `0.95`) and rerun.
2) Add another call issue with no SIM arrears and no speed test to see how the backhaul rule behaves.
3) Add a second `connectedTo` hop and observe how `keep_shortest_path` selects the nearest failing node.
