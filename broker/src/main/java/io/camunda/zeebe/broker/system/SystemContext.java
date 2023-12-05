/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.system;

import static io.camunda.zeebe.broker.system.partitions.impl.AsyncSnapshotDirector.MINIMUM_SNAPSHOT_PERIOD;

import io.atomix.cluster.AtomixCluster;
import io.camunda.identity.sdk.IdentityConfiguration;
import io.camunda.zeebe.backup.azure.AzureBackupStore;
import io.camunda.zeebe.backup.gcs.GcsBackupStore;
import io.camunda.zeebe.backup.s3.S3BackupStore;
import io.camunda.zeebe.broker.Loggers;
import io.camunda.zeebe.broker.system.configuration.BrokerCfg;
import io.camunda.zeebe.broker.system.configuration.ClusterCfg;
import io.camunda.zeebe.broker.system.configuration.DataCfg;
import io.camunda.zeebe.broker.system.configuration.DiskCfg.FreeSpaceCfg;
import io.camunda.zeebe.broker.system.configuration.ExperimentalCfg;
import io.camunda.zeebe.broker.system.configuration.SecurityCfg;
import io.camunda.zeebe.broker.system.configuration.backup.AzureBackupStoreConfig;
import io.camunda.zeebe.broker.system.configuration.backup.BackupStoreCfg;
import io.camunda.zeebe.broker.system.configuration.backup.GcsBackupStoreConfig;
import io.camunda.zeebe.broker.system.configuration.backup.S3BackupStoreConfig;
import io.camunda.zeebe.broker.system.configuration.partitioning.FixedPartitionCfg;
import io.camunda.zeebe.broker.system.configuration.partitioning.Scheme;
import io.camunda.zeebe.gateway.impl.broker.BrokerClient;
import io.camunda.zeebe.scheduler.ActorScheduler;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

public final class SystemContext {

  public static final Logger LOG = Loggers.SYSTEM_LOGGER;
  private static final String BROKER_ID_LOG_PROPERTY = "broker-id";
  private static final String SNAPSHOT_PERIOD_ERROR_MSG =
      "Snapshot period %s needs to be larger then or equals to one minute.";
  private static final String MAX_BATCH_SIZE_ERROR_MSG =
      "Expected to have an append batch size maximum which is non negative and smaller then '%d', but was '%s'.";

  private final BrokerCfg brokerCfg;
  private final IdentityConfiguration identityConfiguration;
  private Map<String, String> diagnosticContext;
  private final ActorScheduler scheduler;
  private final AtomixCluster cluster;
  private final BrokerClient brokerClient;

  public SystemContext(
      final BrokerCfg brokerCfg,
      final IdentityConfiguration identityConfiguration,
      final ActorScheduler scheduler,
      final AtomixCluster cluster,
      final BrokerClient brokerClient) {
    this.brokerCfg = brokerCfg;
    this.identityConfiguration = identityConfiguration;
    this.scheduler = scheduler;
    this.cluster = cluster;
    this.brokerClient = brokerClient;
    initSystemContext();
  }

  public SystemContext(
      final BrokerCfg brokerCfg,
      final ActorScheduler scheduler,
      final AtomixCluster cluster,
      final BrokerClient brokerClient) {
    this(brokerCfg, null, scheduler, cluster, brokerClient);
  }

  private void initSystemContext() {
    validateConfiguration();

    final var cluster = brokerCfg.getCluster();
    final String brokerId = String.format("Broker-%d", cluster.getNodeId());

    diagnosticContext = Collections.singletonMap(BROKER_ID_LOG_PROPERTY, brokerId);
  }

  private void validateConfiguration() {
    final ClusterCfg cluster = brokerCfg.getCluster();

    validateDataConfig(brokerCfg.getData());

    validateExperimentalConfigs(cluster, brokerCfg.getExperimental());

    final var security = brokerCfg.getNetwork().getSecurity();
    if (security.isEnabled()) {
      validateNetworkSecurityConfig(security);
    }
  }

  private void validateExperimentalConfigs(
      final ClusterCfg cluster, final ExperimentalCfg experimental) {
    final var maxAppendBatchSize = experimental.getMaxAppendBatchSize();
    if (maxAppendBatchSize.isNegative() || maxAppendBatchSize.toBytes() >= Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          String.format(MAX_BATCH_SIZE_ERROR_MSG, Integer.MAX_VALUE, maxAppendBatchSize));
    }

    final var partitioningConfig = experimental.getPartitioning();
    if (partitioningConfig.getScheme() == Scheme.FIXED) {
      validateFixedPartitioningScheme(cluster, experimental);
    }
  }

  private void validateDataConfig(final DataCfg dataCfg) {
    final var snapshotPeriod = dataCfg.getSnapshotPeriod();
    if (snapshotPeriod.isNegative() || snapshotPeriod.minus(MINIMUM_SNAPSHOT_PERIOD).isNegative()) {
      throw new IllegalArgumentException(String.format(SNAPSHOT_PERIOD_ERROR_MSG, snapshotPeriod));
    }

    if (dataCfg.getDisk().isEnableMonitoring()) {
      try {
        final FreeSpaceCfg freeSpaceCfg = dataCfg.getDisk().getFreeSpace();
        final var processingFreeSpace = freeSpaceCfg.getProcessing().toBytes();
        final var replicationFreeSpace = freeSpaceCfg.getReplication().toBytes();
        if (processingFreeSpace <= replicationFreeSpace) {
          throw new IllegalArgumentException(
              "Minimum free space for processing (%d) must be greater than minimum free space for replication (%d). Configured values are %s"
                  .formatted(processingFreeSpace, replicationFreeSpace, freeSpaceCfg));
        }
      } catch (final Exception e) {
        throw new InvalidConfigurationException("Failed to parse disk monitoring configuration", e);
      }
    }

    validateBackupCfg(dataCfg.getBackup());
  }

  private void validateBackupCfg(final BackupStoreCfg backup) {
    try {
      switch (backup.getStore()) {
        case NONE -> LOG.warn("No backup store is configured. Backups will not be taken");
        case S3 -> S3BackupStore.validateConfig(S3BackupStoreConfig.toStoreConfig(backup.getS3()));
        case GCS -> GcsBackupStore.validateConfig(
            GcsBackupStoreConfig.toStoreConfig(backup.getGcs()));
        case AZURE -> AzureBackupStore.validateConfig(
            AzureBackupStoreConfig.toStoreConfig(backup.getAzure()));
        default -> throw new UnsupportedOperationException(
            "Does not support validating configuration of backup store %s"
                .formatted(backup.getStore()));
      }
    } catch (final Exception e) {
      throw new InvalidConfigurationException(
          "Failed configuring backup store %s".formatted(backup.getStore()), e);
    }
  }

  private void validateFixedPartitioningScheme(
      final ClusterCfg cluster, final ExperimentalCfg experimental) {
    final var partitioning = experimental.getPartitioning();
    final var partitions = partitioning.getFixed();
    final var replicationFactor = cluster.getReplicationFactor();
    final var partitionsCount = cluster.getPartitionsCount();

    final var partitionMembers = new HashMap<Integer, Set<Integer>>();
    for (final var partition : partitions) {
      final var members =
          validateFixedPartitionMembers(
              cluster, partition, cluster.getRaft().isEnablePriorityElection());
      partitionMembers.put(partition.getPartitionId(), members);
    }

    for (int partitionId = 1; partitionId <= partitionsCount; partitionId++) {
      final var members = partitionMembers.getOrDefault(partitionId, Collections.emptySet());
      if (members.size() < replicationFactor) {
        throw new IllegalArgumentException(
            String.format(
                "Expected fixed partition scheme to define configurations for all partitions such "
                    + "that they have %d replicas, but partition %d has %d configured replicas: %s",
                replicationFactor, partitionId, members.size(), members));
      }
    }
  }

  private Set<Integer> validateFixedPartitionMembers(
      final ClusterCfg cluster,
      final FixedPartitionCfg partitionConfig,
      final boolean isPriorityElectionEnabled) {
    final var members = new HashSet<Integer>();
    final var clusterSize = cluster.getClusterSize();
    final var partitionsCount = cluster.getPartitionsCount();
    final var partitionId = partitionConfig.getPartitionId();

    if (partitionId < 1 || partitionId > partitionsCount) {
      throw new IllegalArgumentException(
          String.format(
              "Expected fixed partition scheme to define entries with a valid partitionId between 1"
                  + " and %d, but %d was given",
              partitionsCount, partitionId));
    }

    final var observedPriorities = new HashSet<Integer>();
    for (final var node : partitionConfig.getNodes()) {
      final var nodeId = node.getNodeId();
      if (nodeId < 0 || nodeId >= clusterSize) {
        throw new IllegalArgumentException(
            String.format(
                "Expected fixed partition scheme for partition %d to define nodes with a nodeId "
                    + "between 0 and %d, but it was %d",
                partitionId, clusterSize - 1, nodeId));
      }

      if (isPriorityElectionEnabled && !observedPriorities.add(node.getPriority())) {
        throw new IllegalArgumentException(
            String.format(
                "Expected each node for a partition %d to have a different priority, but at least "
                    + "two of them have the same priorities: %s",
                partitionId, partitionConfig.getNodes()));
      }

      members.add(nodeId);
    }

    return members;
  }

  private void validateNetworkSecurityConfig(final SecurityCfg security) {
    final var certificateChainPath = security.getCertificateChainPath();
    final var privateKeyPath = security.getPrivateKeyPath();

    if (certificateChainPath == null) {
      throw new IllegalArgumentException(
          "Expected to have a valid certificate chain path for network security, but none "
              + "configured");
    }

    if (privateKeyPath == null) {
      throw new IllegalArgumentException(
          "Expected to have a valid private key path for network security, but none configured");
    }

    if (!certificateChainPath.canRead()) {
      throw new IllegalArgumentException(
          String.format(
              "Expected the configured network security certificate chain path '%s' to point to a"
                  + " readable file, but it does not",
              certificateChainPath));
    }

    if (!privateKeyPath.canRead()) {
      throw new IllegalArgumentException(
          String.format(
              "Expected the configured network security private key path '%s' to point to a "
                  + "readable file, but it does not",
              privateKeyPath));
    }
  }

  public ActorScheduler getScheduler() {
    return scheduler;
  }

  public BrokerCfg getBrokerConfiguration() {
    return brokerCfg;
  }

  public IdentityConfiguration getIdentityConfiguration() {
    return identityConfiguration;
  }

  public AtomixCluster getCluster() {
    return cluster;
  }

  public Map<String, String> getDiagnosticContext() {
    return diagnosticContext;
  }

  public BrokerClient getBrokerClient() {
    return brokerClient;
  }
}
