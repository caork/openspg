package com.antgroup.openspg.examples;

import com.antgroup.openspg.examples.loader.GraphMLLocalGraphLoader;
import com.antgroup.openspg.examples.loader.RiskCausalFusionGraphMLLoader;
import com.antgroup.openspg.reasoner.common.constants.Constants;
import com.antgroup.openspg.reasoner.runner.ConfigKey;
import com.antgroup.openspg.reasoner.runner.local.LocalReasonerRunner;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerResult;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerTask;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import scala.Tuple2;

public class RiskCausalFusionLocalRunnerExample {
  public static void main(String[] args) {
    String dsl = loadDsl("/kgdsl/risk_causal_fusion.kgdsl");

    LocalReasonerTask task = new LocalReasonerTask();
    task.setDsl(dsl);
    task.setGraphLoadClass(RiskCausalFusionGraphMLLoader.class.getName());
    task.setStartIdList(
        Lists.newArrayList(new Tuple2<>("ACC-1", "Account"), new Tuple2<>("ACC-2", "Account")));
    task.setCatalog(
        GraphMLLocalGraphLoader.buildCatalog(RiskCausalFusionGraphMLLoader.GRAPHML_PATH, dsl));

    Map<String, Object> params = new HashMap<>();
    params.put(Constants.SPG_REASONER_LUBE_SUBQUERY_ENABLE, false);
    params.put(ConfigKey.KG_REASONER_BINARY_PROPERTY, "false");
    params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "false");
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    System.out.println(result);
  }

  private static String loadDsl(String resourcePath) {
    InputStream stream = RiskCausalFusionLocalRunnerExample.class.getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IllegalStateException("Missing resource: " + resourcePath);
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load KGDSL: " + resourcePath, e);
    }
  }
}
