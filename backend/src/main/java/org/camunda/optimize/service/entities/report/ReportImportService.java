/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.entities.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.query.IdResponseDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportItemDto;
import org.camunda.optimize.dto.optimize.rest.ConflictedItemDto;
import org.camunda.optimize.dto.optimize.rest.DefinitionExceptionItemDto;
import org.camunda.optimize.dto.optimize.rest.DefinitionVersionResponseDto;
import org.camunda.optimize.dto.optimize.rest.ImportIndexMismatchDto;
import org.camunda.optimize.dto.optimize.rest.export.report.CombinedProcessReportDefinitionExportDto;
import org.camunda.optimize.dto.optimize.rest.export.report.ReportDefinitionExportDto;
import org.camunda.optimize.dto.optimize.rest.export.report.SingleDecisionReportDefinitionExportDto;
import org.camunda.optimize.dto.optimize.rest.export.report.SingleProcessReportDefinitionExportDto;
import org.camunda.optimize.service.DefinitionService;
import org.camunda.optimize.service.es.schema.DefaultIndexMappingCreator;
import org.camunda.optimize.service.es.schema.OptimizeIndexNameService;
import org.camunda.optimize.service.es.schema.index.report.CombinedReportIndex;
import org.camunda.optimize.service.es.schema.index.report.SingleDecisionReportIndex;
import org.camunda.optimize.service.es.schema.index.report.SingleProcessReportIndex;
import org.camunda.optimize.service.es.writer.ReportWriter;
import org.camunda.optimize.service.exceptions.OptimizeImportDefinitionDoesNotExistException;
import org.camunda.optimize.service.exceptions.OptimizeImportForbiddenException;
import org.camunda.optimize.service.exceptions.OptimizeImportIncorrectIndexVersionException;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.service.exceptions.conflict.OptimizeNonDefinitionScopeCompliantException;
import org.camunda.optimize.service.exceptions.conflict.OptimizeNonTenantScopeCompliantException;
import org.camunda.optimize.service.report.ReportService;
import org.camunda.optimize.service.security.AuthorizedCollectionService;
import org.camunda.optimize.service.security.util.definition.DataSourceDefinitionAuthorizationService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.camunda.optimize.dto.optimize.rest.export.ExportEntityType.COMBINED_REPORT;
import static org.camunda.optimize.dto.optimize.rest.export.ExportEntityType.SINGLE_DECISION_REPORT;
import static org.camunda.optimize.dto.optimize.rest.export.ExportEntityType.SINGLE_PROCESS_REPORT;
import static org.camunda.optimize.service.util.DefinitionVersionHandlingUtil.isDefinitionVersionSetToAllOrLatest;

@AllArgsConstructor
@Component
@Slf4j
public class ReportImportService {

  private final ReportService reportService;
  private final ReportWriter reportWriter;
  private final DefinitionService definitionService;
  private final DataSourceDefinitionAuthorizationService definitionAuthorizationService;
  private final AuthorizedCollectionService collectionService;
  private final OptimizeIndexNameService optimizeIndexNameService;

  public void importReportsIntoCollection(final String userId,
                                          final String collectionId,
                                          final List<ReportDefinitionExportDto> reportsToImport,
                                          final Map<String, IdResponseDto> originalIdToNewIdMap) {
    final List<ReportDefinitionExportDto> singleReportsToImport = reportsToImport.stream()
      .filter(entity -> SINGLE_PROCESS_REPORT.equals(entity.getExportEntityType())
        || SINGLE_DECISION_REPORT.equals(entity.getExportEntityType()))
      .collect(toList());
    final List<ReportDefinitionExportDto> combinedReportsToImport = reportsToImport.stream()
      .filter(entity -> COMBINED_REPORT.equals(entity.getExportEntityType()))
      .collect(toList());

    singleReportsToImport.forEach(
      reportToImport -> importReportIntoCollection(userId, collectionId, reportToImport, originalIdToNewIdMap)
    );

    combinedReportsToImport.forEach(
      reportToImport -> importReportIntoCollection(userId, collectionId, reportToImport, originalIdToNewIdMap)
    );
  }

  public void validateAllReportsOrFail(final String userId,
                                       final String collectionId,
                                       final List<ReportDefinitionExportDto> reportsToImport) {
    final Set<ImportIndexMismatchDto> indexMismatches = new HashSet<>();
    final Set<DefinitionExceptionItemDto> missingDefinitions = new HashSet<>();
    final Set<DefinitionExceptionItemDto> forbiddenDefinitions = new HashSet<>();
    final Set<ConflictedItemDto> definitionsNotInScope = new HashSet<>();
    final Set<ConflictedItemDto> tenantsNotInScope = new HashSet<>();

    reportsToImport.forEach(
      reportExportDto -> {
        try {
          validateReportOrFail(userId, collectionId, reportExportDto);
        } catch (OptimizeImportIncorrectIndexVersionException e) {
          indexMismatches.addAll(e.getMismatchingIndices());
        } catch (OptimizeImportDefinitionDoesNotExistException e) {
          missingDefinitions.addAll(e.getMissingDefinitions());
        } catch (OptimizeImportForbiddenException e) {
          forbiddenDefinitions.addAll(e.getForbiddenDefinitions());
        } catch (OptimizeNonDefinitionScopeCompliantException e) {
          definitionsNotInScope.addAll(e.getConflictedItems());
        } catch (OptimizeNonTenantScopeCompliantException e) {
          tenantsNotInScope.addAll(e.getConflictedItems());
        }
      }
    );

    if (!indexMismatches.isEmpty()) {
      throw new OptimizeImportIncorrectIndexVersionException(
        "Could not import because source and target index versions do not match for at least one report.",
        indexMismatches
      );
    }

    if (!missingDefinitions.isEmpty()) {
      throw new OptimizeImportDefinitionDoesNotExistException(
        "Could not import because at least one required definition does not exist.",
        missingDefinitions
      );
    }

    if (!forbiddenDefinitions.isEmpty()) {
      throw new OptimizeImportForbiddenException(
        String.format(
          "Could not import because user with ID [%s] is not authorized to access at least one of the required " +
            "definitions.",
          userId
        ),
        forbiddenDefinitions
      );
    }

    if (!definitionsNotInScope.isEmpty()) {
      throw new OptimizeNonDefinitionScopeCompliantException(definitionsNotInScope);
    }

    if (!tenantsNotInScope.isEmpty()) {
      throw new OptimizeNonTenantScopeCompliantException(tenantsNotInScope);
    }
  }

  private void validateReportOrFail(final String userId,
                                    final String collectionId,
                                    final ReportDefinitionExportDto reportToImport) {
    final CollectionDefinitionDto collection = Optional.ofNullable(collectionId)
      .map(collId -> collectionService.getAuthorizedCollectionDefinitionOrFail(userId, collId).getDefinitionDto())
      .orElse(null);

    switch (reportToImport.getExportEntityType()) {
      case SINGLE_PROCESS_REPORT:
        final SingleProcessReportDefinitionExportDto processExport =
          (SingleProcessReportDefinitionExportDto) reportToImport;
        validateIndexVersionOrFail(new SingleProcessReportIndex(), processExport);
        removeMissingVersionsOrFailIfNoVersionsExist(processExport);
        validateAuthorizedToAccessDefinitionOrFail(userId, processExport);
        validateCollectionScopeOrFail(collection, processExport);
        populateDefinitionXml(processExport);
        break;
      case SINGLE_DECISION_REPORT:
        final SingleDecisionReportDefinitionExportDto decisionExport =
          (SingleDecisionReportDefinitionExportDto) reportToImport;
        validateIndexVersionOrFail(new SingleDecisionReportIndex(), decisionExport);
        removeMissingVersionsOrFailIfNoVersionsExist(decisionExport);
        validateAuthorizedToAccessDefinitionOrFail(userId, decisionExport);
        validateCollectionScopeOrFail(collection, decisionExport);
        populateDefinitionXml(decisionExport);
        break;
      case COMBINED_REPORT:
        final CombinedProcessReportDefinitionExportDto combinedExport =
          (CombinedProcessReportDefinitionExportDto) reportToImport;
        validateIndexVersionOrFail(new CombinedReportIndex(), combinedExport);
        break;
      default:
        throw new OptimizeRuntimeException("Unknown report entity type: " + reportToImport.getExportEntityType());
    }
  }

  private void importReportIntoCollection(final String userId,
                                          final String collectionId,
                                          final ReportDefinitionExportDto reportToImport,
                                          final Map<String, IdResponseDto> originalIdToNewIdMap) {
    switch (reportToImport.getExportEntityType()) {
      case SINGLE_PROCESS_REPORT:
        importProcessReportIntoCollection(
          userId,
          collectionId,
          (SingleProcessReportDefinitionExportDto) reportToImport,
          originalIdToNewIdMap
        );
        break;
      case SINGLE_DECISION_REPORT:
        importDecisionReportIntoCollection(
          userId,
          collectionId,
          (SingleDecisionReportDefinitionExportDto) reportToImport,
          originalIdToNewIdMap
        );
        break;
      case COMBINED_REPORT:
        importCombinedProcessReportIntoCollection(
          userId,
          collectionId,
          (CombinedProcessReportDefinitionExportDto) reportToImport,
          originalIdToNewIdMap
        );
        break;
      default:
        throw new OptimizeRuntimeException("Unknown single report entity type: " + reportToImport.getExportEntityType());
    }
  }

  private void importProcessReportIntoCollection(final String userId,
                                                 final String collectionId,
                                                 final SingleProcessReportDefinitionExportDto reportToImport,
                                                 final Map<String, IdResponseDto> originalIdToNewIdMap) {
    final IdResponseDto newId = importReport(
      userId,
      reportToImport,
      collectionId
    );
    originalIdToNewIdMap.put(reportToImport.getId(), newId);
  }

  private void importCombinedProcessReportIntoCollection(final String userId,
                                                         final String collectionId,
                                                         final CombinedProcessReportDefinitionExportDto reportToImport,
                                                         final Map<String, IdResponseDto> originalIdToNewIdMap) {
    prepareCombinedReportForImport(reportToImport, originalIdToNewIdMap);
    final IdResponseDto newId = importReport(
      userId,
      reportToImport,
      collectionId
    );
    originalIdToNewIdMap.put(reportToImport.getId(), newId);
  }

  private void importDecisionReportIntoCollection(final String userId,
                                                  final String collectionId,
                                                  final SingleDecisionReportDefinitionExportDto reportToImport,
                                                  final Map<String, IdResponseDto> originalIdToNewIdMap) {
    final IdResponseDto newId = importReport(
      userId,
      reportToImport,
      collectionId
    );
    originalIdToNewIdMap.put(reportToImport.getId(), newId);
  }

  public IdResponseDto importReport(final String userId,
                                    final ReportDefinitionExportDto reportToImport,
                                    final String newCollectionId) {
    switch (reportToImport.getExportEntityType()) {
      case COMBINED_REPORT:
        return reportWriter.createNewCombinedReport(
          userId,
          ((CombinedProcessReportDefinitionExportDto) reportToImport).getData(),
          reportToImport.getName(),
          newCollectionId
        );
      case SINGLE_PROCESS_REPORT:
        return reportWriter.createNewSingleProcessReport(
          userId,
          ((SingleProcessReportDefinitionExportDto) reportToImport).getData(),
          reportToImport.getName(),
          newCollectionId
        );
      case SINGLE_DECISION_REPORT:
        return reportWriter.createNewSingleDecisionReport(
          userId,
          ((SingleDecisionReportDefinitionExportDto) reportToImport).getData(),
          reportToImport.getName(),
          newCollectionId
        );
      default:
        throw new IllegalStateException("Unsupported entity type: " + reportToImport.getExportEntityType());
    }
  }

  private void prepareCombinedReportForImport(final CombinedProcessReportDefinitionExportDto reportToImport,
                                              final Map<String, IdResponseDto> originalIdToNewIdMap) {
    // Map single report items within combined report to new IDs
    final List<CombinedReportItemDto> newSingleReportItems = reportToImport.getData()
      .getReports()
      .stream()
      .map(reportItem -> new CombinedReportItemDto(
        originalIdToNewIdMap.get(reportItem.getId()).getId(),
        reportItem.getColor()
      ))
      .collect(toList());
    reportToImport.getData().setReports(newSingleReportItems);
  }

  private void removeMissingVersionsOrFailIfNoVersionsExist(final SingleDecisionReportDefinitionExportDto exportDto) {
    removeMissingVersionsOrFailIfNoneExist(
      DefinitionType.DECISION,
      exportDto.getData().getDecisionDefinitionKey(),
      exportDto.getData().getDecisionDefinitionVersions(),
      exportDto.getData().getTenantIds()
    );
  }

  private void removeMissingVersionsOrFailIfNoVersionsExist(final SingleProcessReportDefinitionExportDto exportDto) {
    removeMissingVersionsOrFailIfNoneExist(
      DefinitionType.PROCESS,
      exportDto.getData().getProcessDefinitionKey(),
      exportDto.getData().getProcessDefinitionVersions(),
      exportDto.getData().getTenantIds()
    );
  }

  private void removeMissingVersionsOrFailIfNoneExist(final DefinitionType definitionType,
                                                      final String definitionKey,
                                                      final List<String> definitionVersions,
                                                      final List<String> tenantIds) {
    final List<String> requiredVersions = new ArrayList<>(definitionVersions);
    final boolean isAllOrLatest = isDefinitionVersionSetToAllOrLatest(requiredVersions);
    final List<String> existingVersions = getExistingDefinitionVersions(
      definitionType,
      definitionKey,
      tenantIds
    );
    if (!isAllOrLatest) {
      // if specific versions are required, remove all nonexistent versions
      definitionVersions.removeIf(version -> !existingVersions.contains(version));
    }
    if (isAllOrLatest && existingVersions.isEmpty()
      || !isAllOrLatest && definitionVersions.isEmpty()) {
      throw new OptimizeImportDefinitionDoesNotExistException(
        "Could not find the required definition for this report",
        Sets.newHashSet(DefinitionExceptionItemDto.builder()
                          .type(definitionType)
                          .key(definitionKey)
                          .tenantIds(tenantIds)
                          .versions(requiredVersions)
                          .build()
        )
      );
    }
  }

  private List<String> getExistingDefinitionVersions(final DefinitionType definitionType,
                                                     final String definitionKey,
                                                     final List<String> tenantIds) {
    return definitionService.getDefinitionVersions(
      definitionType,
      definitionKey,
      tenantIds
    ).stream()
      .map(DefinitionVersionResponseDto::getVersion)
      .collect(toList());
  }

  private void populateDefinitionXml(final SingleProcessReportDefinitionExportDto exportDto) {
    final Optional<ProcessDefinitionOptimizeDto> definitionWithXml = definitionService.getDefinitionWithXmlAsService(
      DefinitionType.PROCESS,
      exportDto.getData().getProcessDefinitionKey(),
      exportDto.getData().getDefinitionVersions(),
      exportDto.getData().getTenantIds()
    );
    definitionWithXml.ifPresent(def -> exportDto.getData().getConfiguration().setXml(def.getBpmn20Xml()));
  }

  private void populateDefinitionXml(final SingleDecisionReportDefinitionExportDto exportDto) {
    final Optional<DecisionDefinitionOptimizeDto> definitionWithXml = definitionService.getDefinitionWithXmlAsService(
      DefinitionType.DECISION,
      exportDto.getData().getDecisionDefinitionKey(),
      exportDto.getData().getDefinitionVersions(),
      exportDto.getData().getTenantIds()
    );
    definitionWithXml.ifPresent(def -> exportDto.getData().getConfiguration().setXml(def.getDmn10Xml()));
  }

  private void validateIndexVersionOrFail(final DefaultIndexMappingCreator targetIndex,
                                          final ReportDefinitionExportDto exportDto) {
    if (targetIndex.getVersion() != exportDto.getSourceIndexVersion()) {
      throw new OptimizeImportIncorrectIndexVersionException(
        "Could not import because source and target index versions do not match",
        Sets.newHashSet(
          ImportIndexMismatchDto.builder()
            .indexName(optimizeIndexNameService.getOptimizeIndexNameWithVersion(targetIndex))
            .sourceIndexVersion(exportDto.getSourceIndexVersion())
            .targetIndexVersion(targetIndex.getVersion())
            .build()
        )
      );
    }
  }

  private void validateAuthorizedToAccessDefinitionOrFail(final String userId,
                                                          final SingleProcessReportDefinitionExportDto exportDto) {
    validateAuthorizedToAccessDefinitionOrFail(
      userId,
      DefinitionType.PROCESS,
      exportDto.getData().getProcessDefinitionKey(),
      exportDto.getData().getTenantIds()
    );
  }

  private void validateAuthorizedToAccessDefinitionOrFail(final String userId,
                                                          final SingleDecisionReportDefinitionExportDto exportDto) {
    validateAuthorizedToAccessDefinitionOrFail(
      userId,
      DefinitionType.DECISION,
      exportDto.getData().getDecisionDefinitionKey(),
      exportDto.getData().getTenantIds()
    );
  }

  private void validateAuthorizedToAccessDefinitionOrFail(final String userId,
                                                          final DefinitionType definitionType,
                                                          final String definitionKey,
                                                          final List<String> tenantIds) {
    if (!definitionAuthorizationService.isAuthorizedToAccessDefinition(
      userId,
      definitionType,
      definitionKey,
      tenantIds
    )) {
      throw new OptimizeImportForbiddenException(
        String.format(
          "User with ID [%s] is not authorized to access the required definition.",
          userId
        ),
        Sets.newHashSet(DefinitionExceptionItemDto.builder()
                          .type(definitionType)
                          .key(definitionKey)
                          .tenantIds(tenantIds)
                          .build())
      );
    }
  }

  private void validateCollectionScopeOrFail(@Nullable final CollectionDefinitionDto collection,
                                             final SingleProcessReportDefinitionExportDto reportToImport) {
    if (collection != null) {
      reportService.ensureCompliesWithCollectionScope(
        reportToImport.getData().getDefinitions(), DefinitionType.PROCESS, collection
      );
    }
  }

  private void validateCollectionScopeOrFail(@Nullable final CollectionDefinitionDto collection,
                                             final SingleDecisionReportDefinitionExportDto reportToImport) {
    if (collection != null) {
      reportService.ensureCompliesWithCollectionScope(
        reportToImport.getData().getDefinitions(), DefinitionType.DECISION, collection
      );
    }
  }
}
