/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.util;

import io.camunda.operate.conditions.DatabaseInfo;
import io.camunda.operate.property.OperateProperties;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionIndex;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionRequirementsIndex;
import io.camunda.webapps.schema.descriptors.operate.index.MetricIndex;
import io.camunda.webapps.schema.descriptors.operate.index.ProcessIndex;
import io.camunda.webapps.schema.descriptors.operate.template.DecisionInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.EventTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.FlowNodeInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.IncidentTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.JobTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.MessageTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.PostImporterQueueTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.SequenceFlowTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.UserTaskTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.VariableTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class IndexTemplateDescriptorsConfigurator {

  @Bean
  public DecisionIndex getDecisionIndex(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new DecisionIndex(
        operateProperties.getIndexPrefix(databaseInfo.getCurrent()),
        databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public DecisionRequirementsIndex getDecisionRequirementsIndex(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new DecisionRequirementsIndex(
        operateProperties.getIndexPrefix(databaseInfo.getCurrent()),
        databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public MetricIndex getMetricIndex(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new MetricIndex(
        operateProperties.getIndexPrefix(databaseInfo.getCurrent()),
        databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public ProcessIndex getProcessIndex(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new ProcessIndex(
        operateProperties.getIndexPrefix(databaseInfo.getCurrent()),
        databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public DecisionInstanceTemplate getDecisionInstanceTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new DecisionInstanceTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public EventTemplate getEventTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new EventTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public FlowNodeInstanceTemplate getFlowNodeInstanceTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new FlowNodeInstanceTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public IncidentTemplate getIncidentTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new IncidentTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public ListViewTemplate getListViewTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new ListViewTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public MessageTemplate getMessageTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new MessageTemplate(
        operateProperties.getIndexPrefix(databaseInfo.getCurrent()),
        databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public PostImporterQueueTemplate getPostImporterQueueTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new PostImporterQueueTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public SequenceFlowTemplate getSequenceFlowTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new SequenceFlowTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public UserTaskTemplate getUserTaskTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new UserTaskTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public JobTemplate getJobTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new JobTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }

  @Bean
  public VariableTemplate getVariableTemplate(
      final OperateProperties operateProperties,
      final DatabaseInfo databaseInfo,
      final IndexPrefixHolder indexPrefixHolder) {
    return new VariableTemplate("", databaseInfo.isElasticsearchDb()) {
      @Override
      public String getIndexPrefix() {
        return indexPrefixHolder.getIndexPrefix();
      }
    };
  }
}
