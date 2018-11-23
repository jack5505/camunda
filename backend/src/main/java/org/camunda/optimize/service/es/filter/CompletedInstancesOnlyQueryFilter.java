package org.camunda.optimize.service.es.filter;

import org.camunda.optimize.dto.optimize.query.report.single.filter.data.CompletedInstancesOnlyFilterDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.RunningInstancesOnlyFilterDataDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.END_DATE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;

@Component
public class CompletedInstancesOnlyQueryFilter implements QueryFilter<CompletedInstancesOnlyFilterDataDto> {

  public void addFilters(BoolQueryBuilder query, List<CompletedInstancesOnlyFilterDataDto> runningOnly) {
    if (runningOnly != null && !runningOnly.isEmpty()) {
      List<QueryBuilder> filters = query.filter();

      BoolQueryBuilder onlyRunningInstances =
        boolQuery()
          .must(existsQuery(END_DATE));

      filters.add(onlyRunningInstances);
    }
  }

}
