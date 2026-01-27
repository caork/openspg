package com.antgroup.openspg.examples;

import com.alibaba.fastjson.JSON;
import com.antgroup.openspg.examples.loader.NetworkCaseSchemaWithDataLoader;
import com.antgroup.openspg.examples.loader.SchemaLocalGraphLoader;
import com.antgroup.openspg.reasoner.common.constants.Constants;
import com.antgroup.openspg.reasoner.graphstate.impl.MemGraphState;
import com.antgroup.openspg.reasoner.lube.catalog.Catalog;
import com.antgroup.openspg.reasoner.lube.logical.operators.LogicalOperator;
import com.antgroup.openspg.reasoner.lube.physical.operators.PhysicalOperator;
import com.antgroup.openspg.reasoner.parser.OpenSPGDslParser;
import com.antgroup.openspg.reasoner.runner.ConfigKey;
import com.antgroup.openspg.reasoner.runner.local.LocalReasonerRunner;
import com.antgroup.openspg.reasoner.runner.local.impl.LocalReasonerSession;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerResult;
import com.antgroup.openspg.reasoner.runner.local.model.LocalReasonerTask;
import com.antgroup.openspg.reasoner.runner.local.rdg.LocalRDG;
import com.antgroup.openspg.reasoner.runner.local.rdg.TypeTags;
import com.antgroup.openspg.reasoner.util.Convert2ScalaUtil;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import scala.Tuple2;
import scala.collection.JavaConversions;

public class NetworkCaseSchemaRootCauseLocalRunnerWithPlanExample {
  private static final String DSL_RESOURCE = "/kgdsl/network_case_1_root_cause.kgdsl";
  private static final String ARTIFACTS_DIR =
      "reasoner-examples/src/main/resources/examples/NetworkCaseSchemaRootCauseLocalRunnerWithPlanExample/output";
  private static final String BRANCH_MID = "\u251c\u2500";
  private static final String BRANCH_END = "\u2514\u2500";
  private static final int DOT_MAX_LABEL_LENGTH = 10000;
  private static final int DOT_MAX_LINE_LENGTH = 48;
  private static final int DOT_MAX_LINES = 200;
  private static final String[] NODE_ID_KEYS = {
    "id",
    "experienceId",
    "eventId",
    "userId",
    "cellId",
    "serviceId",
    "metricId"
  };
  private static final String[] NODE_NAME_KEYS = {"name", "metricName"};
  private static volatile List<RoutePath> lastRoutePaths = new ArrayList<>();

  public static void main(String[] args) {
    String dsl = loadDsl(DSL_RESOURCE);

    MemGraphState graphState = new MemGraphState();
    NetworkCaseSchemaWithDataLoader graphLoader = new NetworkCaseSchemaWithDataLoader();
    graphLoader.setGraphState(graphState);
    graphLoader.load();

    Catalog catalog =
        SchemaLocalGraphLoader.buildCatalog(NetworkCaseSchemaWithDataLoader.SCHEMA_PATH, dsl);

    List<Tuple2<String, String>> startIdList =
        Lists.newArrayList(
            new Tuple2<>("EXP_1", "PhoneCallsServiceExperience"),
            new Tuple2<>("EXP_2", "PhoneCallsServiceExperience"));

    boolean outputGraph = false;
    for (String arg : args) {
      if ("--graph".equalsIgnoreCase(arg) || "graph".equalsIgnoreCase(arg)) {
        outputGraph = true;
        break;
      }
    }

    Map<String, Object> params = new HashMap<>();
    params.put(ConfigKey.KG_REASONER_BINARY_PROPERTY, "false");
    params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, String.valueOf(outputGraph));
    params.put(Constants.SPG_REASONER_PLAN_PRETTY_PRINT_LOGGER_ENABLE, "false");
    params.put(Constants.START_LABEL, startIdList.get(0)._2);

    LocalReasonerSession session =
        new LocalReasonerSession(
            new OpenSPGDslParser(), catalog, TypeTags.rdgTypeTag(), graphState);

    List<PhysicalOperator<LocalRDG>> physicalPlans =
        Lists.newArrayList(
            JavaConversions.asJavaCollection(
                session.plan(dsl, Convert2ScalaUtil.toScalaImmutableMap(params))));
    List<LogicalOperator> logicalPlans =
        Lists.newArrayList(JavaConversions.asJavaCollection(session.getOptimizedLogicalPlan()));

    String planText = formatPlan(logicalPlans, physicalPlans);
    printAndPersistArtifacts(dsl, planText, logicalPlans, physicalPlans);

    LocalReasonerTask task = new LocalReasonerTask();
    ThreadPoolExecutor executor = createDaemonExecutor();
    task.setThreadPoolExecutor(executor);
    task.setDsl(dsl);
    task.setSession(session);
    task.setDslDagList(physicalPlans);
    task.setGraphState(graphState);
    task.setCatalog(catalog);
    task.setStartIdList(startIdList);
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    if (outputGraph) {
      System.out.println(result);
    } else {
      lastRoutePaths = extractRoutePaths(result);
      for (RoutePath route : lastRoutePaths) {
        System.out.println(route);
      }
    }
    executor.shutdown();
  }

  public static List<RoutePath> getLastRoutePaths() {
    return lastRoutePaths;
  }

  public static List<RoutePath> extractRoutePaths(LocalReasonerResult result) {
    if (result == null) {
      return new ArrayList<>();
    }
    if (result.getErrMsg() != null && !result.getErrMsg().isEmpty()) {
      return new ArrayList<>();
    }
    List<String> columns = result.getColumns();
    List<Object[]> rows = result.getRows();
    if (columns == null || rows == null) {
      return new ArrayList<>();
    }
    int pathIndex = findColumnIndex(columns, Constants.GET_PATH_KEY);
    if (pathIndex < 0) {
      return new ArrayList<>();
    }
    List<RoutePath> routePaths = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      Object[] row = rows.get(rowIndex);
      Object pathObj = pathIndex < row.length ? row[pathIndex] : null;
      if (pathObj == null) {
        continue;
      }
      List<Map<String, Object>> entries;
      try {
        entries = parsePathEntries(String.valueOf(pathObj));
      } catch (Exception e) {
        continue;
      }
      PathSummary summary = buildPathSummary(entries);
      List<Route> routes = buildRoutes(summary);
      if (!routes.isEmpty()) {
        for (Route route : routes) {
          routePaths.add(toRoutePath(route, summary.nodesByInternalId));
        }
        continue;
      }
      for (EdgeInfo edge : summary.edges) {
        routePaths.add(toRoutePath(edge, summary.nodesByInternalId));
      }
    }
    return routePaths;
  }

  private static int findColumnIndex(List<String> columns, String name) {
    for (int i = 0; i < columns.size(); i++) {
      if (name.equals(columns.get(i))) {
        return i;
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parsePathEntries(String value) {
    return (List<Map<String, Object>>) (List<?>) JSON.parseArray(value, Map.class);
  }

  private static PathSummary buildPathSummary(List<Map<String, Object>> entries) {
    Map<String, NodeInfo> nodesByInternalId = new LinkedHashMap<>();
    List<NodeInfo> nodes = new ArrayList<>();
    List<EdgeInfo> edges = new ArrayList<>();
    Set<String> edgeKeys = new HashSet<>();

    if (entries == null) {
      return new PathSummary(nodes, edges, nodesByInternalId);
    }

    for (Map<String, Object> entry : entries) {
      if (!isVertex(entry)) {
        continue;
      }
      if (isTruthy(entry.get(Constants.NONE_VERTEX_FLAG))) {
        continue;
      }
      NodeInfo node = toNodeInfo(entry);
      if (node.internalId == null || node.internalId.isEmpty()) {
        continue;
      }
      if (!nodesByInternalId.containsKey(node.internalId)) {
        nodesByInternalId.put(node.internalId, node);
        nodes.add(node);
      }
    }

    for (Map<String, Object> entry : entries) {
      if (!isEdge(entry)) {
        continue;
      }
      if (isTruthy(entry.get(Constants.OPTIONAL_EDGE_FLAG))
          && (entry.get(Constants.EDGE_FROM_INTERNAL_ID_KEY) == null
              || entry.get(Constants.EDGE_TO_INTERNAL_ID_KEY) == null)) {
        continue;
      }
      EdgeInfo edge = toEdgeInfo(entry, nodesByInternalId);
      if (edge == null) {
        continue;
      }
      if (edgeKeys.add(edge.id + "|" + edge.name)) {
        edges.add(edge);
      }
    }

    return new PathSummary(nodes, edges, nodesByInternalId);
  }

  private static List<Route> buildRoutes(PathSummary summary) {
    if (summary.edges.isEmpty()) {
      return new ArrayList<>();
    }
    Map<String, List<EdgeInfo>> adjacency = new LinkedHashMap<>();
    Map<String, Integer> inDegree = new HashMap<>();
    for (EdgeInfo edge : summary.edges) {
      adjacency.computeIfAbsent(edge.fromInternalId, ignored -> new ArrayList<>()).add(edge);
      inDegree.put(edge.toInternalId, inDegree.getOrDefault(edge.toInternalId, 0) + 1);
      inDegree.putIfAbsent(edge.fromInternalId, 0);
    }

    List<String> sources = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
      String nodeId = entry.getKey();
      if (entry.getValue() == 0 && adjacency.containsKey(nodeId)) {
        sources.add(nodeId);
      }
    }
    if (sources.isEmpty()) {
      sources.addAll(adjacency.keySet());
    }

    List<Route> routes = new ArrayList<>();
    for (String source : sources) {
      dfsRoutes(source, source, adjacency, new ArrayList<>(), new HashSet<>(), routes);
    }
    return routes;
  }

  private static void dfsRoutes(
      String source,
      String current,
      Map<String, List<EdgeInfo>> adjacency,
      List<EdgeInfo> pathEdges,
      Set<String> visited,
      List<Route> routes) {
    if (visited.contains(current)) {
      return;
    }
    visited.add(current);
    List<EdgeInfo> outgoing = adjacency.get(current);
    if (outgoing == null || outgoing.isEmpty()) {
      if (!pathEdges.isEmpty()) {
        routes.add(new Route(source, new ArrayList<>(pathEdges)));
      }
    } else {
      for (EdgeInfo edge : outgoing) {
        pathEdges.add(edge);
        dfsRoutes(source, edge.toInternalId, adjacency, pathEdges, visited, routes);
        pathEdges.remove(pathEdges.size() - 1);
      }
    }
    visited.remove(current);
  }

  private static RoutePath toRoutePath(Route route, Map<String, NodeInfo> nodesByInternalId) {
    List<String> nodeIds = new ArrayList<>();
    String current = route.startInternalId;
    nodeIds.add(formatNodeId(nodesByInternalId.get(current), current));
    for (EdgeInfo edge : route.edges) {
      nodeIds.add(formatNodeId(nodesByInternalId.get(edge.toInternalId), edge.toInternalId));
      current = edge.toInternalId;
    }
    return new RoutePath(nodeIds);
  }

  private static RoutePath toRoutePath(
      EdgeInfo edge, Map<String, NodeInfo> nodesByInternalId) {
    List<String> nodeIds = new ArrayList<>();
    nodeIds.add(formatNodeId(nodesByInternalId.get(edge.fromInternalId), edge.fromInternalId));
    nodeIds.add(formatNodeId(nodesByInternalId.get(edge.toInternalId), edge.toInternalId));
    return new RoutePath(nodeIds);
  }

  private static String formatNodeId(NodeInfo node, String fallbackId) {
    String id = fallbackId;
    if (node != null) {
      if (node.id != null && !node.id.isEmpty()) {
        id = node.id;
      }
    }
    return id;
  }

  private static boolean isVertex(Map<String, Object> entry) {
    return "vertex".equals(String.valueOf(entry.get(Constants.CONTEXT_TYPE)));
  }

  private static boolean isEdge(Map<String, Object> entry) {
    return "edge".equals(String.valueOf(entry.get(Constants.CONTEXT_TYPE)));
  }

  private static boolean isTruthy(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return "true".equalsIgnoreCase(String.valueOf(value));
  }

  private static NodeInfo toNodeInfo(Map<String, Object> entry) {
    String internalId = stringValue(entry.get(Constants.VERTEX_INTERNAL_ID_KEY));
    String id = firstNonBlank(entry, NODE_ID_KEYS);
    if (id == null || id.isEmpty()) {
      id = internalId;
    }
    String name = firstNonBlank(entry, NODE_NAME_KEYS);
    if (name == null || name.isEmpty()) {
      name = stringValue(entry.get(Constants.CONTEXT_LABEL));
    }
    return new NodeInfo(internalId, id, name);
  }

  private static EdgeInfo toEdgeInfo(
      Map<String, Object> entry, Map<String, NodeInfo> nodesByInternalId) {
    String fromInternalId = stringValue(entry.get(Constants.EDGE_FROM_INTERNAL_ID_KEY));
    String toInternalId = stringValue(entry.get(Constants.EDGE_TO_INTERNAL_ID_KEY));
    if (fromInternalId == null || toInternalId == null) {
      return null;
    }
    String fromId = resolveNodeId(fromInternalId, nodesByInternalId);
    String toId = resolveNodeId(toInternalId, nodesByInternalId);
    String edgeId = fromId + "->" + toId;
    String edgeName = stringValue(entry.get(Constants.CONTEXT_LABEL));
    return new EdgeInfo(edgeId, edgeName, fromInternalId, toInternalId);
  }

  private static String resolveNodeId(String internalId, Map<String, NodeInfo> nodesByInternalId) {
    NodeInfo node = nodesByInternalId.get(internalId);
    if (node == null) {
      return internalId;
    }
    return node.id;
  }

  private static String firstNonBlank(Map<String, Object> entry, String[] keys) {
    for (String key : keys) {
      String value = stringValue(entry.get(key));
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return null;
  }

  private static String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }

  private static final class PathSummary {
    private final List<NodeInfo> nodes;
    private final List<EdgeInfo> edges;
    private final Map<String, NodeInfo> nodesByInternalId;

    private PathSummary(
        List<NodeInfo> nodes, List<EdgeInfo> edges, Map<String, NodeInfo> nodesByInternalId) {
      this.nodes = nodes;
      this.edges = edges;
      this.nodesByInternalId = nodesByInternalId;
    }
  }

  private static final class Route {
    private final String startInternalId;
    private final List<EdgeInfo> edges;

    private Route(String startInternalId, List<EdgeInfo> edges) {
      this.startInternalId = startInternalId;
      this.edges = edges;
    }
  }

  private static final class NodeInfo {
    private final String internalId;
    private final String id;
    private final String name;

    private NodeInfo(String internalId, String id, String name) {
      this.internalId = internalId;
      this.id = id;
      this.name = name;
    }
  }

  private static final class EdgeInfo {
    private final String id;
    private final String name;
    private final String fromInternalId;
    private final String toInternalId;

    private EdgeInfo(String id, String name, String fromInternalId, String toInternalId) {
      this.id = id;
      this.name = name;
      this.fromInternalId = fromInternalId;
      this.toInternalId = toInternalId;
    }
  }

  public static final class RoutePath {
    private final List<String> nodeIds;

    private RoutePath(List<String> nodeIds) {
      this.nodeIds = nodeIds;
    }

    public List<String> getNodeIds() {
      return nodeIds;
    }

    @Override
    public String toString() {
      return String.join(" -> ", nodeIds);
    }
  }

  private static String formatPlan(
      List<LogicalOperator> logicalPlans, List<PhysicalOperator<LocalRDG>> physicalPlans) {
    StringBuilder builder = new StringBuilder();
    builder.append("Logical Plan").append('\n');
    for (int i = 0; i < logicalPlans.size(); i++) {
      builder.append("--- Logical Plan ").append(i + 1).append(" ---").append('\n');
      builder.append(logicalPlans.get(i).pretty()).append('\n');
    }
    builder.append('\n').append("Physical Plan").append('\n');
    for (int i = 0; i < physicalPlans.size(); i++) {
      builder.append("--- Physical Plan ").append(i + 1).append(" ---").append('\n');
      builder.append(physicalPlans.get(i).pretty()).append('\n');
    }
    return builder.toString();
  }

  private static void printAndPersistArtifacts(
      String dsl,
      String planText,
      List<LogicalOperator> logicalPlans,
      List<PhysicalOperator<LocalRDG>> physicalPlans) {
    System.out.println("=== DQL ===");
    System.out.println(dsl);
    System.out.println("=== EXECUTION PLAN ===");
    System.out.println(planText);

    try {
      Path outputPath = Paths.get(ARTIFACTS_DIR);
      Files.createDirectories(outputPath);
      Files.write(outputPath.resolve("dql.txt"), dsl.getBytes(StandardCharsets.UTF_8));
      Files.write(outputPath.resolve("plan.txt"), planText.getBytes(StandardCharsets.UTF_8));
      List<String> logicalPlanPretties = new ArrayList<>();
      for (LogicalOperator plan : logicalPlans) {
        logicalPlanPretties.add(plan.pretty());
      }
      List<String> physicalPlanPretties = new ArrayList<>();
      for (PhysicalOperator<LocalRDG> plan : physicalPlans) {
        physicalPlanPretties.add(plan.pretty());
      }
      writePlanImages(outputPath, "logical_plan", logicalPlanPretties);
      writePlanImages(outputPath, "physical_plan", physicalPlanPretties);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write artifacts", e);
    }
  }

  private static void writePlanImages(Path outputPath, String prefix, List<String> plans)
      throws IOException {
    for (int i = 0; i < plans.size(); i++) {
      String dot = toDotFromPretty(plans.get(i), prefix + " " + (i + 1));
      Path dotPath = outputPath.resolve(prefix + "_" + (i + 1) + ".dot");
      Path pngPath = outputPath.resolve(prefix + "_" + (i + 1) + ".png");
      Files.write(dotPath, dot.getBytes(StandardCharsets.UTF_8));
      renderDotIfAvailable(dotPath, pngPath);
    }
  }

  private static void renderDotIfAvailable(Path dotPath, Path pngPath) {
    ProcessBuilder builder =
        new ProcessBuilder("dot", "-Tpng", dotPath.toString(), "-o", pngPath.toString());
    builder.redirectErrorStream(true);
    try {
      Process process = builder.start();
      String output = readProcessOutput(process);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        System.out.println("Graphviz dot failed for " + dotPath + ": " + output);
      }
    } catch (IOException e) {
      System.out.println(
          "Graphviz dot not found; plan DOT saved but PNG not rendered for " + dotPath + ".");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Graphviz dot interrupted for " + dotPath + ".");
    }
  }

  private static String readProcessOutput(Process process) throws IOException {
    try (InputStream stream = process.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int read;
      while ((read = stream.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
      return output.toString(StandardCharsets.UTF_8.name()).trim();
    }
  }

  private static String toDotFromPretty(String pretty, String graphLabel) {
    StringBuilder builder = new StringBuilder();
    builder.append("digraph plan {\n");
    builder
        .append("  graph [label=\"")
        .append(escapeDot(graphLabel))
        .append("\", labelloc=t, rankdir=TB, ratio=compress, nodesep=0.35, ranksep=0.45, pad=0.2];\n");
    builder.append(
        "  node [shape=box, style=\"rounded,filled\", fillcolor=\"#E8F4FF\", fontname=\"Helvetica\", fontsize=10];\n");
    builder.append("  edge [color=\"#4A4A4A\", arrowsize=0.7];\n");

    Map<Integer, String> depthToId = new HashMap<>();
    int counter = 0;
    String[] lines = pretty.split("\n");
    for (String line : lines) {
      if (line.trim().isEmpty()) {
        continue;
      }
      int branchIndex = line.indexOf(BRANCH_MID);
      if (branchIndex < 0) {
        branchIndex = line.indexOf(BRANCH_END);
      }
      int depth = 0;
      String label;
      if (branchIndex >= 0) {
        depth = branchIndex / 4;
        label = line.substring(branchIndex + 2);
      } else {
        label = line.trim();
      }
      label = formatDotLabel(label);

      String nodeId = "n" + counter++;
      builder
          .append("  ")
          .append(nodeId)
          .append(" [label=\"")
          .append(escapeDot(label))
          .append("\"];\n");
      if (depth > 0 && depthToId.containsKey(depth - 1)) {
        builder.append("  ").append(depthToId.get(depth - 1)).append(" -> ").append(nodeId).append(";\n");
      }
      depthToId.put(depth, nodeId);
    }

    builder.append("}\n");
    return builder.toString();
  }

  private static String formatDotLabel(String label) {
    String compact = label.trim().replaceAll("\\s+", " ");
    int parenIndex = compact.indexOf('(');
    if (parenIndex > 0 && compact.endsWith(")")) {
      String name = compact.substring(0, parenIndex);
      String args = compact.substring(parenIndex + 1, compact.length() - 1).trim();
      if (args.length() > DOT_MAX_LABEL_LENGTH) {
        args = args.substring(0, DOT_MAX_LABEL_LENGTH - 3) + "...";
      }
      compact = args.isEmpty() ? name : name + "(" + args + ")";
    }
    return wrapLabel(compact, DOT_MAX_LINE_LENGTH, DOT_MAX_LINES);
  }

  private static String escapeDot(String value) {
    return value.replace("\"", "\\\"").replace("\n", "\\n");
  }

  private static String wrapLabel(String text, int maxLineLength, int maxLines) {
    String remaining = text;
    StringBuilder builder = new StringBuilder();
    int lineCount = 0;
    while (!remaining.isEmpty() && lineCount < maxLines) {
      String line;
      if (remaining.length() <= maxLineLength) {
        line = remaining;
        remaining = "";
      } else {
        int breakPos = findBreakPos(remaining, maxLineLength);
        line = remaining.substring(0, breakPos).trim();
        remaining = remaining.substring(breakPos).trim();
        if (remaining.startsWith(",")) {
          remaining = remaining.substring(1).trim();
        }
      }
      if (builder.length() > 0) {
        builder.append("\n");
      }
      builder.append(line);
      lineCount++;
    }
    if (!remaining.isEmpty()) {
      builder.append("\n...");
    }
    return builder.toString();
  }

  private static int findBreakPos(String text, int maxLineLength) {
    int limit = Math.min(text.length(), maxLineLength);
    for (int i = limit; i > 0; i--) {
      char ch = text.charAt(i - 1);
      if (ch == ',' || ch == ' ') {
        return i;
      }
    }
    return limit;
  }

  private static ThreadPoolExecutor createDaemonExecutor() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            2,
            4,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
              Thread thread = new Thread(runnable);
              thread.setDaemon(true);
              thread.setName("network-case-plan-runner");
              return thread;
            });
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  private static String loadDsl(String resourcePath) {
    InputStream stream =
        NetworkCaseSchemaRootCauseLocalRunnerWithPlanExample.class.getResourceAsStream(resourcePath);
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
