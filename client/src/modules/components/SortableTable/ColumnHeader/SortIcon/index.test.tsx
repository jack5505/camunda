/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {render, screen} from 'modules/testing-library';

import {SortIcon} from './index';

describe('SortIcon', () => {
  it('should render an Up icon', () => {
    render(<SortIcon sortOrder="asc" />);
    expect(screen.getByTestId('asc-icon')).toBeInTheDocument();
  });

  it('should render a Down icon', () => {
    render(<SortIcon sortOrder="desc" />);
    expect(screen.getByTestId('desc-icon')).toBeInTheDocument();
  });
});
