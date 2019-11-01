/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.camunda.optimize.dto.optimize.ReportType;
import org.camunda.optimize.dto.optimize.RoleType;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedEntityDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.camunda.optimize.test.util.decision.DecisionReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_DECISION_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_PROCESS_DEFINITION;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class ReportCollectionRoleAuthorizationIT extends AbstractCollectionRoleIT {

  private static final String PROCESS_KEY = "aProcess";
  private static final String DECISION_KEY = "aDecision";

  private static final List<ReportScenario> POSSIBLE_REPORT_SCENARIOS = ImmutableList.of(
    new ReportScenario(ReportType.PROCESS, false),
    new ReportScenario(ReportType.PROCESS, true),
    new ReportScenario(ReportType.DECISION, false)
  );

  private static final String ACCESS_IDENTITY_ROLES_AND_REPORT_TYPES = "accessIdentityRolesAndReportTypes";

  private static IdentityRoleAndReportScenario[] accessIdentityRolesAndReportTypes() {
    return Arrays.stream(accessIdentityRoles())
      .flatMap(ReportCollectionRoleAuthorizationIT::createReportTypeScenarios)
      .toArray(IdentityRoleAndReportScenario[]::new);
  }

  private static final String EDIT_IDENTITY_ROLES_AND_REPORT_TYPES = "editIdentityRolesAndReportTypes";

  private static IdentityRoleAndReportScenario[] editIdentityRolesAndReportTypes() {
    return Arrays.stream(editIdentityRoles())
      .flatMap(ReportCollectionRoleAuthorizationIT::createReportTypeScenarios)
      .toArray(IdentityRoleAndReportScenario[]::new);
  }

  private static final String EDIT_USER_ROLES_AND_REPORT_TYPES = "editUserRolesAndReportTypes";

  private static IdentityRoleAndReportScenario[] editUserRolesAndReportTypes() {
    return Arrays.stream(editUserRoles())
      .flatMap(ReportCollectionRoleAuthorizationIT::createReportTypeScenarios)
      .toArray(IdentityRoleAndReportScenario[]::new);
  }

  private static final String ACCESS_ONLY_IDENTITY_ROLES_AND_REPORT_TYPES = "accessOnlyIdentityRolesAndReportTypes";

  private static IdentityRoleAndReportScenario[] accessOnlyIdentityRolesAndReportTypes() {
    return Arrays.stream(accessOnlyIdentityRoles())
      .flatMap(ReportCollectionRoleAuthorizationIT::createReportTypeScenarios)
      .toArray(IdentityRoleAndReportScenario[]::new);
  }

  private static final String ACCESS_ONLY_USER_ROLES_AND_REPORT_TYPES = "accessOnlyUserRolesAndReportTypes";

  private static IdentityRoleAndReportScenario[] accessOnlyUserRolesAndReportTypes() {
    return Arrays.stream(accessOnlyUserRoles())
      .flatMap(ReportCollectionRoleAuthorizationIT::createReportTypeScenarios)
      .toArray(IdentityRoleAndReportScenario[]::new);
  }

  private static final String REPORT_SCENARIOS = "reportScenarios";

  private static List<ReportScenario> reportScenarios() {
    return POSSIBLE_REPORT_SCENARIOS;
  }

  private static Stream<IdentityRoleAndReportScenario> createReportTypeScenarios(final IdentityAndRole identityAndRole) {
    return POSSIBLE_REPORT_SCENARIOS.stream()
      .map(reportScenario -> new IdentityRoleAndReportScenario(identityAndRole, reportScenario));
  }

  @ParameterizedTest
  @MethodSource(EDIT_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void editorIdentityIsGrantedAddReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = createReportInCollectionAsKermit(identityAndReport.reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_ONLY_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void viewerIdentityIsRejectedToAddReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = createReportInCollectionAsKermit(identityAndReport.reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(EDIT_USER_ROLES_AND_REPORT_TYPES)
  public void editorUserIsGrantedToAddReportByCollectionRoleAlthoughMemberOfViewerGroupRole(
    final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addKermitGroupRoleToCollectionAsDefaultUser(RoleType.VIEWER, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = createReportInCollectionAsKermit(identityAndReport.reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_ONLY_USER_ROLES_AND_REPORT_TYPES)
  public void viewerUserIsRejectedToAddReportByCollectionRoleAlthoughMemberOfEditorGroup(
    final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addKermitGroupRoleToCollectionAsDefaultUser(RoleType.EDITOR, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = createReportInCollectionAsKermit(identityAndReport.reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedAddReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();

    // when
    final Response response = createReportInCollectionAsKermit(reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleIdentityIsRejectedToAddReportToCollection(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();

    // when
    final Response response = createReportInCollectionAsKermit(reportScenario, collectionId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(EDIT_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void editorIdentityIsGrantedCopyPrivateReportToCollectionByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final String reportId = createPrivateReportAsKermit(identityAndReport.reportScenario);
    final Response response = copyReportToCollectionAsKermit(reportId, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
    final String copyId = response.readEntity(IdDto.class).getId();
    final AuthorizedReportDefinitionDto reportCopy = getReportByIdAsKermit(copyId);
    assertThat(reportCopy.getDefinitionDto().getOwner(), is(KERMIT_USER));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_ONLY_USER_ROLES_AND_REPORT_TYPES)
  public void viewerIdentityIsRejectedToCopyPrivateReportToCollectionByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final String reportId = createPrivateReportAsKermit(identityAndReport.reportScenario);
    final Response response = copyReportToCollectionAsKermit(reportId, collectionId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedCopyPrivateReportToCollection(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();

    // when
    final String reportId = createPrivateReportAsKermit(reportScenario);
    final Response response = copyReportToCollectionAsKermit(reportId, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
    final String copyId = response.readEntity(IdDto.class).getId();
    final AuthorizedReportDefinitionDto reportCopy = getReportByIdAsKermit(copyId);
    assertThat(reportCopy.getDefinitionDto().getOwner(), is(KERMIT_USER));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleIdentityIsRejectedToCopyPrivateReportToCollection(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();

    // when
    final String reportId = createPrivateReportAsKermit(reportScenario);
    final Response response = copyReportToCollectionAsKermit(reportId, collectionId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void anyRoleIdentityIsGrantedCopyCollectionReportAsPrivateReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = copyReportAsPrivateReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(200));
    final String copyId = response.readEntity(IdDto.class).getId();
    final AuthorizedReportDefinitionDto reportCopy = getReportByIdAsKermit(copyId);
    assertThat(reportCopy.getDefinitionDto().getOwner(), is(KERMIT_USER));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleIdentityIsRejectedToCopyCollectionReportAsPrivateReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = copyReportAsPrivateReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void readAccessRejectedToCopyPrivateReportOfOtherUser(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = readReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedToCopyPrivateReportOfOtherUser(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = copyReportToCollectionAsKermit(reportId, collectionId);

    // then
    assertThat(response.getStatus(), is(200));
    final String copyId = response.readEntity(IdDto.class).getId();
    final AuthorizedReportDefinitionDto reportCopy = getReportByIdAsKermit(copyId);
    assertThat(reportCopy.getDefinitionDto().getOwner(), is(KERMIT_USER));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void readAccessGrantedToCopyCollectionReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = readReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleReadAccessRejectedToCollectionReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = readReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedAccessToCollectionReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = readReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void evaluateAccessRejectedToPrivateReportOfOtherUser(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = evaluateReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedEvaluateAccessToPrivateReportOfOtherUser(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantAllDefinitionAuthorizationsForUserWithReadHistoryPermission(
      KERMIT_USER, getEngineResourceTypeForReportType(reportScenario)
    );
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);
    if (!reportScenario.combined) {
      // for non combined reports a definition needs to be set for them to be evaluable
      updateReportAsDefaultUser(
        reportId, constructReportWithDefinition(getEngineResourceTypeForReportType(reportScenario))
      );
    }

    // when
    final Response response = evaluateReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void evaluateAccessGrantedToCollectionReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final int engineDefinitionResourceType = getEngineResourceTypeForReportType(identityAndReport);
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(engineDefinitionResourceType);
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();


    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    if (!identityAndReport.reportScenario.combined) {
      // for non combined reports a definition needs to be set for them to be evaluable
      updateReportAsDefaultUser(reportId, constructReportWithDefinition(engineDefinitionResourceType));
    }
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = evaluateReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(200));
    final AuthorizedEntityDto evaluationResultDto = response.readEntity(AuthorizedEntityDto.class);
    assertThat(evaluationResultDto.getCurrentUserRole(), is(getExpectedResourceRoleForCollectionRole(identityAndRole)));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_IDENTITY_ROLES)
  public void evaluateWithErrorContainsCurrentUserRole(final IdentityAndRole identityAndRole) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();


    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(
      new ReportScenario(ReportType.PROCESS, false), collectionId
    );
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = evaluateReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(500));
    final ErrorResponseDto errorResponseDto = response.readEntity(ErrorResponseDto.class);
    assertThat(
      errorResponseDto.getReportDefinition().getCurrentUserRole(),
      is(getExpectedResourceRoleForCollectionRole(identityAndRole))
    );
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleEvaluateAccessRejectedToCollectionReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = evaluateReportByIdAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void listReportsContainsNoPrivateReportsOfOtherUsers(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    createPrivateReportAsDefaultUser(reportScenario);

    // when
    final List<AuthorizedReportDefinitionDto> authorizedReports = listReportsAsKermit();

    // then
    assertThat(authorizedReports.size(), is(0));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void listReportsContainsNoReportsFromUnauthorizedCollections(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final List<AuthorizedReportDefinitionDto> authorizedReports = listReportsAsKermit();

    // then
    assertThat(authorizedReports.size(), is(0));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserListReportsContainsOtherUsersPrivateReports(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String kermitReportId = createPrivateReportAsKermit(reportScenario);
    final String otherReportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final List<AuthorizedReportDefinitionDto> authorizedReports = listReportsAsKermit();

    // then only private reports are included in the results
    assertThat(authorizedReports.size(), is(2));
    assertThat(
      authorizedReports.stream()
        .map(AuthorizedReportDefinitionDto::getDefinitionDto)
        .map(ReportDefinitionDto::getId)
        .collect(Collectors.toList()),
      containsInAnyOrder(otherReportId, kermitReportId)
    );
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void updateOtherPrivateReportFails(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = updateReportAsKermit(reportId, reportScenario);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserCanUpdateOtherPrivateReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = updateReportAsKermit(reportId, reportScenario);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(EDIT_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void editorIdentityIsGrantedUpdateReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = updateReportAsKermit(reportId, identityAndReport.reportScenario);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_ONLY_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void viewerIdentityIsRejectedToUpdateReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = updateReportAsKermit(reportId, identityAndReport.reportScenario);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedUpdateReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = updateReportAsKermit(reportId, reportScenario);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleIdentityIsRejectedToUpdateReportToCollection(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = updateReportAsKermit(reportId, reportScenario);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void deleteOtherPrivateReportFails(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserCanDeleteOtherPrivateReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String reportId = createPrivateReportAsDefaultUser(reportScenario);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(EDIT_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void editorIdentityIsGrantedDeleteReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(ACCESS_ONLY_IDENTITY_ROLES_AND_REPORT_TYPES)
  public void viewerIdentityIsRejectedToDeleteReportByCollectionRole(final IdentityRoleAndReportScenario identityAndReport) {
    // given
    final IdentityAndRole identityAndRole = identityAndReport.identityAndRole;
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(identityAndReport.reportScenario, collectionId);
    addRoleToCollectionAsDefaultUser(identityAndRole.roleType, identityAndRole.identityDto, collectionId);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void superUserIdentityIsGrantedDeleteReport(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(KERMIT_USER);

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(204));
  }

  @ParameterizedTest
  @MethodSource(REPORT_SCENARIOS)
  public void noRoleIdentityIsRejectedToDeleteReportToCollection(final ReportScenario reportScenario) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = createNewCollectionAsDefaultUser();
    final String reportId = createReportInCollectionAsDefaultUser(reportScenario, collectionId);

    // when
    final Response response = deleteReportAsKermit(reportId);

    // then
    assertThat(response.getStatus(), is(403));
  }

  private String createPrivateReportAsDefaultUser(final ReportScenario reportScenario) {
    return createPrivateReportAsUser(reportScenario, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  private int getEngineResourceTypeForReportType(final IdentityRoleAndReportScenario identityAndReport) {
    return getEngineResourceTypeForReportType(identityAndReport.reportScenario);
  }

  private int getEngineResourceTypeForReportType(final ReportScenario reportScenario) {
    return reportScenario.reportType.equals(ReportType.PROCESS)
      ? RESOURCE_TYPE_PROCESS_DEFINITION
      : RESOURCE_TYPE_DECISION_DEFINITION;
  }

  private String createPrivateReportAsKermit(final ReportScenario reportScenario) {
    return createPrivateReportAsUser(reportScenario, KERMIT_USER, KERMIT_USER);
  }

  private String createPrivateReportAsUser(final ReportScenario reportScenario,
                                           final String user,
                                           final String password) {
    return createReportInCollectionAsUser(reportScenario, null, user, password)
      .readEntity(IdDto.class)
      .getId();
  }

  private Response createReportInCollectionAsKermit(final ReportScenario reportScenario,
                                                    final String collectionId) {
    return createReportInCollectionAsUser(reportScenario, collectionId, KERMIT_USER, KERMIT_USER);
  }

  private String createReportInCollectionAsDefaultUser(final ReportScenario reportScenario,
                                                       final String collectionId) {
    return createReportInCollectionAsUser(reportScenario, collectionId, DEFAULT_USERNAME, DEFAULT_PASSWORD)
      .readEntity(IdDto.class)
      .getId();
  }

  private Response createReportInCollectionAsUser(final ReportScenario reportScenario,
                                                  final String collectionId,
                                                  final String user,
                                                  final String password) {
    switch (reportScenario.reportType) {
      case PROCESS:
        if (reportScenario.combined) {
          CombinedReportDefinitionDto combinedReportDefinitionDto = new CombinedReportDefinitionDto();
          combinedReportDefinitionDto.setCollectionId(collectionId);
          return embeddedOptimizeExtension
            .getRequestExecutor()
            .withUserAuthentication(user, password)
            .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
            .execute();
        } else {
          SingleProcessReportDefinitionDto singleProcessReportDefinitionDto = new SingleProcessReportDefinitionDto();
          singleProcessReportDefinitionDto.setCollectionId(collectionId);
          return embeddedOptimizeExtension
            .getRequestExecutor()
            .withUserAuthentication(user, password)
            .buildCreateSingleProcessReportRequest(singleProcessReportDefinitionDto)
            .execute();
        }
      case DECISION:
        SingleDecisionReportDefinitionDto singleDecisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
        singleDecisionReportDefinitionDto.setCollectionId(collectionId);
        return embeddedOptimizeExtension
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildCreateSingleDecisionReportRequest(singleDecisionReportDefinitionDto)
          .execute();
      default:
        throw new OptimizeIntegrationTestException("Unsupported reportType: " + reportScenario.reportType);
    }
  }

  private Response copyReportAsPrivateReportAsKermit(final String reportId) {
    return copyReportToCollectionAsKermit(reportId, "null");
  }

  private Response copyReportToCollectionAsKermit(final String reportId, final String collectionId) {
    return copyReportToCollectionAsUser(reportId, collectionId, KERMIT_USER, KERMIT_USER);
  }

  private Response copyReportToCollectionAsUser(final String reportId,
                                                final String collectionId,
                                                final String user,
                                                final String password) {
    return embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(user, password)
      .buildCopyReportRequest(reportId, collectionId)
      .execute();
  }

  private AuthorizedReportDefinitionDto getReportByIdAsKermit(final String reportId) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildGetReportRequest(reportId)
      .execute(AuthorizedReportDefinitionDto.class, 200);
  }

  private Response readReportByIdAsKermit(final String reportId) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildGetReportRequest(reportId)
      .execute();
  }

  private Response evaluateReportByIdAsKermit(final String reportId) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSavedReportRequest(reportId)
      .execute();
  }

  private List<AuthorizedReportDefinitionDto> listReportsAsKermit() {
    return listReportsAsUser(KERMIT_USER, KERMIT_USER);
  }

  private List<AuthorizedReportDefinitionDto> listReportsAsUser(final String user, final String password) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .withUserAuthentication(user, password)
      .buildGetAllPrivateReportsRequest()
      .executeAndReturnList(AuthorizedReportDefinitionDto.class, 200);
  }

  private Response updateReportAsKermit(final String reportId, final ReportScenario reportScenario) {
    final ReportDefinitionDto<ReportDataDto> reportUpdate = ReportDefinitionDto.builder()
      .reportType(reportScenario.reportType)
      .combined(reportScenario.combined)
      .data(getReportDataForScenario(reportScenario))
      .build();

    return updateReportAsKermit(reportId, reportUpdate);
  }

  private Response updateReportAsDefaultUser(final String reportId, final ReportDefinitionDto reportUpdate) {
    return updateReportAsUser(reportId, reportUpdate, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  private Response updateReportAsKermit(final String reportId, final ReportDefinitionDto reportUpdate) {
    return updateReportAsUser(reportId, reportUpdate, KERMIT_USER, KERMIT_USER);
  }

  private Response updateReportAsUser(final String reportId,
                                      final ReportDefinitionDto reportUpdate,
                                      final String user,
                                      final String password) {
    switch (reportUpdate.getReportType()) {
      case PROCESS:
        if (reportUpdate.getCombined()) {
          return embeddedOptimizeExtension
            .getRequestExecutor()
            .withUserAuthentication(user, password)
            .buildUpdateCombinedProcessReportRequest(reportId, reportUpdate)
            .execute();
        } else {
          return embeddedOptimizeExtension
            .getRequestExecutor()
            .withUserAuthentication(user, password)
            .buildUpdateSingleProcessReportRequest(reportId, reportUpdate)
            .execute();
        }
      case DECISION:
        return embeddedOptimizeExtension
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildUpdateSingleDecisionReportRequest(reportId, reportUpdate)
          .execute();
      default:
        throw new OptimizeIntegrationTestException("Unsupported reportType: " + reportUpdate.getReportType());
    }
  }

  private Response deleteReportAsKermit(final String reportId) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDeleteReportRequest(reportId)
      .execute();
  }

  private ReportDefinitionDto constructReportWithDefinition(int resourceType) {
    switch (resourceType) {
      default:
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        SingleProcessReportDefinitionDto processReportDefinitionDto = new SingleProcessReportDefinitionDto();
        ProcessReportDataDto processReportDataDto = ProcessReportDataBuilder
          .createReportData()
          .setProcessDefinitionKey(getDefinitionKey(resourceType))
          .setProcessDefinitionVersion("1")
          .setReportDataType(ProcessReportDataType.RAW_DATA)
          .build();
        processReportDefinitionDto.setData(processReportDataDto);
        return processReportDefinitionDto;
      case RESOURCE_TYPE_DECISION_DEFINITION:
        SingleDecisionReportDefinitionDto decisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
        DecisionReportDataDto decisionReportDataDto = DecisionReportDataBuilder.create()
          .setDecisionDefinitionKey(getDefinitionKey(resourceType))
          .setDecisionDefinitionVersion("1")
          .setReportDataType(DecisionReportDataType.RAW_DATA)
          .build();
        decisionReportDefinitionDto.setData(decisionReportDataDto);
        return decisionReportDefinitionDto;
    }
  }

  private String getDefinitionKey(final int definitionResourceType) {
    return definitionResourceType == RESOURCE_TYPE_PROCESS_DEFINITION ? PROCESS_KEY : DECISION_KEY;
  }

  private ReportDataDto getReportDataForScenario(final ReportScenario reportScenario) {
    switch (reportScenario.reportType) {
      case PROCESS:
        if (reportScenario.combined) {
          return new CombinedReportDataDto();
        } else {
          return new ProcessReportDataDto();
        }
      case DECISION:
        return new DecisionReportDataDto();
      default:
        throw new OptimizeIntegrationTestException("Unsupported reportType: " + reportScenario.reportType);
    }
  }

  @Data
  @AllArgsConstructor
  protected static class IdentityRoleAndReportScenario {

    IdentityAndRole identityAndRole;
    ReportScenario reportScenario;
  }

  @Data
  @AllArgsConstructor
  protected static class ReportScenario {

    ReportType reportType;
    boolean combined;
  }
}
