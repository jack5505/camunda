/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.dashboard.filter.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.data.DashboardVariableFilterSubDataDto;
import org.camunda.optimize.dto.optimize.query.variable.VariableType;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DashboardIntegerVariableFilterDataDto extends DashboardVariableFilterDataDto {
  protected List<String> defaultValues;

  protected DashboardIntegerVariableFilterDataDto() {
    this(null, new DashboardVariableFilterSubDataDto(null, null, false));
  }

  public DashboardIntegerVariableFilterDataDto(final String name, final DashboardVariableFilterSubDataDto data) {
    this(name, data, null);
  }

  public DashboardIntegerVariableFilterDataDto(final String name, final DashboardVariableFilterSubDataDto data,
                                               final List<String> defaultValues) {
    super(VariableType.INTEGER, name, data);
    this.defaultValues = defaultValues;
  }
}
