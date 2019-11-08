/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {shallow} from 'enzyme';
import TenantSource from './TenantSource';

const props = {
  onChange: jest.fn(),
  setInvalid: jest.fn(),
  tenantsWithDefinitions: [
    {
      id: null,
      name: 'Not defined',
      definitions: [
        {
          key: 'invoice-assign-approver',
          name: 'Assign Approver Group',
          type: 'decision'
        },
        {
          key: 'beverages',
          name: 'Beverages',
          type: 'decision'
        }
      ]
    },
    {
      id: 'csm',
      name: 'csm',
      definitions: [
        {
          key: 'hiring-demo-5-tenants',
          name: 'Hiring Demo 5 Tenants',
          type: 'process'
        }
      ]
    }
  ]
};

it('should match snapshot', () => {
  const node = shallow(<TenantSource {...props} />);

  expect(node).toMatchSnapshot();
});

it('should inoke onChange with the selected source', () => {
  const node = shallow(<TenantSource {...props} />);

  node
    .find('Typeahead')
    .props()
    .onSelect(props.tenantsWithDefinitions[0]);

  node
    .find('Checklist')
    .props()
    .onChange('invoice-assign-approver', true);

  expect(props.onChange).toHaveBeenCalledWith([
    {definitionKey: 'invoice-assign-approver', definitionType: 'decision', tenants: [null]}
  ]);
});

it('should show the shared definition when selecting a tenant', () => {
  const node = shallow(<TenantSource {...props} />);

  node
    .find('Typeahead')
    .props()
    .onSelect(props.tenantsWithDefinitions[1]);

  expect(node.find('Checklist').props().data).toMatchSnapshot();
});
