/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {HttpClient} from "@angular/common/http";
import {ComputeService} from "./compute.service";


describe('ComputeService', () => {
	let service: ComputeService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockProvider(HttpClient)],
		});
		service = TestBed.inject(ComputeService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
