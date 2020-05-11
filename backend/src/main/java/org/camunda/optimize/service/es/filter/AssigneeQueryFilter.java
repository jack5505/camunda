/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.filter;

import org.apache.commons.collections4.CollectionUtils;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.data.AssigneeCandidateGroupFilterDataDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.camunda.optimize.service.es.filter.AssigneeOrCandidateGroupFilterQueryUtil.createAssigneeFilterQuery;

@Component
public class AssigneeQueryFilter implements QueryFilter<AssigneeCandidateGroupFilterDataDto> {

  public void addFilters(final BoolQueryBuilder query,
                         final List<AssigneeCandidateGroupFilterDataDto> assigneeFilters) {
    if (!CollectionUtils.isEmpty(assigneeFilters)) {
      final List<QueryBuilder> filters = query.filter();
      for (AssigneeCandidateGroupFilterDataDto assigneeFilter : assigneeFilters) {
        filters.add(createAssigneeFilterQuery(assigneeFilter));
      }
    }
  }

}
