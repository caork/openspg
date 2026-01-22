package com.antgroup.openspg.examples.loader;

public class GraphExplorationGraphMLLoader extends GraphMLLocalGraphLoader {
  public static final String GRAPHML_PATH = "/graphml/graph_exploration.graphml";

  @Override
  protected String getGraphMLPath() {
    return GRAPHML_PATH;
  }
}
