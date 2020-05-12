/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.es;

import static io.zeebe.tasklist.util.CollectionUtil.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import io.zeebe.tasklist.es.schema.indices.IndexDescriptor;
import io.zeebe.tasklist.es.schema.templates.TemplateDescriptor;
import io.zeebe.tasklist.exceptions.TasklistRuntimeException;
import io.zeebe.tasklist.property.TasklistProperties;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("schemaManager")
@Profile("!test")
public class ElasticsearchSchemaManager {

  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSchemaManager.class);

  @Autowired
  private List<IndexDescriptor> indexDescriptors;

  @Autowired
  private List<TemplateDescriptor> templateDescriptors;

  @Autowired
  protected RestHighLevelClient esClient;

  @Autowired
  protected TasklistProperties tasklistProperties;
    
  @PostConstruct
  public boolean initializeSchema() {
    if (tasklistProperties.getElasticsearch().isCreateSchema() && !schemaAlreadyExists()) {
      logger.info("Elasticsearch schema is empty. Indices will be created.");
      return createSchema();
    } else {
      logger.info("Elasticsearch schema won't be created, it either already exist, or schema creation is disabled in configuration.");
      return false;
    }
  }
  
  public boolean createSchema() {
     return createTemplates() && createIndices();
  }

  public boolean createIndices() {
    return !map(indexDescriptors,this::createIndex).contains(false);
  }

  public boolean createTemplates() {
    return !map(templateDescriptors,this::createTemplate).contains(false);
  }
  
  protected boolean createIndex(IndexDescriptor indexDescriptor) {
    final Map<String, Object> indexDescription = readJSONFileToMap(indexDescriptor.getFileName());
    // Adjust aliases in case of other configured indexNames, e.g. non-default prefix
    indexDescription.put("aliases", Collections.singletonMap(indexDescriptor.getAlias(), Collections.EMPTY_MAP));
    
    return createIndex(new CreateIndexRequest(indexDescriptor.getIndexName()).source(indexDescription), indexDescriptor.getIndexName());
  }
  
  protected boolean createTemplate(TemplateDescriptor templateDescriptor) {
    final Map<String, Object> template = readJSONFileToMap(templateDescriptor.getFileName());
    
    // Adjust prefixes and aliases in case of other configured indexNames, e.g. non-default prefix
    template.put("index_patterns", Collections.singletonList(templateDescriptor.getIndexPattern()));
    template.put("aliases", Collections.singletonMap(templateDescriptor.getAlias(), Collections.EMPTY_MAP));

    return putIndexTemplate(new PutIndexTemplateRequest(templateDescriptor.getTemplateName()).source(template),
        templateDescriptor.getTemplateName())
        // This is necessary, otherwise we won't find indexes at startup
        && createIndex(new CreateIndexRequest(templateDescriptor.getMainIndexName()), templateDescriptor.getMainIndexName());
  }
  
  public boolean schemaAlreadyExists() {
    try {
      String indexName = indexDescriptors.get(0).getAlias();
      return esClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
    } catch (IOException e) {
      final String message = String.format("Exception occurred, while checking schema existence: %s", e.getMessage());
      logger.error(message, e);
      throw new TasklistRuntimeException(message, e);
    }
  }
  
  protected Map<String, Object> readJSONFileToMap(final String filename) {
    final Map<String, Object> result;
    try (InputStream inputStream = ElasticsearchSchemaManager.class.getResourceAsStream(filename)) {
      if (inputStream != null) {
        result = XContentHelper.convertToMap(XContentType.JSON.xContent(), inputStream, true);
      } else {
        throw new TasklistRuntimeException("Failed to find " + filename + " in classpath ");
      }
    } catch (IOException e) {
      throw new TasklistRuntimeException("Failed to load file " + filename + " from classpath ", e);
    }
    return result;
  }
  
  protected boolean createIndex(final CreateIndexRequest createIndexRequest, String indexName) {
    try {
      boolean acknowledged = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT).isAcknowledged();
      if (acknowledged) {
        logger.debug("Index [{}] was successfully created", indexName);
      }
      return acknowledged;
    } catch (IOException e) {
      throw new TasklistRuntimeException("Failed to create index ", e);
    }
  }
  
  
  protected boolean putIndexTemplate(final PutIndexTemplateRequest putIndexTemplateRequest, String templateName) {
    try {
      boolean acknowledged = esClient.indices().putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT).isAcknowledged();
      if (acknowledged) {
        logger.debug("Template [{}] was successfully created", templateName);
      }
      return acknowledged;
    } catch (IOException e) {
      throw new TasklistRuntimeException("Failed to put index template", e);
    }
  }
  
}

