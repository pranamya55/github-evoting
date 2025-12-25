/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {LocationStrategy} from '@angular/common';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {MockStore} from '@ngrx/store/testing';
import {ProcessCancellationService} from '@vp/voter-portal-ui-services';
import {LanguageSelectorActions, SharedActions} from '@vp/voter-portal-ui-state';
import {MockComponent, MockProvider} from 'ng-mocks';
import {TranslateTestingModule} from 'ngx-translate-testing';

import {AppComponent} from './app.component';
import {HeaderComponent} from './header/header.component';
import {StepperComponent} from './stepper/stepper.component';
import {CompatibilityCheckComponent} from "./compatibility-check/compatibility-check.component";
import {MockStoreProvider, RandomItem, setState} from "@vp/shared-util-testing";
import {ActivatedRoute} from "@angular/router";
import {BehaviorSubject} from "rxjs";

describe('AppComponent', () => {
	let fixture: ComponentFixture<AppComponent>;
	let app: AppComponent;
	let store: MockStore;
	let popStateCallback: () => void;

	let queryParamMap: BehaviorSubject<Map<any, any>>;

	beforeEach(async () => {
		queryParamMap = new BehaviorSubject(new Map());

		const onPopState = jest
			.fn()
			.mockImplementation((callback) => (popStateCallback = callback));

		await TestBed.configureTestingModule({
			imports: [
				RouterTestingModule,
				TranslateTestingModule.withTranslations({}),
			],
			declarations: [
				AppComponent,
				MockComponent(HeaderComponent),
				MockComponent(StepperComponent),
				MockComponent(CompatibilityCheckComponent),
			],
			providers: [
				MockStoreProvider(),
				MockProvider(ProcessCancellationService),
				MockProvider(LocationStrategy, {onPopState}),
				MockProvider(ActivatedRoute, {queryParamMap} as unknown as ActivatedRoute)
			],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(AppComponent);
		app = fixture.componentInstance;
		store = TestBed.inject(MockStore);
	});

	describe('store registration', () => {
		it('should not register a store in the window object if "Cypress" is undefined', () => {
			window.Cypress = undefined;

			app.ngOnInit();

			expect(window.store).toBeFalsy();
		});

		it('should register the store in the window object if "Cypress" is defined', () => {
			window.Cypress = {};

			app.ngOnInit();

			expect(window.store).toEqual(store);
		});
	});

	describe('route exit handling', () => {
		let cancellationService: ProcessCancellationService;

		beforeEach(() => {
			cancellationService = TestBed.inject(ProcessCancellationService);
			app.ngOnInit();
		});

		it('should mark the back button as pressed with every new "popstate" event', () => {
			expect(cancellationService.backButtonPressed).toBeFalsy();

			popStateCallback();

			expect(cancellationService.backButtonPressed).toBeTruthy();
		});

		it('should dispatch a logout action on init when the user is authenticated', () => {
			const dispatchSpy = jest.spyOn(store, 'dispatch');

			setState(store, {isAuthenticated: true});
			app.ngOnInit();

			expect(dispatchSpy).toHaveBeenCalledWith(SharedActions.loggedOut());
		});
	});

	it('should not dispatch a logout action on init when the user is not authenticated ', () => {
		const dispatchSpy = jest.spyOn(store, 'dispatch');

		setState(store, {isAuthenticated: false});
		app.ngOnInit();

		expect(dispatchSpy).not.toHaveBeenCalledWith(SharedActions.loggedOut());
	});

	it('should dispatch a language selected action when a language is set in the query params', () => {
		const lang = RandomItem(['DE', 'FR', 'IT', 'EN']);
		const dispatchSpy = jest.spyOn(store, 'dispatch');

		app.ngOnInit();
		queryParamMap.next(new Map().set('lang', lang));

		expect(dispatchSpy).toHaveBeenCalledWith(
			LanguageSelectorActions.languageSelected({lang})
		);
	});
});
