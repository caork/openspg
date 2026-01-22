package com.antgroup.openspg.examples.loader;

public class RcaGraphMLLoader extends GraphMLLocalGraphLoader {
  public static final String GRAPHML_PATH = "/graphml/rca_root_cause.graphml";

  @Override
  protected String getGraphMLPath() {
    return GRAPHML_PATH;
  }
}
