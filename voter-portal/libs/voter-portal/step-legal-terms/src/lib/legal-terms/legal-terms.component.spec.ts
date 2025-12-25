/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {provideMockStore} from '@ngrx/store/testing';
import {TranslateTestingModule} from 'ngx-translate-testing';
import {ConfigurationService} from '@vp/voter-portal-ui-services';
import {ExtendedFactor, VoterPortalConfig} from '@vp/voter-portal-util-types';
import {MockComponent, MockProvider} from 'ng-mocks';
import {RandomElectionEventId} from '@vp/shared-util-testing';
import {FormsModule} from '@angular/forms';
import {AnswerComponent, FooterComponentComponent,} from '@vp/voter-portal-ui-components';
import {IconComponent} from "@vp/shared-ui-components";
import {By} from "@angular/platform-browser";
import {focusFirstInvalidControl} from '@vp/voter-portal-util-helpers';
import {Store} from "@ngrx/store";
import {LegalTermsActions} from "@vp/voter-portal-ui-state";

import {LegalTermsComponent} from './legal-terms.component';

jest.mock('@vp/voter-portal-util-helpers', () => ({
	focusFirstInvalidControl: jest.fn(),
}));

describe('LegalTermsComponent', () => {
	let component: LegalTermsComponent;
	let fixture: ComponentFixture<LegalTermsComponent>;
	let voterPortalConfig: VoterPortalConfig;
	let store: Store;

	beforeEach(async () => {
		voterPortalConfig = {
			identification: ExtendedFactor.YearOfBirth,
			contestsCapabilities: {
				writeIns: true,
			},
			requestTimeout: {
				authenticateVoter: 30000,
				sendVote: 120000,
				confirmVote: 120000,
			},
			header: {
				logo: '',
				logoHeight: {
					desktop: 0,
					mobile: 0
				},
			},
			electionEventId: RandomElectionEventId(),
			favicon: '',
		};

		await TestBed.configureTestingModule({
			imports: [
				FormsModule,
				RouterTestingModule,
				HttpClientTestingModule,
				MockComponent(IconComponent),
				TranslateTestingModule.withTranslations({}),
			],
			declarations: [
				LegalTermsComponent,
				MockComponent(AnswerComponent),
				MockComponent(FooterComponentComponent),
			],
			providers: [
				provideMockStore({}),
				MockProvider(ConfigurationService, voterPortalConfig),
			],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(LegalTermsComponent);
		component = fixture.componentInstance;
		store = TestBed.inject(Store);
		fixture.detectChanges();
	});

	afterEach(() => {
		jest.resetAllMocks();
	})

	function submitForm() {
		const submit = fixture.debugElement.query(By.css('button[type="submit"]'));
		submit.nativeElement.click();
		fixture.detectChanges();
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('form submitted invalid', () => {
		it('should not agree to the legal terms', () => {
			const dispatchSpy = jest.spyOn(store, 'dispatch');
			submitForm();
			expect(dispatchSpy).not.toHaveBeenCalled();
		});

		it('should show an error', () => {
			expect(fixture.debugElement.query(By.css('#agreementrequired'))).toBeFalsy();
			submitForm();
			expect(fixture.debugElement.query(By.css('#agreementrequired'))).toBeTruthy();
		});

		it('should show invalid check boxes', () => {
			submitForm();

			const checkboxes = fixture.debugElement.queryAll(By.css('input[type="checkbox"]'));
			checkboxes.forEach(checkbox => {
				const isChecked = checkbox.nativeElement.matches(':checked')
				expect(checkbox.nativeElement.classList.contains('is-invalid')).toBe(!isChecked);
			});
		});

		it('should call focusFirstInvalidControl', () => {
			submitForm();
			expect(focusFirstInvalidControl).toHaveBeenCalledTimes(1);
		});
	});

	describe('valid form', () => {
		beforeEach(() => {
			fixture.debugElement.queryAll(By.css('input[type="checkbox"]')).forEach(checkbox => {
				checkbox.nativeElement.click();
			});
			fixture.detectChanges();
		});

		it('should not show an error', () => {
			expect(fixture.debugElement.query(By.css('#agreementrequired'))).toBeFalsy();
			submitForm();
			expect(fixture.debugElement.query(By.css('#agreementrequired'))).toBeFalsy();
		});

		it('should agree to the legal terms', () => {
			const dispatchSpy = jest.spyOn(store, 'dispatch');
			submitForm();
			expect(dispatchSpy).toHaveBeenNthCalledWith(1, LegalTermsActions.agreeClicked());
		});
	});
});
