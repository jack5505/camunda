/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.upgrade.steps.schema;

import org.camunda.optimize.service.es.schema.IndexMappingCreator;
import org.camunda.optimize.upgrade.es.ESIndexAdjuster;
import org.camunda.optimize.upgrade.steps.UpgradeStep;


public class CreateIndexStep implements UpgradeStep {
  private final IndexMappingCreator index;

  public CreateIndexStep(final IndexMappingCreator index) {
    this.index = index;
  }

  @Override
  public void execute(final ESIndexAdjuster esIndexAdjuster) {
    if (index.getCreateFromTemplate()) {
      final String templateName = esIndexAdjuster.getIndexNameService()
        .getOptimizeIndexAliasForIndex(index.getIndexName());
      esIndexAdjuster.createOrUpdateTemplateWithoutAliases(index, templateName);
      esIndexAdjuster.createIndexFromTemplate(index);
    } else {
      esIndexAdjuster.createIndex(index);
    }
    esIndexAdjuster.addAlias(index);
  }
}
