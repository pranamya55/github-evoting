/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController,} from '@angular/common/http/testing';
import {GeneratePrintFileService} from './generate-print-file.service';
import {catchError} from 'rxjs';

describe('PrintFileGenerateService', () => {
	let service: GeneratePrintFileService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [GeneratePrintFileService],
		});
		service = TestBed.inject(GeneratePrintFileService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpMock.verify();
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('should send a POST request to print-file-generate', () => {
		service.generatePrintFile().subscribe();

		const req = httpMock.expectOne(
			`${service['url']}sdm-setup/print-file-generate`,
		);
		expect(req.request.method).toBe('POST');
		req.flush({});
	});

	it('should handle error for print-file-generate request', () => {
		service
			.generatePrintFile()
			.pipe(
				catchError((error) => {
					expect(error).toBeTruthy();
					fail('Expected an error, but received a response');
					throw error;
				}),
			)
			.subscribe();

		const req = httpMock.expectOne(
			`${service['url']}sdm-setup/print-file-generate`,
		);
		req.error(
			new ProgressEvent('error', {
				lengthComputable: false,
				loaded: 0,
				total: 0,
			}),
		);
	});
});
