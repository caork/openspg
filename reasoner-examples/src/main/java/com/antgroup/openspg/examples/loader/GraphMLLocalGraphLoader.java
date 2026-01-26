package com.antgroup.openspg.examples.loader;

import com.antgroup.openspg.reasoner.common.graph.edge.IEdge;
import com.antgroup.openspg.reasoner.common.graph.property.IProperty;
import com.antgroup.openspg.reasoner.common.graph.vertex.IVertex;
import com.antgroup.openspg.reasoner.common.graph.vertex.IVertexId;
import com.antgroup.openspg.reasoner.common.constants.Constants;
import com.antgroup.openspg.reasoner.lube.catalog.Catalog;
import com.antgroup.openspg.reasoner.lube.catalog.impl.PropertyGraphCatalog;
import com.antgroup.openspg.reasoner.runner.local.load.graph.AbstractLocalGraphLoader;
import com.antgroup.openspg.reasoner.util.Convert2ScalaUtil;
import com.antgroup.openspg.reasoner.utils.RunnerUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import scala.Tuple2;

public abstract class GraphMLLocalGraphLoader extends AbstractLocalGraphLoader {
  protected abstract String getGraphMLPath();

  @Override
  public List<IVertex<String, IProperty>> genVertexList() {
    return parseGraphML(getGraphMLPath()).vertices;
  }

  @Override
  public List<IEdge<String, IProperty>> genEdgeList() {
    return parseGraphML(getGraphMLPath()).edges;
  }

  @Override
  public Tuple2<List<IVertex<IVertexId, IProperty>>, List<IEdge<IVertexId, IProperty>>>
      getGraphData() {
    GraphMLGraph graph = parseGraphML(getGraphMLPath());
    return generateGraphData(graph.vertices, graph.edges);
  }

  public static Catalog buildCatalog(String graphMLPath) {
    return buildCatalog(graphMLPath, null);
  }

  public static Catalog buildCatalog(String graphMLPath, String dsl) {
    GraphMLLocalGraphLoader loader =
        new GraphMLLocalGraphLoader() {
          @Override
          protected String getGraphMLPath() {
            return graphMLPath;
          }
        };
    GraphMLGraph graph = loader.parseGraphML(graphMLPath);
    Map<String, Set<String>> schema = inferSchema(graph);
    if (dsl != null && !dsl.trim().isEmpty()) {
      addDefineEdges(schema, dsl);
    }
    Map<String, scala.collection.immutable.Set<String>> scalaSchema = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : schema.entrySet()) {
      scalaSchema.put(entry.getKey(), Convert2ScalaUtil.toScalaImmutableSet(entry.getValue()));
    }
    Catalog catalog = new PropertyGraphCatalog(Convert2ScalaUtil.toScalaImmutableMap(scalaSchema));
    catalog.init();
    return catalog;
  }

  private GraphMLGraph parseGraphML(String path) {
    try (InputStream stream = openGraphML(path)) {
      if (stream == null) {
        throw new IllegalStateException("Missing GraphML file: " + path);
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(stream);

      Map<String, KeyDef> keyDefs = parseKeyDefs(document);
      List<IVertex<String, IProperty>> vertices = parseVertices(document, keyDefs);
      List<IEdge<String, IProperty>> edges = parseEdges(document, keyDefs);
      return new GraphMLGraph(vertices, edges);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load GraphML: " + path, e);
    }
  }

  private static InputStream openGraphML(String path) throws IOException {
    InputStream stream = GraphMLLocalGraphLoader.class.getResourceAsStream(path);
    if (stream != null) {
      return stream;
    }
    if (Files.exists(Paths.get(path))) {
      return Files.newInputStream(Paths.get(path));
    }
    return null;
  }

  private static Map<String, KeyDef> parseKeyDefs(Document document) {
    Map<String, KeyDef> defs = new HashMap<>();
    NodeList keys = document.getElementsByTagNameNS("*", "key");
    for (int i = 0; i < keys.getLength(); i++) {
      Node node = keys.item(i);
      if (!(node instanceof Element)) {
        continue;
      }
      Element key = (Element) node;
      String id = key.getAttribute("id");
      if (id == null || id.isEmpty()) {
        continue;
      }
      String name = key.getAttribute("attr.name");
      String type = key.getAttribute("attr.type");
      defs.put(id, new KeyDef(name, type));
    }
    return defs;
  }

  private List<IVertex<String, IProperty>> parseVertices(
      Document document, Map<String, KeyDef> keyDefs) {
    List<IVertex<String, IProperty>> vertices = new ArrayList<>();
    NodeList nodes = document.getElementsByTagNameNS("*", "node");
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (!(node instanceof Element)) {
        continue;
      }
      Element element = (Element) node;
      String id = element.getAttribute("id");
      Map<String, Object> props = parseDataElements(element, keyDefs);
      String label = extractLabel(props, "node", id);
      vertices.add(constructionVertex(id, label, toKeyValueArray(props)));
    }
    return vertices;
  }

  private List<IEdge<String, IProperty>> parseEdges(
      Document document, Map<String, KeyDef> keyDefs) {
    List<IEdge<String, IProperty>> edges = new ArrayList<>();
    NodeList edgeNodes = document.getElementsByTagNameNS("*", "edge");
    for (int i = 0; i < edgeNodes.getLength(); i++) {
      Node node = edgeNodes.item(i);
      if (!(node instanceof Element)) {
        continue;
      }
      Element element = (Element) node;
      String source = element.getAttribute("source");
      String target = element.getAttribute("target");
      Map<String, Object> props = parseDataElements(element, keyDefs);
      String label = extractLabel(props, "edge", source + "->" + target);
      edges.add(constructionEdge(source, label, target, toKeyValueArray(props)));
    }
    return edges;
  }

  private static Map<String, Object> parseDataElements(
      Element element, Map<String, KeyDef> keyDefs) {
    Map<String, Object> props = new LinkedHashMap<>();
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (!(child instanceof Element)) {
        continue;
      }
      Element data = (Element) child;
      if (!"data".equals(data.getLocalName())) {
        continue;
      }
      String key = data.getAttribute("key");
      String value = data.getTextContent() == null ? "" : data.getTextContent().trim();
      KeyDef def = keyDefs.get(key);
      String name = def != null && def.name != null && !def.name.isEmpty() ? def.name : key;
      String type = def != null ? def.type : null;
      props.put(name, parseValue(value, type));
    }
    return props;
  }

  private static Object parseValue(String value, String type) {
    if (type == null || type.isEmpty() || value.isEmpty()) {
      return value;
    }
    switch (type) {
      case "int":
        return Integer.parseInt(value);
      case "long":
        return Long.parseLong(value);
      case "double":
        return Double.parseDouble(value);
      case "float":
        return Double.parseDouble(value);
      case "boolean":
        return Boolean.parseBoolean(value);
      default:
        return value;
    }
  }

  private static String extractLabel(Map<String, Object> props, String kind, String id) {
    Object label = props.remove("label");
    if (label == null) {
      label = props.remove("type");
    }
    if (label == null) {
      throw new IllegalStateException("Missing " + kind + " label/type for " + id);
    }
    return String.valueOf(label);
  }

  private static Object[] toKeyValueArray(Map<String, Object> props) {
    List<Object> kvs = new ArrayList<>();
    for (Map.Entry<String, Object> entry : props.entrySet()) {
      kvs.add(entry.getKey());
      kvs.add(entry.getValue());
    }
    return kvs.toArray(new Object[0]);
  }

  private static Map<String, Set<String>> inferSchema(GraphMLGraph graph) {
    Map<String, IVertex<String, IProperty>> vertexMap = new HashMap<>();
    for (IVertex<String, IProperty> vertex : graph.vertices) {
      String label = RunnerUtil.getVertexTypeFromProperty(vertex.getValue());
      vertexMap.put(vertex.getId(), vertex);
      vertexMap.put(vertex.getId() + "_" + label, vertex);
    }

    Map<String, Set<String>> schema = new HashMap<>();
    for (IVertex<String, IProperty> vertex : graph.vertices) {
      String label = RunnerUtil.getVertexTypeFromProperty(vertex.getValue());
      Set<String> props = schema.computeIfAbsent(label, k -> new LinkedHashSet<>());
      addPropertyKeys(props, vertex.getValue());
    }

    for (IEdge<String, IProperty> edge : graph.edges) {
      String sourceType = getVertexType(vertexMap, edge.getSourceId());
      String targetType = getVertexType(vertexMap, edge.getTargetId());
      String edgeType = sourceType + "_" + edge.getType() + "_" + targetType;
      Set<String> props = schema.computeIfAbsent(edgeType, k -> new LinkedHashSet<>());
      addPropertyKeys(props, edge.getValue());
    }
    return schema;
  }

  private static void addDefineEdges(Map<String, Set<String>> schema, String dsl) {
    String label =
        "[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*(?:/`[^`]+`)?";
    String edge = "[A-Za-z_][A-Za-z0-9_]*";
    Pattern pattern =
        Pattern.compile(
            "Define\\s*\\(\\s*\\w+\\s*:\\s*(" + label + ")\\s*\\)\\s*-\\[\\s*\\w*\\s*:"
                + "\\s*(" + edge + ")\\s*\\]\\s*->\\s*\\(\\s*\\w+\\s*:\\s*(" + label + ")\\s*\\)",
            Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(dsl);
    while (matcher.find()) {
      String sourceType = matcher.group(1);
      String edgeLabel = matcher.group(2);
      String targetType = matcher.group(3);
      String edgeType = sourceType + "_" + edgeLabel + "_" + targetType;
      schema.computeIfAbsent(edgeType, k -> new LinkedHashSet<>());
    }
  }

  private static void addPropertyKeys(Set<String> target, IProperty property) {
    if (property == null) {
      return;
    }
    for (String key : property.getKeySet()) {
      if (isInternalKey(key)) {
        continue;
      }
      target.add(key);
    }
  }

  private static boolean isInternalKey(String key) {
    return Constants.CONTEXT_LABEL.equals(key)
        || Constants.EDGE_FROM_ID_KEY.equals(key)
        || Constants.EDGE_TO_ID_KEY.equals(key)
        || Constants.EDGE_FROM_ID_TYPE_KEY.equals(key)
        || Constants.EDGE_TO_ID_TYPE_KEY.equals(key);
  }

  private static String getVertexType(
      Map<String, IVertex<String, IProperty>> vertexMap, String id) {
    IVertex<String, IProperty> vertex = vertexMap.get(id);
    if (vertex == null) {
      throw new IllegalStateException("Missing vertex for id: " + id);
    }
    return RunnerUtil.getVertexTypeFromProperty(vertex.getValue());
  }

  private static class GraphMLGraph {
    private final List<IVertex<String, IProperty>> vertices;
    private final List<IEdge<String, IProperty>> edges;

    private GraphMLGraph(
        List<IVertex<String, IProperty>> vertices, List<IEdge<String, IProperty>> edges) {
      this.vertices = vertices;
      this.edges = edges;
    }
  }

  private static class KeyDef {
    private final String name;
    private final String type;

    private KeyDef(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }
}
