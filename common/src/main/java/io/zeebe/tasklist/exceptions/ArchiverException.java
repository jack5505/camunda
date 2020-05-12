/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.exceptions;

public class ArchiverException extends Exception {

  public ArchiverException() {
  }

  public ArchiverException(String message) {
    super(message);
  }
}
