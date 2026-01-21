package com.antgroup.openspg.examples;

import com.antgroup.openspg.reasoner.common.constants.Constants;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scala.Tuple2;

public class NetworkRootCauseLocalRunnerExample {
  public static void main(String[] args) {
    String dsl = loadDsl("/kgdsl/network_root_cause.kgdsl");

    LocalReasonerTask task = new LocalReasonerTask();
    task.setDsl(dsl);
    task.setGraphLoadClass(NetworkRootCauseGraphLoader.class.getName());
    task.setStartIdList(
        Lists.newArrayList(
            new Tuple2<>("CI-1", "CallIssue"),
            new Tuple2<>("CI-2", "CallIssue"),
            new Tuple2<>("CI-3", "CallIssue")));

    Map<String, scala.collection.immutable.Set<String>> schema = new HashMap<>();
    schema.put("User", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name")));
    schema.put(
        "CallIssue",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "issueType", "createTime")));
    schema.put("SimCard", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "status")));
    schema.put(
        "BaseStation",
        Convert2ScalaUtil.toScalaImmutableSet(
            Sets.newHashSet("id", "name", "successRate", "region")));
    schema.put(
        "NetworkNode",
        Convert2ScalaUtil.toScalaImmutableSet(
            Sets.newHashSet("id", "name", "nodeType", "signalLossRate")));
    schema.put(
        "SpeedTest",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "downMbps", "upMbps")));
    schema.put(
        "Event",
        Convert2ScalaUtil.toScalaImmutableSet(
            Sets.newHashSet("id", "eventType", "severity", "startTime", "description")));

    schema.put("CallIssue_reportedBy_User", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("CallIssue_at_BaseStation", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("User_usesSim_SimCard", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("User_speedTest_SpeedTest", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "BaseStation_connectedTo_NetworkNode",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "NetworkNode_hasEvent_Event", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("SimCard_hasEvent_Event", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("SpeedTest_hasEvent_Event", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("CallIssue_rootCause_Event", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));

    Catalog catalog = new PropertyGraphCatalog(Convert2ScalaUtil.toScalaImmutableMap(schema));
    catalog.init();
    task.setCatalog(catalog);

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

  public static class NetworkRootCauseGraphLoader extends AbstractLocalGraphLoader {
    @Override
    public List<IVertex<String, IProperty>> genVertexList() {
      return Lists.newArrayList(
          constructionVertex("U-1", "User", "name", "Yuan"),
          constructionVertex("U-2", "User", "name", "Chen"),
          constructionVertex("U-3", "User", "name", "Lin"),
          constructionVertex("CI-1", "CallIssue", "issueType", "CALL_DROP", "createTime", "2024-07-01"),
          constructionVertex("CI-2", "CallIssue", "issueType", "CALL_FAIL", "createTime", "2024-07-02"),
          constructionVertex(
              "CI-3", "CallIssue", "issueType", "POOR_QUALITY", "createTime", "2024-07-03"),
          constructionVertex("SIM-1", "SimCard", "status", "OK"),
          constructionVertex("SIM-2", "SimCard", "status", "ARREARS"),
          constructionVertex("SIM-3", "SimCard", "status", "OK"),
          constructionVertex(
              "BS-1", "BaseStation", "name", "BS-HZ-01", "successRate", 0.72, "region", "HZ"),
          constructionVertex(
              "BS-2", "BaseStation", "name", "BS-SH-05", "successRate", 0.99, "region", "SH"),
          constructionVertex(
              "BS-3", "BaseStation", "name", "BS-SZ-02", "successRate", 0.98, "region", "SZ"),
          constructionVertex(
              "N-1", "NetworkNode", "name", "Backhaul-A", "nodeType", "BACKHAUL", "signalLossRate", 0.6),
          constructionVertex(
              "N-2", "NetworkNode", "name", "Core-A", "nodeType", "CORE", "signalLossRate", 0.1),
          constructionVertex(
              "N-3", "NetworkNode", "name", "Backhaul-B", "nodeType", "BACKHAUL", "signalLossRate", 0.2),
          constructionVertex("ST-1", "SpeedTest", "downMbps", 45.5, "upMbps", 8.1),
          constructionVertex("ST-2", "SpeedTest", "downMbps", 12.0, "upMbps", 4.2),
          constructionVertex("ST-3", "SpeedTest", "downMbps", 0.4, "upMbps", 0.2),
          constructionVertex(
              "EV-1",
              "Event",
              "eventType",
              "BACKHAUL_OUTAGE",
              "severity",
              4,
              "startTime",
              "2024-07-01T09:05:00Z",
              "description",
              "Backhaul fiber cut"),
          constructionVertex(
              "EV-2",
              "Event",
              "eventType",
              "SIM_ARREARS",
              "severity",
              1,
              "startTime",
              "2024-07-02T08:10:00Z",
              "description",
              "SIM card overdue"),
          constructionVertex(
              "EV-3",
              "Event",
              "eventType",
              "LOW_SPEED",
              "severity",
              2,
              "startTime",
              "2024-07-03T18:20:00Z",
              "description",
              "Local access speed degradation"));
    }

    @Override
    public List<IEdge<String, IProperty>> genEdgeList() {
      return Lists.newArrayList(
          constructionEdge("CI-1", "reportedBy", "U-1"),
          constructionEdge("CI-2", "reportedBy", "U-2"),
          constructionEdge("CI-3", "reportedBy", "U-3"),
          constructionEdge("CI-1", "at", "BS-1"),
          constructionEdge("CI-2", "at", "BS-2"),
          constructionEdge("CI-3", "at", "BS-3"),
          constructionEdge("U-1", "usesSim", "SIM-1"),
          constructionEdge("U-2", "usesSim", "SIM-2"),
          constructionEdge("U-3", "usesSim", "SIM-3"),
          constructionEdge("U-1", "speedTest", "ST-1"),
          constructionEdge("U-2", "speedTest", "ST-2"),
          constructionEdge("U-3", "speedTest", "ST-3"),
          constructionEdge("BS-1", "connectedTo", "N-1"),
          constructionEdge("N-1", "connectedTo", "N-2"),
          constructionEdge("BS-2", "connectedTo", "N-2"),
          constructionEdge("BS-3", "connectedTo", "N-3"),
          constructionEdge("N-1", "hasEvent", "EV-1"),
          constructionEdge("SIM-2", "hasEvent", "EV-2"),
          constructionEdge("ST-3", "hasEvent", "EV-3"));
    }
  }
}
