/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {HttpClient} from "@angular/common/http";
import {PreConfigureService} from "./pre-configure.service";

describe('PreConfigurationService', () => {
	let service: PreConfigureService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockProvider(HttpClient)],
		});
		service = TestBed.inject(PreConfigureService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
