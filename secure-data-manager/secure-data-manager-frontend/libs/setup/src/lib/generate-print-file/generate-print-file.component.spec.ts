/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Store} from '@ngrx/store';
import {TranslateModule} from '@ngx-translate/core';
import {MockProvider} from 'ng-mocks';
import {GeneratePrintFileService} from './generate-print-file.service';
import {GeneratePrintFileComponent} from './generate-print-file.component';

describe('GeneratePrintFileComponent', () => {
	let component: GeneratePrintFileComponent;
	let fixture: ComponentFixture<GeneratePrintFileComponent>;

	beforeEach(() => {
		TestBed.configureTestingModule({
			declarations: [
				GeneratePrintFileComponent,
			],
			imports: [FormsModule, TranslateModule],
			providers: [
				MockProvider(Store),
				GeneratePrintFileService,
			],
		});

		fixture = TestBed.createComponent(GeneratePrintFileComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
