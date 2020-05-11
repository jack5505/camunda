/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process.filter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.data.AssigneeCandidateGroupFilterDataDto;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssigneeFilterDto extends ProcessFilterDto<AssigneeCandidateGroupFilterDataDto> {

  public AssigneeFilterDto(final AssigneeCandidateGroupFilterDataDto assigneeCandidateGroupFilterDataDto) {
    super(assigneeCandidateGroupFilterDataDto);
  }

}
