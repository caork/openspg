# Examples

This page documents runnable examples stored in the `reasoner-examples` module.

## Module

- `reasoner-examples/`: standalone module that depends on the Reasoner runtime.
- Import the Maven project and run the main class from the IDE.

## How to run (Maven)

```
# from repo root
mvn -pl reasoner-examples -am exec:java
```

## Local runner configuration tips

The local runner is configured through `LocalReasonerTask`:

- `setDsl(...)` or `setDslDagList(...)` for the KGDSL or preplanned operators.
- `setCatalog(...)` or `setSchemaString(...)` for schema binding.
- `setGraphLoadClass(...)` or `setGraphState(...)` for graph loading.
- `setParams(...)` for runtime controls (see `ConfigKey`).

Common parameters:

- `ConfigKey.KG_REASONER_OUTPUT_GRAPH`: return inferred graph data.
- `ConfigKey.KG_REASONER_MOCK_GRAPH_DATA`: inline graph data for quick testing.
- `ConfigKey.KG_REASONER_MAX_PATH_LIMIT`: cap path expansion.
- `ConfigKey.KG_REASONER_STRICT_MAX_PATH_THRESHOLD`: fail fast on path overflow.

## Files

- `reasoner-examples/src/main/resources/kgdsl/rca_root_cause.kgdsl`: RCA inference rule (Incident -> rootCause -> Component).
- `reasoner-examples/src/main/java/com/antgroup/openspg/examples/RcaLocalRunnerExample.java`: Java example that loads a graph and runs the RCA rule.
- `reasoner-examples/src/main/resources/kgdsl/student_teacher.kgdsl`: basic KGDSL pattern with a `get` action.
- `reasoner-examples/src/main/resources/kgdsl/graph_exploration.kgdsl`: graph exploration starting from seed accounts and expanding via transfers, devices, IPs, and merchants.
- `reasoner-examples/src/main/java/com/antgroup/openspg/examples/GraphExplorationLocalRunnerExample.java`: Java example that loads the exploration graph and runs the DSL.
- `reasoner-examples/src/main/resources/tutorials/graph_exploration_tutorial.md`: tutorial for the seed-based exploration scenario.
- `reasoner-examples/src/main/resources/kgdsl/network_root_cause.kgdsl`: network root cause analysis for call issues (SIM arrears, backhaul outage, and low speed).
- `reasoner-examples/src/main/java/com/antgroup/openspg/examples/NetworkRootCauseLocalRunnerExample.java`: Java example that runs the network root cause rules.
- `reasoner-examples/src/main/resources/tutorials/network_root_cause_tutorial.md`: tutorial for the network root cause scenario.

## RCA KGDSL example

```
Define (i:Incident)-[rc:rootCause]->(c:Component) {
  GraphStructure {
    (i:Incident)-[t:triggeredBy]->(a:Alarm)-[on]->(c:Component)
  }
  Rule {
  }
  Action {
    createEdgeInstance(
      src=i,
      dst=c,
      type=rootCause,
      value={}
    )
  }
}
```

## Java local runner example

```
import com.antgroup.openspg.reasoner.common.graph.edge.IEdge;
import com.antgroup.openspg.reasoner.common.graph.property.IProperty;
import com.antgroup.openspg.reasoner.common.graph.vertex.IVertex;
import com.antgroup.openspg.reasoner.lube.catalog.Catalog;
import com.antgroup.openspg.reasoner.lube.catalog.impl.PropertyGraphCatalog;
import com.antgroup.openspg.reasoner.runner.ConfigKey;
import com.antgroup.openspg.reasoner.runner.local.LocalReasonerRunner;
import com.antgroup.openspg.reasoner.runner.local.load.graph.AbstractLocalGraphLoader;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerResult;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerTask;
import com.antgroup.openspg.reasoner.util.Convert2ScalaUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RcaLocalRunnerExample {
  public static void main(String[] args) {
    String dsl =
        "Define (i:Incident)-[rc:rootCause]->(c:Component) {\n"
            + "  GraphStructure {\n"
            + "    (i:Incident)-[t:triggeredBy]->(a:Alarm)-[on]->(c:Component)\n"
            + "  }\n"
            + "  Rule {\n"
            + "  }\n"
            + "  Action {\n"
            + "    createEdgeInstance(\n"
            + "      src=i,\n"
            + "      dst=c,\n"
            + "      type=rootCause,\n"
            + "      value={}\n"
            + "    )\n"
            + "  }\n"
            + "}";

    LocalReasonerTask task = new LocalReasonerTask();
    task.setDsl(dsl);
    task.setGraphLoadClass(RcaGraphLoader.class.getName());

    Map<String, scala.collection.immutable.Set<String>> schema = new HashMap<>();
    schema.put("Incident", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name")));
    schema.put("Alarm", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "level")));
    schema.put("Component", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name")));
    schema.put(
        "Incident_triggeredBy_Alarm",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("Alarm_on_Component", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "Incident_rootCause_Component",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));

    Catalog catalog = new PropertyGraphCatalog(Convert2ScalaUtil.toScalaImmutableMap(schema));
    catalog.init();
    task.setCatalog(catalog);

    Map<String, Object> params = new HashMap<>();
    params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "true");
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    System.out.println(result);
  }

  public static class RcaGraphLoader extends AbstractLocalGraphLoader {
    @Override
    public List<IVertex<String, IProperty>> genVertexList() {
      return Lists.newArrayList(
          constructionVertex("INC-1", "Incident", "name", "Checkout incident"),
          constructionVertex("ALARM-1", "Alarm", "level", "HIGH"),
          constructionVertex("COMP-1", "Component", "name", "PaymentService"));
    }

    @Override
    public List<IEdge<String, IProperty>> genEdgeList() {
      return Lists.newArrayList(
          constructionEdge("INC-1", "triggeredBy", "ALARM-1"),
          constructionEdge("ALARM-1", "on", "COMP-1"));
    }
  }
}
```

## Graph exploration KGDSL example

```
GraphStructure {
  Seed [Account, __start__='true']
  Peer [Account]
  Owner [Person]
  SeedDevice [Device]
  PeerDevice [Device]
  SeedIp [Ip]
  PeerIp [Ip]
  SeedTxn [Transaction]
  PeerTxn [Transaction]
  SeedMerchant [Merchant]
  PeerMerchant [Merchant]
  SeedCity [City]
  PeerCity [City]
  Seed->Peer [transfer] repeat(1,3) as transferPath
  Seed->Owner [ownedBy]
  Seed->SeedDevice [loginWith, __optional__='true'] as seedDevice
  Peer->PeerDevice [loginWith, __optional__='true'] as peerDevice
  SeedDevice->SeedIp [bindIp, __optional__='true'] as seedIp
  PeerDevice->PeerIp [bindIp, __optional__='true'] as peerIp
  Seed->SeedTxn [makeTxn, __optional__='true'] as seedTxn
  Peer->PeerTxn [makeTxn, __optional__='true'] as peerTxn
  SeedTxn->SeedMerchant [toMerchant, __optional__='true'] as seedMerchant
  PeerTxn->PeerMerchant [toMerchant, __optional__='true'] as peerMerchant
  SeedMerchant->SeedCity [locatedIn, __optional__='true'] as seedCity
  PeerMerchant->PeerCity [locatedIn, __optional__='true'] as peerCity
}
Rule {
  R1: Seed.riskLevel == 'HIGH'
  R2: group(Seed).keep_shortest_path(transferPath)
}
Action {
  get(Seed.id, Peer.id, Owner.name, SeedDevice.id, PeerDevice.id, SeedIp.ip, PeerIp.ip, SeedTxn.amount, PeerTxn.amount, SeedMerchant.name, PeerMerchant.name, SeedCity.name, PeerCity.name, __path__)
}
```

## Notes

- `LocalReasonerRunner` uses the graph loader class to populate an in-memory `GraphState`.
- The inferred `rootCause` edge is returned in the graph result.
