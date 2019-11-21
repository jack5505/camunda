/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.modules.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedBy;
import org.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import org.camunda.optimize.dto.optimize.query.sorting.SortingDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.raw.RawDataDecisionReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.NumberResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.ReportMapResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.hyper.ReportHyperMapResultDto;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Data
@Accessors(chain = true)
public class CompositeCommandResult {


  private SortingDto sorting = new SortingDto(null, null);
  private boolean keyIsOfNumericType = false;

  private List<GroupByResult> groups = new ArrayList<>();
  private Boolean isComplete = true;

  public void setGroup(GroupByResult groupByResult) {
    this.groups = singletonList(groupByResult);
  }

  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  @Data
  public static class GroupByResult {
    private String key;
    private String label;
    private List<DistributedByResult> distributions;

    public static GroupByResult createResultWithEmptyValue(final String key) {
      return new GroupByResult(key, null, singletonList(DistributedByResult.createResultWithEmptyValue(null)));
    }

    public static GroupByResult createEmptyGroupBy(List<DistributedByResult> distributions) {
      return new GroupByResult(null, null, distributions);
    }

    public static GroupByResult createGroupByResult(final String key, final String label,
                                                    final List<DistributedByResult> distributions) {
      return new GroupByResult(key, label, distributions);
    }

    public static GroupByResult createGroupByResult(final String key, final List<DistributedByResult> distributions) {
      return new GroupByResult(key, null, distributions);
    }

    public String getLabel() {
      return label != null && !label.isEmpty() ? label : key;
    }
  }

  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  @Data
  @EqualsAndHashCode
  public static class DistributedByResult {

    private String key;
    private String label;
    private ViewResult viewResult;

    public static DistributedByResult createResultWithEmptyValue(String key) {
      return new DistributedByResult(key, null, new ViewResult());
    }

    public static DistributedByResult createEmptyDistributedBy(ViewResult viewResult) {
      return new DistributedByResult(null, null, viewResult);
    }

    public static DistributedByResult createDistributedByResult(String key, String label, ViewResult viewResult) {
      return new DistributedByResult(key, label, viewResult);
    }

    public String getLabel() {
      return label != null && !label.isEmpty() ? label : key;
    }

    public Long getValueAsLong() {
      return this.getViewResult().getNumber();
    }

    public MapResultEntryDto getValueAsMapResultEntry() {
      return new MapResultEntryDto(this.key, getValueAsLong(), this.label);
    }
  }

  @NoArgsConstructor
  @Accessors(chain = true)
  @Data
  public static class ViewResult {

    private Long number;
    private RawDataProcessReportResultDto processRawData;
    private RawDataDecisionReportResultDto decisionRawData;
  }

  public ReportHyperMapResultDto transformToHyperMap() {
    ReportHyperMapResultDto resultDto = new ReportHyperMapResultDto();
    resultDto.setIsComplete(this.isComplete);

    for (GroupByResult group : groups) {
      List<MapResultEntryDto> distribution = group.distributions.stream()
        .map(DistributedByResult::getValueAsMapResultEntry)
        .sorted(getSortingComparator(sorting, keyIsOfNumericType))
        .collect(Collectors.toList());
      resultDto.getData().add(new HyperMapResultEntryDto(group.getKey(), distribution, group.getLabel()));
    }
    return resultDto;
  }

  public ReportMapResultDto transformToMap() {
    ReportMapResultDto resultDto = new ReportMapResultDto();
    resultDto.setIsComplete(this.isComplete);
    List<MapResultEntryDto> mapData = new ArrayList<>();
    for (GroupByResult group : groups) {
      final List<DistributedByResult> distributions = group.getDistributions();
      if (distributions.size() == 1) {
        final Long value = distributions.get(0).getValueAsLong();
        mapData.add(new MapResultEntryDto(group.getKey(), value, group.getLabel()));
      } else {
        throw new OptimizeRuntimeException(createErrorMessage(ReportMapResultDto.class, DistributedBy.class));
      }
    }
    mapData.sort(getSortingComparator(sorting, keyIsOfNumericType));
    resultDto.setData(mapData);
    return resultDto;
  }

  public NumberResultDto transformToNumber() {
    NumberResultDto numberResultDto = new NumberResultDto();
    final List<GroupByResult> groups = this.groups;
    if (groups.size() == 1) {
      final List<DistributedByResult> distributions = groups.get(0).distributions;
      if (distributions.size() == 1) {
        final Long value = distributions.get(0).getViewResult().getNumber();
        numberResultDto.setData(value);
        return numberResultDto;
      } else {
        throw new OptimizeRuntimeException(createErrorMessage(NumberResultDto.class, DistributedBy.class));
      }
    } else {
      throw new OptimizeRuntimeException(createErrorMessage(NumberResultDto.class, GroupByResult.class));
    }
  }

  public RawDataProcessReportResultDto transformToProcessRawData() {
    final List<GroupByResult> groups = this.groups;
    if (groups.size() == 1) {
      final List<DistributedByResult> distributions = groups.get(0).distributions;
      if (distributions.size() == 1) {
        return distributions.get(0).getViewResult().getProcessRawData();
      } else {
        throw new OptimizeRuntimeException(createErrorMessage(
          RawDataProcessReportResultDto.class,
          DistributedBy.class
        ));
      }
    } else {
      throw new OptimizeRuntimeException(createErrorMessage(RawDataProcessReportResultDto.class, GroupByResult.class));
    }
  }

  public RawDataDecisionReportResultDto transformToDecisionRawData() {
    final List<GroupByResult> groups = this.groups;
    if (groups.size() == 1) {
      final List<DistributedByResult> distributions = groups.get(0).distributions;
      if (distributions.size() == 1) {
        return distributions.get(0).getViewResult().getDecisionRawData();
      } else {
        throw new OptimizeRuntimeException(createErrorMessage(
          RawDataDecisionReportResultDto.class,
          DistributedBy.class
        ));
      }
    } else {
      throw new OptimizeRuntimeException(createErrorMessage(RawDataDecisionReportResultDto.class, GroupByResult.class));
    }
  }

  private Comparator<MapResultEntryDto> getSortingComparator(
    final SortingDto sorting,
    final boolean keyIsOfNumericType) {

    final String sortBy = sorting.getBy().orElse(SortingDto.SORT_BY_KEY);
    final SortOrder sortOrder = sorting.getOrder().orElse(SortOrder.ASC);

    final Function<MapResultEntryDto, Comparable> valueToSortByExtractor;
    switch (sortBy) {
      default:
      case SortingDto.SORT_BY_KEY:
        valueToSortByExtractor = keyIsOfNumericType?
          entry -> Double.valueOf(entry.getKey()) : entry -> entry.getKey().toLowerCase();
        break;
      case SortingDto.SORT_BY_VALUE:
        valueToSortByExtractor = MapResultEntryDto::getValue;
        break;
      case SortingDto.SORT_BY_LABEL:
        valueToSortByExtractor = entry -> entry.getLabel().toLowerCase();
        break;
    }

    final Comparator<MapResultEntryDto> comparator;
    switch (sortOrder) {
      case DESC:
        comparator = Comparator.comparing(valueToSortByExtractor, Comparator.nullsLast(Comparator.reverseOrder()));
        break;
      default:
      case ASC:
        comparator = Comparator.comparing(valueToSortByExtractor, Comparator.nullsLast(Comparator.naturalOrder()));
        break;
    }

    return comparator;
  }

  private String createErrorMessage(Class resultClass, Class resultPartClass) {
    return String.format(
      "Could not transform the result of command to a %s since the result has not the right structure. For %s the %s " +
        "result is supposed to contain just one value!",
      resultClass.getSimpleName(),
      resultClass.getSimpleName(),
      resultPartClass.getSimpleName()
    );
  }
}
