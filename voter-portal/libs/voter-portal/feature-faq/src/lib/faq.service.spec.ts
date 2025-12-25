/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';

import { FAQService } from './faq.service';

describe('FaqService', () => {
	let service: FAQService;

	beforeEach(() => {
		TestBed.configureTestingModule({});
		service = TestBed.inject(FAQService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
