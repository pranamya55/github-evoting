/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { TranslationListDirective } from '@vp/voter-portal-ui-directives';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { MockDirective, MockProvider } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { BackendErrorComponent } from './backend-error.component';

describe('BackendErrorComponent', () => {
	let component: BackendErrorComponent;
	let fixture: ComponentFixture<BackendErrorComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				TranslateTestingModule.withTranslations({}).withDefaultLanguage('FR'),
				HttpClientTestingModule,
			],
			declarations: [
				BackendErrorComponent,
				MockDirective(TranslationListDirective),
			],
			providers: [MockProvider(ConfigurationService), provideMockStore({})],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(BackendErrorComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
