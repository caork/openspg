package com.antgroup.openspg.examples.loader;

import com.antgroup.openspg.reasoner.common.graph.edge.IEdge;
import com.antgroup.openspg.reasoner.common.graph.property.IProperty;
import com.antgroup.openspg.reasoner.common.graph.vertex.IVertex;
import com.antgroup.openspg.reasoner.lube.catalog.Catalog;
import com.antgroup.openspg.reasoner.lube.catalog.impl.PropertyGraphCatalog;
import com.antgroup.openspg.reasoner.runner.local.load.graph.AbstractLocalGraphLoader;
import com.antgroup.openspg.reasoner.util.Convert2ScalaUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SchemaLocalGraphLoader extends AbstractLocalGraphLoader {
  private static final Pattern TYPE_DEF_PATTERN =
      Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)(?:\\([^)]*\\))?\\s*:\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*$");

  protected abstract String getSchemaPath();

  @Override
  public List<IVertex<String, IProperty>> genVertexList() {
    return new ArrayList<>();
  }

  @Override
  public List<IEdge<String, IProperty>> genEdgeList() {
    return new ArrayList<>();
  }

  @Override
  public String getDemoGraph() {
    return readGraphBlock(getSchemaPath());
  }

  public static Catalog buildCatalog(String schemaPath) {
    return buildCatalog(schemaPath, null);
  }

  public static Catalog buildCatalog(String schemaPath, String dsl) {
    Map<String, Set<String>> schema = parseSchema(schemaPath).toCatalogSchema();
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

  private static SchemaDefinition parseSchema(String path) {
    try (InputStream stream = openSchema(path)) {
      if (stream == null) {
        throw new IllegalStateException("Missing schema file: " + path);
      }
      return parseSchema(stream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load schema: " + path, e);
    }
  }

  private static InputStream openSchema(String path) throws IOException {
    InputStream stream = SchemaLocalGraphLoader.class.getResourceAsStream(path);
    if (stream != null) {
      return stream;
    }
    if (Files.exists(Paths.get(path))) {
      return Files.newInputStream(Paths.get(path));
    }
    return null;
  }

  private static SchemaDefinition parseSchema(InputStream stream) throws IOException {
    SchemaDefinition schema = new SchemaDefinition();
    SchemaType currentType = null;
    Section section = Section.NONE;
    boolean inRuleBlock = false;
    boolean inDataBlock = false;
    int dataDepth = 0;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        if (inRuleBlock) {
          if (trimmed.contains("]]")) {
            inRuleBlock = false;
          }
          continue;
        }
        if (inDataBlock) {
          dataDepth += countBraces(trimmed);
          if (dataDepth <= 0) {
            inDataBlock = false;
          }
          continue;
        }
        if (trimmed.startsWith("rule:") && trimmed.contains("[[")) {
          inRuleBlock = true;
          continue;
        }
        if (isGraphBlockStart(trimmed)) {
          inDataBlock = true;
          dataDepth = countBraces(trimmed);
          continue;
        }
        if (line.equals(trimmed)) {
          Matcher matcher = TYPE_DEF_PATTERN.matcher(trimmed);
          if (matcher.matches()) {
            currentType = schema.getOrCreate(matcher.group(1));
            section = Section.NONE;
            continue;
          }
        }
        if ("properties:".equals(trimmed)) {
          section = Section.PROPERTIES;
          continue;
        }
        if ("relations:".equals(trimmed)) {
          section = Section.RELATIONS;
          continue;
        }
        if (trimmed.startsWith("desc:")) {
          continue;
        }
        if (currentType == null) {
          continue;
        }

        NameTypePair pair = parseNameType(trimmed);
        if (pair == null) {
          continue;
        }
        if (section == Section.PROPERTIES) {
          currentType.properties.add(pair.name);
        } else if (section == Section.RELATIONS && pair.type != null) {
          currentType.relations.put(pair.name, pair.type);
        }
      }
    }
    return schema;
  }

  private static String readGraphBlock(String path) {
    try (InputStream stream = openSchema(path)) {
      if (stream == null) {
        return "";
      }
      return readGraphBlock(stream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load graph data: " + path, e);
    }
  }

  private static String readGraphBlock(InputStream stream) throws IOException {
    StringBuilder builder = new StringBuilder();
    boolean inDataBlock = false;
    int depth = 0;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (!inDataBlock) {
          if (isGraphBlockStart(trimmed)) {
            inDataBlock = true;
            depth = countBraces(trimmed);
            builder.append(line).append('\n');
            if (depth <= 0) {
              break;
            }
          }
          continue;
        }
        builder.append(line).append('\n');
        depth += countBraces(trimmed);
        if (depth <= 0) {
          break;
        }
      }
    }
    return builder.toString().trim();
  }

  private static boolean isGraphBlockStart(String trimmed) {
    String lower = trimmed.toLowerCase();
    return lower.startsWith("graph") && trimmed.contains("{");
  }

  private static int countBraces(String line) {
    int count = 0;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '{') {
        count++;
      } else if (c == '}') {
        count--;
      }
    }
    return count;
  }

  private static NameTypePair parseNameType(String line) {
    int colonIndex = line.indexOf(':');
    if (colonIndex <= 0) {
      return null;
    }
    String namePart = line.substring(0, colonIndex).trim();
    if (namePart.isEmpty()) {
      return null;
    }
    int parenIndex = namePart.indexOf('(');
    if (parenIndex > 0) {
      namePart = namePart.substring(0, parenIndex).trim();
    }
    if (namePart.isEmpty()) {
      return null;
    }
    String typePart = line.substring(colonIndex + 1).trim();
    if (typePart.isEmpty()) {
      return new NameTypePair(namePart, null);
    }
    int spaceIndex = typePart.indexOf(' ');
    if (spaceIndex > 0) {
      typePart = typePart.substring(0, spaceIndex).trim();
    }
    if (typePart.isEmpty()) {
      typePart = null;
    }
    return new NameTypePair(namePart, typePart);
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

  private enum Section {
    NONE,
    PROPERTIES,
    RELATIONS
  }

  private static final class SchemaDefinition {
    private final Map<String, SchemaType> types = new LinkedHashMap<>();

    private SchemaType getOrCreate(String name) {
      return types.computeIfAbsent(name, SchemaType::new);
    }

    private Map<String, Set<String>> toCatalogSchema() {
      Map<String, Set<String>> schema = new HashMap<>();
      for (SchemaType type : types.values()) {
        schema.put(type.name, new LinkedHashSet<>(type.properties));
      }
      for (SchemaType type : types.values()) {
        for (Map.Entry<String, String> relation : type.relations.entrySet()) {
          String edgeType = type.name + "_" + relation.getKey() + "_" + relation.getValue();
          schema.computeIfAbsent(edgeType, k -> new LinkedHashSet<>());
        }
      }
      return schema;
    }
  }

  private static final class SchemaType {
    private final String name;
    private final Set<String> properties = new LinkedHashSet<>();
    private final Map<String, String> relations = new LinkedHashMap<>();

    private SchemaType(String name) {
      this.name = name;
    }
  }

  private static final class NameTypePair {
    private final String name;
    private final String type;

    private NameTypePair(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }
}
