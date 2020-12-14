/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.optimize.dto.optimize.GroupDto;
import org.camunda.optimize.dto.optimize.query.IdentitySearchResultResponseDto;
import org.camunda.optimize.dto.optimize.query.definition.AssigneeCandidateGroupSearchRequestDto;
import org.camunda.optimize.dto.optimize.query.definition.AssigneeRequestDto;
import org.camunda.optimize.rest.providers.Secured;
import org.camunda.optimize.service.AssigneeCandidateGroupService;
import org.camunda.optimize.service.security.SessionService;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.camunda.optimize.rest.CandidateGroupRestService.CANDIDATE_GROUP_RESOURCE_PATH;

@AllArgsConstructor
@Path(CANDIDATE_GROUP_RESOURCE_PATH)
@Component
@Slf4j
public class CandidateGroupRestService {

  public static final String CANDIDATE_GROUP_RESOURCE_PATH = "/candidateGroup";
  public static final String CANDIDATE_GROUP_SEARCH_SUB_PATH = "/search";

  private final SessionService sessionService;
  private final AssigneeCandidateGroupService assigneeCandidateGroupService;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<GroupDto> getCandidateGroupsByIds(@QueryParam("idIn") final String commaSeparatedIdn) {
    if (StringUtils.isEmpty(commaSeparatedIdn)) {
      return Collections.emptyList();
    }
    return assigneeCandidateGroupService.getCandidateGroupsByIds(Arrays.asList(commaSeparatedIdn.split(",")));
  }

  @POST
  @Path(CANDIDATE_GROUP_SEARCH_SUB_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  public IdentitySearchResultResponseDto searchCandidateGroups(@Context final ContainerRequestContext requestContext,
                                                               @Valid final AssigneeCandidateGroupSearchRequestDto requestDto) {
    final String userId = sessionService.getRequestUserOrFailNotAuthorized(requestContext);
    return assigneeCandidateGroupService.searchForCandidateGroupsAsUser(userId, requestDto);
  }

  @POST
  @Path("/values")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Secured
  public List<String> getCandidateGroupsForProcessDefinition(@Context ContainerRequestContext requestContext,
                                                             AssigneeRequestDto requestDto) {
    String userId = sessionService.getRequestUserOrFailNotAuthorized(requestContext);
    return assigneeCandidateGroupService.getAllCandidateGroups(userId, requestDto);
  }
}
