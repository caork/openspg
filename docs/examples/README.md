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

## Files

- `reasoner-examples/src/main/resources/kgdsl/rca_root_cause.kgdsl`: RCA inference rule (Incident -> rootCause -> Component).
- `reasoner-examples/src/main/java/com/antgroup/openspg/examples/RcaLocalRunnerExample.java`: Java example that loads a graph and runs the RCA rule.
- `reasoner-examples/src/main/resources/kgdsl/student_teacher.kgdsl`: basic KGDSL pattern with a `get` action.

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

## Notes

- `LocalReasonerRunner` uses the graph loader class to populate an in-memory `GraphState`.
- The inferred `rootCause` edge is returned in the graph result.

