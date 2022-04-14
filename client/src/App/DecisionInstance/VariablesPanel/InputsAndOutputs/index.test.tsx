/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {
  render,
  screen,
  waitForElementToBeRemoved,
  within,
} from 'modules/testing-library';
import {mockServer} from 'modules/mock-server/node';
import {
  assignApproverGroup,
  assignApproverGroupWithoutVariables,
  invoiceClassification,
} from 'modules/mocks/mockDecisionInstance';
import {decisionInstanceDetailsStore} from 'modules/stores/decisionInstanceDetails';
import {ThemeProvider} from 'modules/theme/ThemeProvider';
import {rest} from 'msw';
import {InputsAndOutputs} from './index';

describe('<InputsAndOutputs />', () => {
  afterEach(() => {
    decisionInstanceDetailsStore.reset();
  });

  it('should have section panels', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.json(invoiceClassification))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {
      wrapper: ThemeProvider,
    });

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('inputs-skeleton')
    );

    expect(screen.getByRole('heading', {name: /inputs/i})).toBeInTheDocument();
    expect(screen.getByRole('heading', {name: /outputs/i})).toBeInTheDocument();
  });

  it('should show a loading skeleton', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.status(500))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {wrapper: ThemeProvider});

    expect(screen.getByTestId('inputs-skeleton')).toBeInTheDocument();
    expect(screen.getByTestId('outputs-skeleton')).toBeInTheDocument();

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('inputs-skeleton')
    );

    expect(screen.queryByTestId('inputs-skeleton')).not.toBeInTheDocument();
    expect(screen.queryByTestId('outputs-skeleton')).not.toBeInTheDocument();
  });

  it('should show empty message for failed decision instances with variables', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.json(assignApproverGroup))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {wrapper: ThemeProvider});

    expect(
      await screen.findByText(
        'No output available because the evaluation failed'
      )
    ).toBeInTheDocument();

    expect(
      screen.queryByText('No input available because the evaluation failed')
    ).not.toBeInTheDocument();
  });

  it('should show empty message for failed decision instances without variables', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.json(assignApproverGroupWithoutVariables))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {wrapper: ThemeProvider});

    expect(
      await screen.findByText(
        'No output available because the evaluation failed'
      )
    ).toBeInTheDocument();

    expect(
      screen.getByText('No input available because the evaluation failed')
    ).toBeInTheDocument();
  });

  it('should load inputs and outputs', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.json(invoiceClassification))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {wrapper: ThemeProvider});

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('inputs-skeleton')
    );

    const [inputsTable, outputsTable] = screen.getAllByRole('table');

    const [inputsNameColumnHeader, inputsValueColumnHeader] = within(
      inputsTable!
    ).getAllByRole('columnheader');
    const [
      outputsRuleColumnHeader,
      outputsNameColumnHeader,
      outputsValueColumnHeader,
    ] = within(outputsTable!).getAllByRole('columnheader');
    const [, inputsFirstTableBodyRow] = within(inputsTable!).getAllByRole(
      'row'
    );
    const [, outputsFirstTableBodyRow] = within(outputsTable!).getAllByRole(
      'row'
    );
    const [inputsNameCell, inputsValueCell] = within(
      inputsFirstTableBodyRow!
    ).getAllByRole('cell');
    const [outputsRuleCell, outputsNameCell, outputsValueCell] = within(
      outputsFirstTableBodyRow!
    ).getAllByRole('cell');

    expect(inputsNameColumnHeader).toBeInTheDocument();
    expect(inputsValueColumnHeader).toBeInTheDocument();
    expect(outputsRuleColumnHeader).toBeInTheDocument();
    expect(outputsNameColumnHeader).toBeInTheDocument();
    expect(outputsValueColumnHeader).toBeInTheDocument();

    expect(inputsNameCell).toBeInTheDocument();
    expect(inputsValueCell).toBeInTheDocument();
    expect(outputsRuleCell).toBeInTheDocument();
    expect(outputsNameCell).toBeInTheDocument();
    expect(outputsValueCell).toBeInTheDocument();
  });

  it('should show an error', async () => {
    mockServer.use(
      rest.get('/api/decision-instances/:decisionInstanceId', (_, res, ctx) =>
        res.once(ctx.status(500))
      )
    );
    decisionInstanceDetailsStore.fetchDecisionInstance('1');

    render(<InputsAndOutputs />, {wrapper: ThemeProvider});

    expect(
      await screen.findAllByText(/data could not be fetched/i)
    ).toHaveLength(2);
  });
});
