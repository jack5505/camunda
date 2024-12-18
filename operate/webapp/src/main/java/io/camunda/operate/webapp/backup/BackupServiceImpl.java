/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.backup;

import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.backup.Prio1Backup;
import io.camunda.operate.schema.backup.Prio2Backup;
import io.camunda.operate.schema.backup.Prio3Backup;
import io.camunda.operate.schema.backup.Prio4Backup;
import io.camunda.operate.webapp.rest.exception.InvalidRequestException;
import io.camunda.webapps.backup.BackupRepository;
import io.camunda.webapps.backup.BackupService;
import io.camunda.webapps.backup.GetBackupStateResponseDto;
import io.camunda.webapps.backup.TakeBackupRequestDto;
import io.camunda.webapps.backup.TakeBackupResponseDto;
import io.camunda.webapps.schema.descriptors.IndexDescriptor;
import io.camunda.webapps.schema.descriptors.IndexTemplateDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class BackupServiceImpl implements BackupService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupServiceImpl.class);
  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
  private final Queue<SnapshotRequest> requestsQueue = new ConcurrentLinkedQueue<>();

  private final List<Prio1Backup> prio1BackupIndices;

  private final List<Prio2Backup> prio2BackupTemplates;

  private final List<Prio3Backup> prio3BackupTemplates;

  private final List<Prio4Backup> prio4BackupIndices;

  private final OperateProperties operateProperties;

  private final BackupRepository repository;

  private String[][] indexPatternsOrdered;

  public BackupServiceImpl(
      @Qualifier("backupThreadPoolExecutor") final ThreadPoolTaskExecutor threadPoolTaskExecutor,
      final List<Prio1Backup> prio1BackupIndices,
      final List<Prio2Backup> prio2BackupTemplates,
      final List<Prio3Backup> prio3BackupTemplates,
      final List<Prio4Backup> prio4BackupIndices,
      final OperateProperties operateProperties,
      final BackupRepository repository) {
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    this.prio1BackupIndices = prio1BackupIndices;
    this.prio2BackupTemplates = prio2BackupTemplates;
    this.prio3BackupTemplates = prio3BackupTemplates;
    this.prio4BackupIndices = prio4BackupIndices;
    this.repository = repository;
    this.operateProperties = operateProperties;
  }

  @Override
  public void deleteBackup(final Long backupId) {
    repository.validateRepositoryExists(getRepositoryName());
    final String repositoryName = getRepositoryName();
    final int count = getIndexPatternsOrdered().length;
    final String version = getCurrentOperateVersion();
    for (int index = 0; index < count; index++) {
      final String snapshotName =
          new Metadata()
              .setVersion(version)
              .setPartCount(count)
              .setPartNo(index + 1)
              .setBackupId(backupId)
              .buildSnapshotName();
      repository.deleteSnapshot(repositoryName, snapshotName);
    }
  }

  @Override
  public TakeBackupResponseDto takeBackup(final TakeBackupRequestDto request) {
    repository.validateRepositoryExists(getRepositoryName());
    repository.validateNoDuplicateBackupId(getRepositoryName(), request.getBackupId());
    if (!requestsQueue.isEmpty()) {
      throw new InvalidRequestException("Another backup is running at the moment");
    } // TODO remove duplicate
    synchronized (requestsQueue) {
      if (!requestsQueue.isEmpty()) {
        throw new InvalidRequestException("Another backup is running at the moment");
      }
      return scheduleSnapshots(request);
    }
  }

  @Override
  public GetBackupStateResponseDto getBackupState(final Long backupId) {
    return repository.getBackupState(getRepositoryName(), backupId);
  }

  @Override
  public List<GetBackupStateResponseDto> getBackups() {
    return repository.getBackups(getRepositoryName());
  }

  TakeBackupResponseDto scheduleSnapshots(final TakeBackupRequestDto request) {
    final String repositoryName = getRepositoryName();
    final int count = getIndexPatternsOrdered().length;
    final List<String> snapshotNames = new ArrayList<>();
    final String version = getCurrentOperateVersion();
    for (int index = 0; index < count; index++) {
      final List<String> indexPattern = Arrays.asList(getIndexPatternsOrdered()[index]);
      final Metadata metadata =
          new Metadata()
              .setVersion(version)
              .setPartCount(count)
              .setPartNo(index + 1)
              .setBackupId(request.getBackupId());
      final String snapshotName = metadata.buildSnapshotName();
      final SnapshotRequest snapshotRequest =
          new SnapshotRequest(repositoryName, snapshotName, indexPattern, metadata.toCommon());

      requestsQueue.offer(snapshotRequest);
      LOGGER.debug("Snapshot scheduled: {}", snapshotName);
      snapshotNames.add(snapshotName);
    }
    // schedule next snapshot
    scheduleNextSnapshot();
    return new TakeBackupResponseDto().setScheduledSnapshots(snapshotNames);
  }

  void scheduleNextSnapshot() {
    final SnapshotRequest nextRequest = requestsQueue.poll();
    if (nextRequest != null) {
      threadPoolTaskExecutor.submit(
          () ->
              repository.executeSnapshotting(
                  nextRequest, this::scheduleNextSnapshot, requestsQueue::clear));
      LOGGER.debug("Snapshot picked for execution: {}", nextRequest);
    }
  }

  private String getRepositoryName() {
    return operateProperties.getBackup().getRepositoryName();
  }

  private String[][] getIndexPatternsOrdered() {
    if (indexPatternsOrdered == null) {
      indexPatternsOrdered =
          new String[][] {
            prio1BackupIndices.stream()
                .map(index -> ((IndexDescriptor) index).getFullQualifiedName())
                .toArray(String[]::new),
            prio2BackupTemplates.stream()
                .map(index -> ((IndexTemplateDescriptor) index).getFullQualifiedName())
                .toArray(String[]::new),
            // dated indices
            prio2BackupTemplates.stream()
                .map(
                    index ->
                        new String[] {
                          ((IndexTemplateDescriptor) index).getFullQualifiedName() + "*",
                          "-" + ((IndexTemplateDescriptor) index).getFullQualifiedName()
                        })
                .flatMap(Arrays::stream)
                .toArray(String[]::new),
            prio3BackupTemplates.stream()
                .map(index -> ((IndexTemplateDescriptor) index).getFullQualifiedName())
                .toArray(String[]::new),
            // dated indices
            prio3BackupTemplates.stream()
                .map(
                    index ->
                        new String[] {
                          ((IndexTemplateDescriptor) index).getFullQualifiedName() + "*",
                          "-" + ((IndexTemplateDescriptor) index).getFullQualifiedName()
                        })
                .flatMap(Arrays::stream)
                .toArray(String[]::new),
            prio4BackupIndices.stream()
                .map(index -> ((IndexDescriptor) index).getFullQualifiedName())
                .toArray(String[]::new),
          };
    }
    return indexPatternsOrdered;
  }

  private String getCurrentOperateVersion() {
    return operateProperties.getVersion().toLowerCase();
  }
}
