package com.antgroup.openspg.examples;

import com.antgroup.openspg.reasoner.runner.ConfigKey;
import com.antgroup.openspg.reasoner.runner.local.LocalReasonerRunner;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerResult;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerTask;
import com.antgroup.openspg.examples.loader.NetworkRootCauseGraphMLLoader;
import com.antgroup.openspg.examples.loader.GraphMLLocalGraphLoader;
import com.google.common.collect.Lists;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NetworkRootCauseLocalRunnerExample {
  public static void main(String[] args) {
    String dsl = loadDsl("/kgdsl/network_root_cause.kgdsl");

    LocalReasonerTask task = new LocalReasonerTask();
    task.setDsl(dsl);
    task.setGraphLoadClass(NetworkRootCauseGraphMLLoader.class.getName());
    task.setStartIdList(
        Lists.newArrayList(
            new Tuple2<>("CI-1", "CallIssue"),
            new Tuple2<>("CI-2", "CallIssue"),
            new Tuple2<>("CI-3", "CallIssue")));

    task.setCatalog(
        GraphMLLocalGraphLoader.buildCatalog(NetworkRootCauseGraphMLLoader.GRAPHML_PATH, dsl));

    Map<String, Object> params = new HashMap<>();
    params.put(ConfigKey.KG_REASONER_BINARY_PROPERTY, "false");
    params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "true");
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    System.out.println(result);
  }

  private static String loadDsl(String resourcePath) {
    InputStream stream = NetworkRootCauseLocalRunnerExample.class.getResourceAsStream(resourcePath);
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
