/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';

import {MetricService} from './metric.service';

describe('MetricService', () => {
  let service: MetricService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MockProvider(HttpClient)],
    });
    service = TestBed.inject(MetricService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
