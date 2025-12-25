/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {CanActivateChildFn} from '@angular/router';

import {workflowGuard} from './workflow.guard';

describe('workflowGuard', () => {
  const executeGuard: CanActivateChildFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => workflowGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
