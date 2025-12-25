/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BackendService } from './backend.service';

describe('BackendService', () => {
	let service: BackendService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [RouterTestingModule],
			providers: [BackendService],
		});
		service = TestBed.inject(BackendService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});
});
