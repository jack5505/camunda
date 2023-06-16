/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {computed} from 'mobx';
import {observer} from 'mobx-react';
import {flowNodeMetaDataStore} from 'modules/stores/flowNodeMetaData';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
import {modificationsStore} from 'modules/stores/modifications';
import {processInstanceDetailsStatisticsStore} from 'modules/stores/processInstanceDetailsStatistics';
import {VariableFormValues} from 'modules/types/variables';
import {generateUniqueID} from 'modules/utils/generateUniqueID';
import {FormRenderProps} from 'react-final-form';

import {AddVariableButton, Form, VariablesContainer} from './styled';
import Variables from '../Variables';

const VariablesForm: React.FC<
  FormRenderProps<VariableFormValues, Partial<VariableFormValues>>
> = observer(({handleSubmit, form, values}) => {
  const hasEmptyNewVariable = (values: VariableFormValues) =>
    values.newVariables?.some(
      (variable) =>
        variable === undefined ||
        variable.name === undefined ||
        variable.value === undefined
    );

  const {isModificationModeEnabled} = modificationsStore;

  const isVariableModificationAllowed = computed(() => {
    if (
      !isModificationModeEnabled ||
      flowNodeSelectionStore.state.selection === null
    ) {
      return false;
    }

    if (flowNodeSelectionStore.isRootNodeSelected) {
      return !processInstanceDetailsStatisticsStore.willAllFlowNodesBeCanceled;
    }

    return (
      flowNodeSelectionStore.isPlaceholderSelected ||
      (flowNodeMetaDataStore.isSelectedInstanceRunning &&
        !flowNodeSelectionStore.hasPendingCancelOrMoveModification)
    );
  });

  return (
    <Form onSubmit={handleSubmit}>
      {isVariableModificationAllowed.get() && (
        <AddVariableButton
          onClick={() => {
            form.mutators.push?.('newVariables', {
              id: generateUniqueID(),
            });
          }}
          disabled={
            form.getState().submitting ||
            form.getState().hasValidationErrors ||
            form.getState().validating ||
            hasEmptyNewVariable(values)
          }
        />
      )}
      <VariablesContainer>
        <Variables />
      </VariablesContainer>
    </Form>
  );
});

export {VariablesForm};
