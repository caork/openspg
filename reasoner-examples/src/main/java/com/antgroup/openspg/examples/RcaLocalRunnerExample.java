package com.antgroup.openspg.examples;

import com.antgroup.openspg.reasoner.runner.ConfigKey;
import com.antgroup.openspg.reasoner.runner.local.LocalReasonerRunner;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerResult;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerTask;
import com.antgroup.openspg.examples.loader.RcaGraphMLLoader;
import com.antgroup.openspg.examples.loader.GraphMLLocalGraphLoader;
import java.util.HashMap;
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
    task.setGraphLoadClass(RcaGraphMLLoader.class.getName());
    task.setCatalog(GraphMLLocalGraphLoader.buildCatalog(RcaGraphMLLoader.GRAPHML_PATH, dsl));

    Map<String, Object> params = new HashMap<>();
    params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "true");
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    System.out.println(result);
  }

}
