package org.camunda.optimize.service.es.report.command.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.optimize.dto.optimize.importing.ProcessInstanceDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.raw.RawDataProcessInstanceDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.raw.RawDataSingleReportResultDto;
import org.camunda.optimize.dto.optimize.query.variable.value.VariableInstanceDto;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RawDataSingleReportResultDtoMapper {
  private static final Logger logger = LoggerFactory.getLogger(RawDataSingleReportResultDtoMapper.class);

  private final Long recordLimit;

  public RawDataSingleReportResultDtoMapper(final Long recordLimit) {
    this.recordLimit = recordLimit;
  }

  public RawDataSingleReportResultDto mapFrom(final SearchResponse searchResponse, final ObjectMapper objectMapper) {
    List<RawDataProcessInstanceDto> rawData = new ArrayList<>();
    Set<String> allVariableNames = new HashSet<>();
    SearchHits searchHits = searchResponse.getHits();

    Arrays.stream(searchHits.getHits())
        .limit(recordLimit)
        .forEach(hit -> {
          final String sourceAsString = hit.getSourceAsString();
          try {
            final ProcessInstanceDto processInstanceDto = objectMapper.readValue(sourceAsString, ProcessInstanceDto.class);

            Map<String, Object> variables = getVariables(processInstanceDto, objectMapper);
            allVariableNames.addAll(variables.keySet());
            RawDataProcessInstanceDto dataEntry = convertToRawDataEntry(processInstanceDto, variables);
            rawData.add(dataEntry);
          } catch (IOException e) {
            logger.error("can't map process instance {}", sourceAsString, e);
          }
        });

    ensureEveryRawDataInstanceContainsAllVariableNames(rawData, allVariableNames);

    return createResult(rawData, searchHits.getTotalHits());
  }

  private void ensureEveryRawDataInstanceContainsAllVariableNames(final List<RawDataProcessInstanceDto> rawData,
                                                                  final Set<String> allVariableNames) {
    rawData
        .forEach(data -> allVariableNames
            .forEach(varName -> data.getVariables().putIfAbsent(varName, ""))
        );
  }

  private RawDataProcessInstanceDto convertToRawDataEntry(final ProcessInstanceDto processInstanceDto,
                                                          final Map<String, Object> variables) {
    RawDataProcessInstanceDto rawDataInstance = new RawDataProcessInstanceDto();
    rawDataInstance.setProcessInstanceId(processInstanceDto.getProcessInstanceId());
    rawDataInstance.setProcessDefinitionId(processInstanceDto.getProcessDefinitionId());
    rawDataInstance.setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey());
    rawDataInstance.setStartDate(processInstanceDto.getStartDate());
    rawDataInstance.setEndDate(processInstanceDto.getEndDate());
    rawDataInstance.setEngineName(processInstanceDto.getEngine());
    rawDataInstance.setBusinessKey(processInstanceDto.getBusinessKey());

    rawDataInstance.setVariables(variables);
    return rawDataInstance;
  }

  private Map<String, Object> getVariables(final ProcessInstanceDto processInstanceDto, final ObjectMapper objectMapper) {
    Map<String, Object> result = new TreeMap<>();

    for (VariableInstanceDto instance : processInstanceDto.obtainAllVariables()) {
      if (instance.getName() != null) {
        result.put(instance.getName(), instance.getValue());
      } else {
        try {
          logger.debug("Found variable with null name []", objectMapper.writeValueAsString(instance));
        } catch (JsonProcessingException e) {
          //nothing to do
        }
      }

    }
    return result;
  }

  private RawDataSingleReportResultDto createResult(final List<RawDataProcessInstanceDto> limitedRawDataResult,
                                                    final Long totalHits) {
    RawDataSingleReportResultDto result = new RawDataSingleReportResultDto();
    result.setResult(limitedRawDataResult);
    result.setProcessInstanceCount(totalHits);
    return result;
  }

}
