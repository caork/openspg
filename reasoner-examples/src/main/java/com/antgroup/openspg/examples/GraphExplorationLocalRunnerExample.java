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

public class  GraphExplorationLocalRunnerExample {
  public static void main(String[] args) {
    String dsl = loadDsl("/kgdsl/graph_exploration.kgdsl");

    LocalReasonerTask task = new LocalReasonerTask();
    task.setDsl(dsl);
    task.setGraphLoadClass(GraphExplorationGraphLoader.class.getName());
    task.setStartIdList(
        Lists.newArrayList(new Tuple2<>("ACC-1", "Account"), new Tuple2<>("ACC-2", "Account")));

    Map<String, scala.collection.immutable.Set<String>> schema = new HashMap<>();
    schema.put(
        "Account", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name", "riskLevel")));
    schema.put("Person", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name")));
    schema.put("Device", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "type")));
    schema.put("Ip", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "ip")));
    schema.put(
        "Transaction",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "amount", "channel")));
    schema.put(
        "Merchant",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name", "category")));
    schema.put("City", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet("id", "name")));

    schema.put("Account_ownedBy_Person", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("Account_loginWith_Device", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("Device_bindIp_Ip", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put("Account_transfer_Account", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "Account_makeTxn_Transaction", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "Transaction_toMerchant_Merchant",
        Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));
    schema.put(
        "Merchant_locatedIn_City", Convert2ScalaUtil.toScalaImmutableSet(Sets.newHashSet()));

    Catalog catalog = new PropertyGraphCatalog(Convert2ScalaUtil.toScalaImmutableMap(schema));
    catalog.init();
    task.setCatalog(catalog);

    Map<String, Object> params = new HashMap<>();
    params.put(Constants.SPG_REASONER_LUBE_SUBQUERY_ENABLE, true);
    params.put(ConfigKey.KG_REASONER_BINARY_PROPERTY, "false");
    task.setParams(params);

    LocalReasonerRunner runner = new LocalReasonerRunner();
    LocalReasonerResult result = runner.run(task);
    System.out.println(result);
  }

  private static String loadDsl(String resourcePath) {
    InputStream stream = GraphExplorationLocalRunnerExample.class.getResourceAsStream(resourcePath);
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

  public static class GraphExplorationGraphLoader extends AbstractLocalGraphLoader {
    @Override
    public List<IVertex<String, IProperty>> genVertexList() {
      return Lists.newArrayList(
          constructionVertex("ACC-1", "Account", "name", "acct-1", "riskLevel", "HIGH"),
          constructionVertex("ACC-2", "Account", "name", "acct-2", "riskLevel", "HIGH"),
          constructionVertex("ACC-3", "Account", "name", "acct-3", "riskLevel", "MEDIUM"),
          constructionVertex("ACC-4", "Account", "name", "acct-4", "riskLevel", "LOW"),
          constructionVertex("ACC-5", "Account", "name", "acct-5", "riskLevel", "LOW"),
          constructionVertex("P-1", "Person", "name", "Alice"),
          constructionVertex("P-2", "Person", "name", "Bob"),
          constructionVertex("DEV-1", "Device", "type", "MOBILE"),
          constructionVertex("DEV-2", "Device", "type", "WEB"),
          constructionVertex("DEV-3", "Device", "type", "MOBILE"),
          constructionVertex("IP-1", "Ip", "ip", "10.0.0.1"),
          constructionVertex("IP-2", "Ip", "ip", "10.0.0.2"),
          constructionVertex("TX-1", "Transaction", "amount", 900, "channel", "POS"),
          constructionVertex("TX-2", "Transaction", "amount", 1200, "channel", "WEB"),
          constructionVertex("TX-3", "Transaction", "amount", 5000, "channel", "APP"),
          constructionVertex("TX-4", "Transaction", "amount", 300, "channel", "POS"),
          constructionVertex("M-1", "Merchant", "name", "GrocerOne", "category", "GROCERY"),
          constructionVertex("M-2", "Merchant", "name", "GameHub", "category", "GAMING"),
          constructionVertex("CITY-1", "City", "name", "Hangzhou"),
          constructionVertex("CITY-2", "City", "name", "Singapore"));
    }

    @Override
    public List<IEdge<String, IProperty>> genEdgeList() {
      return Lists.newArrayList(
          constructionEdge("ACC-1", "ownedBy", "P-1"),
          constructionEdge("ACC-2", "ownedBy", "P-2"),
          constructionEdge("ACC-1", "loginWith", "DEV-1"),
          constructionEdge("ACC-2", "loginWith", "DEV-1"),
          constructionEdge("ACC-3", "loginWith", "DEV-2"),
          constructionEdge("ACC-4", "loginWith", "DEV-3"),
          constructionEdge("DEV-1", "bindIp", "IP-1"),
          constructionEdge("DEV-2", "bindIp", "IP-2"),
          constructionEdge("DEV-3", "bindIp", "IP-1"),
          constructionEdge("ACC-1", "transfer", "ACC-3"),
          constructionEdge("ACC-3", "transfer", "ACC-4"),
          constructionEdge("ACC-2", "transfer", "ACC-3"),
          constructionEdge("ACC-4", "transfer", "ACC-5"),
          constructionEdge("ACC-1", "makeTxn", "TX-1"),
          constructionEdge("ACC-2", "makeTxn", "TX-2"),
          constructionEdge("ACC-3", "makeTxn", "TX-3"),
          constructionEdge("ACC-4", "makeTxn", "TX-4"),
          constructionEdge("TX-1", "toMerchant", "M-1"),
          constructionEdge("TX-2", "toMerchant", "M-2"),
          constructionEdge("TX-3", "toMerchant", "M-2"),
          constructionEdge("TX-4", "toMerchant", "M-1"),
          constructionEdge("M-1", "locatedIn", "CITY-1"),
          constructionEdge("M-2", "locatedIn", "CITY-2"));
    }
  }
}
