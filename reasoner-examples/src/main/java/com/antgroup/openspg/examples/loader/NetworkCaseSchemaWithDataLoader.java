package com.antgroup.openspg.examples.loader;

public class NetworkCaseSchemaWithDataLoader extends SchemaLocalGraphLoader {
  public static final String SCHEMA_PATH = "/schema/network_case_1_with_data.schema";

  @Override
  protected String getSchemaPath() {
    return SCHEMA_PATH;
  }
}
