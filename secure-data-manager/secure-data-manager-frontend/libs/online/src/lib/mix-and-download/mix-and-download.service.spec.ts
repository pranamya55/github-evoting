/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';

import {MixAndDownloadService} from './mix-and-download.service';

describe('MixAndDownloadService', () => {
	let service: MixAndDownloadService;

	beforeEach(() => {
		TestBed.configureTestingModule({});
		service = TestBed.inject(MixAndDownloadService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
