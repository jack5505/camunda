/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.providers;

import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import io.camunda.optimize.service.LocalizationService;
import io.camunda.optimize.service.exceptions.OptimizeUserOrGroupIdNotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;

@Provider
public class OptimizeUserOrGroupIdNotFoundExceptionMapper
    implements ExceptionMapper<OptimizeUserOrGroupIdNotFoundException> {

  private static final Logger LOG =
      org.slf4j.LoggerFactory.getLogger(OptimizeUserOrGroupIdNotFoundExceptionMapper.class);
  private final LocalizationService localizationService;

  public OptimizeUserOrGroupIdNotFoundExceptionMapper(
      @Context final LocalizationService localizationService) {
    this.localizationService = localizationService;
  }

  @Override
  public Response toResponse(final OptimizeUserOrGroupIdNotFoundException idNotFoundException) {
    LOG.info("Mapping OptimizeIdNotFoundException");

    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(getErrorResponseDto(idNotFoundException))
        .build();
  }

  private ErrorResponseDto getErrorResponseDto(
      final OptimizeUserOrGroupIdNotFoundException exception) {
    final String errorCode = exception.getErrorCode();
    final String errorMessage =
        localizationService.getDefaultLocaleMessageForApiErrorCode(errorCode);
    final String detailedErrorMessage = exception.getMessage();

    return new ErrorResponseDto(errorCode, errorMessage, detailedErrorMessage);
  }
}
