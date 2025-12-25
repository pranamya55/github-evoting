/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideMockStore } from '@ngrx/store/testing';

import { ProcessCancellationService } from './process-cancellation.service';
import { MockProvider } from "ng-mocks";

describe('CancelProcessService', () => {
	let service: ProcessCancellationService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [RouterTestingModule],
			providers: [provideMockStore({})],
		});
		service = TestBed.inject(ProcessCancellationService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
