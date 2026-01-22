package com.antgroup.openspg.examples.loader;

public class NetworkRootCauseGraphMLLoader extends GraphMLLocalGraphLoader {
  public static final String GRAPHML_PATH = "/graphml/network_root_cause.graphml";

  @Override
  protected String getGraphMLPath() {
    return GRAPHML_PATH;
  }
}
