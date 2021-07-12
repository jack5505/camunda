/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {shallow} from 'enzyme';

import {BPMNDiagram} from 'components';
import {loadProcessDefinitionXml} from 'services';

import FilterSingleDefinitionSelection from '../FilterSingleDefinitionSelection';
import {NodeDuration} from './NodeDuration';

jest.mock('services', () => ({
  ...jest.requireActual('services'),
  loadProcessDefinitionXml: jest.fn().mockReturnValue('someXml'),
}));

jest.mock('bpmn-js/lib/NavigatedViewer', () => {
  return class Viewer {
    constructor() {
      this.elements = [
        {id: 'a', name: 'Element A'},
        {id: 'b', name: 'Element B'},
        {id: 'c', name: 'Element C'},
      ];

      this.elementRegistry = {
        filter: () => {
          return {
            map: () => this.elements,
          };
        },
      };
    }
    attachTo = jest.fn();
    importXML = jest.fn();
    get = () => {
      return this.elementRegistry;
    };
  };
});

beforeEach(() => {
  loadProcessDefinitionXml.mockClear();
});

const props = {
  mightFail: (data, fn) => fn(data),
  close: jest.fn(),
  addFilter: jest.fn(),
  data: [],
  definitions: [
    {identifier: 'definition', key: 'definitionKey', versions: ['all'], tenantIds: [null]},
  ],
};

it('should display the bpmn diagram in the modal', async () => {
  const node = await shallow(<NodeDuration {...props} />);
  await flushPromises();

  expect(node.find(BPMNDiagram)).toExist();
});

it('should add duration filters correctly', async () => {
  const spy = jest.fn();

  const node = shallow(<NodeDuration {...props} addFilter={spy} />);
  await flushPromises();

  node.find('NodesTable').prop('onChange')({a: {unit: 'years', value: '12', operator: '>'}});

  node.find('[primary]').simulate('click');

  expect(spy).toHaveBeenCalledWith({
    data: {a: {operator: '>', unit: 'years', value: 12}},
    type: 'flowNodeDuration',
    appliedTo: ['definition'],
  });
});

it('should apply previously defined values', async () => {
  const node = shallow(
    <NodeDuration
      {...props}
      filterData={{
        type: 'flowNodeDuration',
        data: {a: {operator: '>', unit: 'years', value: 12}},
        appliedTo: ['definition'],
      }}
    />
  );

  node.setProps({open: true});
  await flushPromises();

  expect(node.state('values').a.value).toBe('12');
  expect(node.state('values').a.unit).toBe('years');
});

it('should initially load xml', async () => {
  shallow(<NodeDuration {...props} />);
  await flushPromises();

  expect(loadProcessDefinitionXml).toHaveBeenCalledWith('definitionKey', 'all', null);
});

it('should load new xml after changing definition', async () => {
  const definitions = [
    {identifier: 'definition', key: 'definitionKey', versions: ['all'], tenantIds: [null]},
    {
      identifier: 'otherDefinition',
      key: 'otherDefinitionKey',
      versions: ['1'],
      tenantIds: ['marketing', 'sales'],
    },
  ];
  const node = shallow(<NodeDuration {...props} definitions={definitions} />);
  await flushPromises();

  node.find(FilterSingleDefinitionSelection).prop('setApplyTo')(definitions[1]);
  await flushPromises();

  expect(loadProcessDefinitionXml).toHaveBeenCalledWith('otherDefinitionKey', '1', 'marketing');
});
