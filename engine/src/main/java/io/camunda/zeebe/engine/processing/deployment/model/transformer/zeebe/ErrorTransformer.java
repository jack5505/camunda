/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.deployment.model.transformer.zeebe;

import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableError;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeError;

public class ErrorTransformer {

  public void transform(final ExecutableError executableElement, final ZeebeError zeebeError) {

    if (zeebeError == null) {
      return;
    }

    executableElement.setErrorCodeVariable(zeebeError.getErrorCodeVariable());
    executableElement.setErrorMessageVariable(zeebeError.getErrorMessageVariable());
  }
}
