/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {MockProvider} from 'ng-mocks';
import {HttpClient} from "@angular/common/http";
import {DataCollectionService} from "./data-collection.service";

describe('DataCollectionService', () => {
	let service: DataCollectionService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockProvider(HttpClient)],
		});
		service = TestBed.inject(DataCollectionService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
