/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateTestingModule} from 'ngx-translate-testing';
import {InitializationPageComponent} from './initialization-page.component';
import {RouterTestingModule} from '@angular/router/testing';
import {provideMockStore} from '@ngrx/store/testing';

describe('InitializationPageComponent', () => {
	let component: InitializationPageComponent;
	let fixture: ComponentFixture<InitializationPageComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				RouterTestingModule,
				TranslateTestingModule.withTranslations({}),
			],
			providers: [provideMockStore({initialState: {}})],
			declarations: [InitializationPageComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(InitializationPageComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
