/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {HttpClient} from "@angular/common/http";
import {DataExchangeService} from "./data-exchange.service";

describe('DataExchangeService', () => {
	let service: DataExchangeService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockProvider(HttpClient)],
		});
		service = TestBed.inject(DataExchangeService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
