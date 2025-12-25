/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';

import {SessionService} from './session.service';

describe('PhaseService', () => {
  let service: SessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SessionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
