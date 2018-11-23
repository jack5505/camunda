package org.camunda.optimize.service.es.schema.type;

import org.camunda.optimize.service.es.schema.StrictTypeMappingCreator;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public abstract class AbstractReportType extends StrictTypeMappingCreator {

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String LAST_MODIFIED = "lastModified";
  public static final String CREATED = "created";
  public static final String OWNER = "owner";
  public static final String LAST_MODIFIER = "lastModifier";

  public static final String REPORT_TYPE = "reportType";
  public static final String DATA = "data";

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
     XContentBuilder newBuilder = xContentBuilder
      .startObject(ID)
        .field("type", "keyword")
      .endObject()
      .startObject(NAME)
        .field("type", "keyword")
      .endObject()
      .startObject(LAST_MODIFIED)
        .field("type", "date")
              .field("format",configurationService.getOptimizeDateFormat())
      .endObject()
      .startObject(CREATED)
        .field("type", "date")
              .field("format",configurationService.getOptimizeDateFormat())
      .endObject()
      .startObject(OWNER)
        .field("type", "keyword")
      .endObject()
      .startObject(LAST_MODIFIER)
        .field("type", "keyword")
      .endObject()
      .startObject(REPORT_TYPE)
        .field("type", "keyword")
      .endObject();
     newBuilder = addDataField(newBuilder);
     return newBuilder;
  }

  protected abstract XContentBuilder addDataField(XContentBuilder xContentBuilder) throws IOException;

}
