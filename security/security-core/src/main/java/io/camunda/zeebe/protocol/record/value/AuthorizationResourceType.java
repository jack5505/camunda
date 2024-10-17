/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.protocol.record.value;

public enum AuthorizationResourceType {
  AUTHORIZATION,
  MAPPING_RULE,
  MESSAGE,
  BATCH,
  APPLICATION,
  SYSTEM,
  TENANT,
  DEPLOYMENT,
  PROCESS_DEFINITION,
  DECISION_REQUIREMENTS_DEFINITION,
  DECISION_DEFINITION,
  GROUP,
  USER,
  ROLE
}
