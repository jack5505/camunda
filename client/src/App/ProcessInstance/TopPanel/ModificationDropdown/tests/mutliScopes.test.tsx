/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {screen, waitFor} from '@testing-library/react';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
import {processInstanceDetailsDiagramStore} from 'modules/stores/processInstanceDetailsDiagram';
import {modificationsStore} from 'modules/stores/modifications';
import {open} from 'modules/mocks/diagrams';
import {renderPopover} from './mocks';
import {mockFetchProcessInstanceDetailStatistics} from 'modules/mocks/api/processInstances/fetchProcessInstanceDetailStatistics';
import {mockFetchProcessXML} from 'modules/mocks/api/processes/fetchProcessXML';
import {IS_ADD_TOKEN_WITH_ANCESTOR_KEY_SUPPORTED} from 'modules/feature-flags';

describe('Modification Dropdown - Multi Scopes', () => {
  beforeAll(() => {
    //@ts-ignore
    IS_REACT_ACT_ENVIRONMENT = false;
  });

  afterAll(() => {
    //@ts-ignore
    IS_REACT_ACT_ENVIRONMENT = true;
  });

  beforeEach(() => {
    mockFetchProcessXML().withSuccess(open('multipleInstanceSubProcess.bpmn'));
  });

  it('should support add modification for task with multiple scopes', async () => {
    mockFetchProcessInstanceDetailStatistics().withSuccess([
      {
        activityId: 'OuterSubProcess',
        active: 1,
        incidents: 0,
        completed: 0,
        canceled: 0,
      },
      {
        activityId: 'InnerSubProcess',
        active: 1,
        incidents: 0,
        completed: 0,
        canceled: 0,
      },
      {
        activityId: 'TaskB',
        active: 10,
        incidents: 0,
        completed: 0,
        canceled: 0,
      },
    ]);

    renderPopover();

    await waitFor(() =>
      expect(
        processInstanceDetailsDiagramStore.state.diagramModel
      ).not.toBeNull()
    );
    modificationsStore.enableModificationMode();

    flowNodeSelectionStore.selectFlowNode({
      flowNodeId: 'TaskB',
    });

    expect(
      await screen.findByText(/Flow Node Modifications/)
    ).toBeInTheDocument();
    expect(screen.getByText(/Cancel/)).toBeInTheDocument();
    expect(screen.getByText(/Move/)).toBeInTheDocument();
    expect(screen.getByText(/Add/)).toBeInTheDocument();
  });

  (IS_ADD_TOKEN_WITH_ANCESTOR_KEY_SUPPORTED ? it.skip : it)(
    'should not support add modification for task with multiple inner parent scopes',
    async () => {
      mockFetchProcessInstanceDetailStatistics().withSuccess([
        {
          activityId: 'OuterSubProcess',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'InnerSubProcess',
          active: 10,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'TaskB',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
      ]);

      renderPopover();

      await waitFor(() =>
        expect(
          processInstanceDetailsDiagramStore.state.diagramModel
        ).not.toBeNull()
      );
      modificationsStore.enableModificationMode();

      flowNodeSelectionStore.selectFlowNode({
        flowNodeId: 'TaskB',
      });

      expect(
        await screen.findByText(/Flow Node Modifications/)
      ).toBeInTheDocument();
      expect(screen.getByText(/Cancel/)).toBeInTheDocument();
      expect(screen.getByText(/Move/)).toBeInTheDocument();
      expect(screen.queryByText(/Add/)).not.toBeInTheDocument();
    }
  );

  (IS_ADD_TOKEN_WITH_ANCESTOR_KEY_SUPPORTED ? it.skip : it)(
    'should not support add modification for task with multiple outer parent scopes',
    async () => {
      mockFetchProcessInstanceDetailStatistics().withSuccess([
        {
          activityId: 'OuterSubProcess',
          active: 10,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'InnerSubProcess',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'TaskB',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
      ]);

      renderPopover();

      await waitFor(() =>
        expect(
          processInstanceDetailsDiagramStore.state.diagramModel
        ).not.toBeNull()
      );
      modificationsStore.enableModificationMode();

      flowNodeSelectionStore.selectFlowNode({
        flowNodeId: 'TaskB',
      });

      expect(
        await screen.findByText(/Flow Node Modifications/)
      ).toBeInTheDocument();
      expect(screen.getByText(/Cancel/)).toBeInTheDocument();
      expect(screen.getByText(/Move/)).toBeInTheDocument();
      expect(screen.queryByText(/Add/)).not.toBeInTheDocument();
    }
  );

  (IS_ADD_TOKEN_WITH_ANCESTOR_KEY_SUPPORTED ? it.skip : it)(
    'should render no modifications available',
    async () => {
      mockFetchProcessInstanceDetailStatistics().withSuccess([
        {
          activityId: 'OuterSubProcess',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'InnerSubProcess',
          active: 10,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'TaskB',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
      ]);

      renderPopover();

      await waitFor(() =>
        expect(
          processInstanceDetailsDiagramStore.state.diagramModel
        ).not.toBeNull()
      );
      modificationsStore.enableModificationMode();

      modificationsStore.cancelAllTokens('TaskB');

      flowNodeSelectionStore.selectFlowNode({
        flowNodeId: 'TaskB',
      });

      expect(
        await screen.findByText(/Flow Node Modifications/)
      ).toBeInTheDocument();
      expect(
        screen.getByText(/No modifications available/)
      ).toBeInTheDocument();
      expect(screen.queryByText(/Cancel/)).not.toBeInTheDocument();
      expect(screen.queryByText(/Move/)).not.toBeInTheDocument();
      expect(screen.queryByText(/Add/)).not.toBeInTheDocument();
    }
  );

  (IS_ADD_TOKEN_WITH_ANCESTOR_KEY_SUPPORTED ? it : it.skip)(
    'should render add modification for flow nodes that has multiple running scopes',
    async () => {
      mockFetchProcessInstanceDetailStatistics().withSuccess([
        {
          activityId: 'OuterSubProcess',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'InnerSubProcess',
          active: 10,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
        {
          activityId: 'TaskB',
          active: 1,
          incidents: 0,
          completed: 0,
          canceled: 0,
        },
      ]);

      renderPopover();

      await waitFor(() =>
        expect(
          processInstanceDetailsDiagramStore.state.diagramModel
        ).not.toBeNull()
      );
      modificationsStore.enableModificationMode();

      modificationsStore.cancelAllTokens('TaskB');

      flowNodeSelectionStore.selectFlowNode({
        flowNodeId: 'TaskB',
      });

      expect(
        await screen.findByText(/Flow Node Modifications/)
      ).toBeInTheDocument();
      expect(screen.queryByText(/Cancel/)).not.toBeInTheDocument();
      expect(screen.queryByText(/Move/)).not.toBeInTheDocument();
      expect(screen.getByText(/Add/)).toBeInTheDocument();
    }
  );
});
