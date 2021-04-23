/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.webapp.rest;

import static org.camunda.operate.webapp.rest.ProcessInstanceRestService.PROCESS_INSTANCE_URL;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.camunda.operate.Metrics;
import org.camunda.operate.entities.BatchOperationEntity;
import org.camunda.operate.entities.FlowNodeState;
import org.camunda.operate.entities.OperationType;
import org.camunda.operate.entities.SequenceFlowEntity;
import org.camunda.operate.util.CollectionUtil;
import org.camunda.operate.webapp.es.reader.ActivityStatisticsReader;
import org.camunda.operate.webapp.es.reader.FlowNodeInstanceReader;
import org.camunda.operate.webapp.es.reader.IncidentReader;
import org.camunda.operate.webapp.es.reader.ListViewReader;
import org.camunda.operate.webapp.es.reader.ProcessInstanceReader;
import org.camunda.operate.webapp.es.reader.SequenceFlowReader;
import org.camunda.operate.webapp.es.reader.VariableReader;
import org.camunda.operate.webapp.es.writer.BatchOperationWriter;
import org.camunda.operate.webapp.rest.dto.ActivityStatisticsDto;
import org.camunda.operate.webapp.rest.dto.ProcessInstanceCoreStatisticsDto;
import org.camunda.operate.webapp.rest.dto.SequenceFlowDto;
import org.camunda.operate.webapp.rest.dto.VariableDto;
import org.camunda.operate.webapp.rest.dto.VariableRequestDto;
import org.camunda.operate.webapp.rest.dto.incidents.IncidentResponseDto;
import org.camunda.operate.webapp.rest.dto.listview.ListViewProcessInstanceDto;
import org.camunda.operate.webapp.rest.dto.listview.ListViewQueryDto;
import org.camunda.operate.webapp.rest.dto.listview.ListViewRequestDto;
import org.camunda.operate.webapp.rest.dto.listview.ListViewResponseDto;
import org.camunda.operate.webapp.rest.dto.metadata.FlowNodeMetadataDto;
import org.camunda.operate.webapp.rest.dto.metadata.FlowNodeMetadataRequestDto;
import org.camunda.operate.webapp.rest.dto.operation.CreateBatchOperationRequestDto;
import org.camunda.operate.webapp.rest.dto.operation.CreateOperationRequestDto;
import org.camunda.operate.webapp.rest.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"Process instances"})
@SwaggerDefinition(tags = {
  @Tag(name = "Process instances", description = "Process instances")
})
@RestController
@RequestMapping(value = PROCESS_INSTANCE_URL)
public class ProcessInstanceRestService {

  public static final String PROCESS_INSTANCE_URL = "/api/process-instances";

  @Autowired
  private BatchOperationWriter batchOperationWriter;

  @Autowired
  private ProcessInstanceReader processInstanceReader;

  @Autowired
  private ListViewReader listViewReader;

  @Autowired
  private ActivityStatisticsReader activityStatisticsReader;

  @Autowired
  private IncidentReader incidentReader;

  @Autowired
  private VariableReader variableReader;

  @Autowired
  private FlowNodeInstanceReader flowNodeInstanceReader;

  @Autowired
  private SequenceFlowReader sequenceFlowReader;

  @ApiOperation("Query process instances by different parameters")
  @PostMapping
  @Timed(value = Metrics.TIMER_NAME_QUERY, extraTags = {Metrics.TAG_KEY_NAME, Metrics.TAG_VALUE_PROCESSINSTANCES}, description = "How long does it take to retrieve the processinstances by query.")
  public ListViewResponseDto queryProcessInstances(
      @RequestBody ListViewRequestDto processInstanceRequest) {
    if (processInstanceRequest.getQuery() == null) {
      throw new InvalidRequestException("Query must be provided.");
    }
    if (processInstanceRequest.getQuery().getProcessVersion() != null && processInstanceRequest.getQuery().getBpmnProcessId() == null) {
      throw new InvalidRequestException("BpmnProcessId must be provided in request, when process version is not null.");
    }
    return listViewReader.queryProcessInstances(processInstanceRequest);
  }

  @ApiOperation("Perform single operation on an instance (async)")
  @PostMapping("/{id}/operation")
  public BatchOperationEntity operation(@PathVariable String id,
      @RequestBody CreateOperationRequestDto operationRequest) {
    validateOperationRequest(operationRequest);
    return batchOperationWriter.scheduleSingleOperation(Long.valueOf(id), operationRequest);
  }

  private void validateBatchOperationRequest(CreateBatchOperationRequestDto batchOperationRequest) {
    if (batchOperationRequest.getQuery() == null) {
      throw new InvalidRequestException("List view query must be defined.");
    }
    if (batchOperationRequest.getOperationType() == null) {
      throw new InvalidRequestException("Operation type must be defined.");
    }
    if (batchOperationRequest.getOperationType().equals(OperationType.UPDATE_VARIABLE)) {
      throw new InvalidRequestException("For variable update use \"Create operation for one process instance\" endpoint.");
    }
  }

  private void validateOperationRequest(CreateOperationRequestDto operationRequest) {
    if (operationRequest.getOperationType() == null) {
      throw new InvalidRequestException("Operation type must be defined.");
    }
    if (operationRequest.getOperationType().equals(OperationType.UPDATE_VARIABLE)
      && (operationRequest.getVariableScopeId() == null || operationRequest.getVariableName() == null || operationRequest.getVariableName().isEmpty()
        || operationRequest.getVariableValue() == null)) {
        throw new InvalidRequestException("ScopeId, name and value must be defined for UPDATE_VARIABLE operation.");
    }
  }

  @ApiOperation("Create batch operation based on filter")
  @PostMapping("/batch-operation")
  public BatchOperationEntity createBatchOperation(@RequestBody CreateBatchOperationRequestDto batchOperationRequest) {
    validateBatchOperationRequest(batchOperationRequest);
    return batchOperationWriter.scheduleBatchOperation(batchOperationRequest);
  }

  @ApiOperation("Get process instance by id")
  @GetMapping("/{id}")
  public ListViewProcessInstanceDto queryProcessInstanceById(@PathVariable String id) {
    return processInstanceReader.getProcessInstanceWithOperationsByKey(Long.valueOf(id));
  }

  @ApiOperation("Get incidents by process instance id")
  @GetMapping("/{id}/incidents")
  public IncidentResponseDto queryIncidentsByProcessInstanceId(@PathVariable String id) {
    return incidentReader.getIncidentsByProcessInstanceKey(Long.valueOf(id));
  }

  @ApiOperation("Get sequence flows by process instance id")
  @GetMapping("/{id}/sequence-flows")
  public List<SequenceFlowDto> querySequenceFlowsByProcessInstanceId(@PathVariable String id) {
    final List<SequenceFlowEntity> sequenceFlows = sequenceFlowReader.getSequenceFlowsByProcessInstanceKey(Long.valueOf(id));
    return SequenceFlowDto.createFrom(sequenceFlows);
  }

  @ApiOperation("Get variables by process instance id and scope id")
  @GetMapping("/{processInstanceId}/variables")
  @Deprecated
  public List<VariableDto> getVariables(@PathVariable String processInstanceId, @RequestParam String scopeId) {
    return variableReader.getVariablesOld(Long.valueOf(processInstanceId), Long.valueOf(scopeId));
  }

  @ApiOperation("Get variables by process instance id and scope id")
  @PostMapping("/{processInstanceId}/variables-new")
  public List<VariableDto> getVariables(
      @PathVariable String processInstanceId, @RequestBody VariableRequestDto variableRequest) {
    validateRequest(variableRequest);
    return variableReader.getVariables(processInstanceId, variableRequest);
  }

  @ApiOperation("Get flow node states by process instance id")
  @GetMapping("/{processInstanceId}/flow-node-states")
  public Map<String, FlowNodeState> getFlowNodeStates(@PathVariable String processInstanceId) {
    return flowNodeInstanceReader.getFlowNodeStates(processInstanceId);
  }

  @ApiOperation("Get flow node metadata.")
  @PostMapping("/{processInstanceId}/flow-node-metadata")
  public FlowNodeMetadataDto getFlowNodeMetadata(@PathVariable String processInstanceId,
      @RequestBody FlowNodeMetadataRequestDto request) {
    validateRequest(request);
    return flowNodeInstanceReader.getFlowNodeMetadata(processInstanceId, request);
  }

  private void validateRequest(final VariableRequestDto request) {
    if (request.getScopeId() == null) {
      throw new InvalidRequestException("ScopeId must be specifies in the request.");
    }
  }

  private void validateRequest(final FlowNodeMetadataRequestDto request) {
    if (request.getFlowNodeId() == null && request.getFlowNodeType() ==null && request.getFlowNodeInstanceId()==null) {
      throw new InvalidRequestException("At least flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
    if (request.getFlowNodeId() != null && request.getFlowNodeInstanceId() != null) {
      throw new InvalidRequestException("Only one of flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
  }

  @ApiOperation("Get activity instance statistics")
  @PostMapping(path = "/statistics")
  public Collection<ActivityStatisticsDto> getStatistics(@RequestBody ListViewQueryDto query) {
    final List<Long> processDefinitionKeys = CollectionUtil.toSafeListOfLongs(query.getProcessIds());
    final String bpmnProcessId = query.getBpmnProcessId();
    final Integer processVersion = query.getProcessVersion();

    if ( (processDefinitionKeys != null && processDefinitionKeys.size() == 1) == (bpmnProcessId != null && processVersion != null) ) {
      throw new InvalidRequestException("Exactly one process must be specified in the request (via processIds or bpmnProcessId/version).");
    }
    return activityStatisticsReader.getActivityStatistics(query);
  }

  @ApiOperation("Get process instance core statistics (aggregations)")
  @GetMapping(path = "/core-statistics")
  @Timed(value = Metrics.TIMER_NAME_QUERY, extraTags = {Metrics.TAG_KEY_NAME,Metrics.TAG_VALUE_CORESTATISTICS},description = "How long does it take to retrieve the core statistics.")
  public ProcessInstanceCoreStatisticsDto getCoreStatistics() {
    return processInstanceReader.getCoreStatistics();
  }

}
