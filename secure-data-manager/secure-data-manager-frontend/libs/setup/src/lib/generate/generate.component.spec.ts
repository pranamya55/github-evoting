/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Store} from '@ngrx/store';
import {TranslateModule} from '@ngx-translate/core';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {MockComponent, MockProvider} from 'ng-mocks';
import {GenerateService} from './generate.service';
import {GenerateComponent} from './generate.component';

describe('GenerateComponent', () => {
	let component: GenerateComponent;
	let fixture: ComponentFixture<GenerateComponent>;

	beforeEach(() => {
		TestBed.configureTestingModule({
			declarations: [
				GenerateComponent,
				MockComponent(ProgressComponent),
			],
			imports: [FormsModule, TranslateModule],
			providers: [MockProvider(Store), GenerateService],
		});

		fixture = TestBed.createComponent(GenerateComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
