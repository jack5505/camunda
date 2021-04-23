/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.webapp.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.camunda.operate.webapp.rest.exception.InvalidRequestException;

public abstract class PaginatedQuery<T extends PaginatedQuery<T>> {

  private static final int DEFAULT_PAGE_SIZE = 50;

  private SortingDto sorting;
  /**
   * Search for process instances that goes exactly after the given sort values.
   */
  private Object[] searchAfter;

  private Object[] searchAfterOrEqual;
  /**
   * Search for process instance that goes exactly before the given sort values.
   */
  private Object[] searchBefore;

  private Object[] searchBeforeOrEqual;
  /**
   * Page size.
   */
  protected Integer pageSize = DEFAULT_PAGE_SIZE;

  public SortingDto getSorting() {
    return sorting;
  }

  public T setSorting(SortingDto sorting) {
    if (sorting != null && !getValidSortByValues().contains(sorting.getSortBy())) {
      throw new InvalidRequestException("SortBy parameter has invalid value: " + sorting.getSortBy());
    }
    this.sorting = sorting;
    return (T) this;
  }

  @JsonIgnore
  protected Set<String> getValidSortByValues() {
    return new HashSet<>();
  }

  @ApiModelProperty(value= "Array of values (can be one): copy/paste of sortValues field from one of the objects.",
      example = "[1605160098477, 4629710542312628000]")
  public Object[] getSearchAfter() {
    return searchAfter;
  }

  public T setSearchAfter(final Object[] searchAfter) {
    this.searchAfter = searchAfter;
    return (T) this;
  }

  public Object[] getSearchAfterOrEqual() {
    return searchAfterOrEqual;
  }

  public T setSearchAfterOrEqual(final Object[] searchAfterOrEqual) {
    this.searchAfterOrEqual = searchAfterOrEqual;
    return (T) this;
  }

  @ApiModelProperty(value= "Array of values (can be one): copy/paste of sortValues field from one of the objects.",
      example = "[1605160098477, 4629710542312628000]")
  public Object[] getSearchBefore() {
    return searchBefore;
  }

  public T setSearchBefore(final Object[] searchBefore) {
    this.searchBefore = searchBefore;
    return (T) this;
  }

  public Object[] getSearchBeforeOrEqual() {
    return searchBeforeOrEqual;
  }

  public T setSearchBeforeOrEqual(final Object[] searchBeforeOrEqual) {
    this.searchBeforeOrEqual = searchBeforeOrEqual;
    return (T) this;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public T setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
    return (T) this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PaginatedQuery that = (PaginatedQuery) o;
    return Objects.equals(sorting, that.sorting) &&
        Arrays.equals(searchAfter, that.searchAfter) &&
        Arrays.equals(searchAfterOrEqual, that.searchAfterOrEqual) &&
        Arrays.equals(searchBefore, that.searchBefore) &&
        Arrays.equals(searchBeforeOrEqual, that.searchBeforeOrEqual) &&
        Objects.equals(pageSize, that.pageSize);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(sorting, pageSize);
    result = 31 * result + Arrays.hashCode(searchAfter);
    result = 31 * result + Arrays.hashCode(searchAfterOrEqual);
    result = 31 * result + Arrays.hashCode(searchBefore);
    result = 31 * result + Arrays.hashCode(searchBeforeOrEqual);
    return result;
  }

}
