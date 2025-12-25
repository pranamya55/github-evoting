/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {HttpClient} from "@angular/common/http";
import {PreComputeService} from "./pre-compute.service";

describe('PreComputeService', () => {
	let service: PreComputeService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockProvider(HttpClient)],
		});
		service = TestBed.inject(PreComputeService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
