package com.antgroup.openspg.examples.loader;

public class RiskCausalFusionGraphMLLoader extends GraphMLLocalGraphLoader {
  public static final String GRAPHML_PATH = "/graphml/risk_causal_fusion.graphml";

  @Override
  protected String getGraphMLPath() {
    return GRAPHML_PATH;
  }
}
