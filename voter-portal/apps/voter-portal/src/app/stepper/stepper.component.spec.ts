/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {DebugElement} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {Routes} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {RandomElectionEventId, RandomKey} from '@vp/shared-util-testing';
import {TranslateTestingModule} from 'ngx-translate-testing';
import {StepperComponent} from './stepper.component';
import {ExtendedFactor, VoterPortalConfig, VotingStep} from '@vp/voter-portal-util-types';
import {ConfigurationService} from '@vp/voter-portal-ui-services';
import {MockComponent, MockProvider} from 'ng-mocks';
import {IconComponent} from "@vp/shared-ui-components";

describe('stepperComponent', () => {
	let component: StepperComponent;
	let fixture: ComponentFixture<StepperComponent>;
	let stepper: DebugElement;
	let voterPortalConfig: VoterPortalConfig;
	let routes: Routes;

	beforeEach(async () => {
		voterPortalConfig = {
			identification: ExtendedFactor.YearOfBirth,
			contestsCapabilities: {
				writeIns: true,
			},
			requestTimeout: {
				authenticateVoter: 30000,
				sendVote: 120000,
				confirmVote: 120000
			},
			header: {
				logo: '',
				logoHeight: {desktop: 0, mobile: 0},
			},
			electionEventId: RandomElectionEventId(),
			favicon: ''
		};

		routes = Object.values(VotingStep).map((step) => ({
			path: step,
			children: [],
		}));

		await TestBed.configureTestingModule({
			imports: [
				RouterTestingModule.withRoutes(routes),
				TranslateTestingModule.withTranslations({}),
				MockComponent(IconComponent)
			],
			declarations: [StepperComponent],
			providers: [MockProvider(ConfigurationService, voterPortalConfig)],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(StepperComponent);
		component = fixture.componentInstance;
	});

	function setCurrentStep(step: VotingStep | null) {
		component.currentStep = step ?? undefined;
		fixture.detectChanges();
		stepper = fixture.debugElement.query(By.css('#voting-progress-stepper'));
	}

	describe('stepper not rendered', () => {
		it('should not show the stepper if there is no current step', () => {
			setCurrentStep(null);

			expect(stepper).toBeFalsy();
		});
	});

	describe('stepper rendered', () => {
		let steps: DebugElement[];

		beforeEach(() => {
			setCurrentStep(VotingStep[RandomKey(VotingStep)]);
			steps = stepper.queryAll(By.css('.stepper-item'));
		});

		it('should show the stepper if the current step has a step name', () => {
			expect(stepper).toBeTruthy();
		});

		it('should show all the steps that have a step name', () => {
			routes.forEach((_route, i) => {
				expect(steps[i].nativeElement.textContent).toContain(
					Object.values(VotingStep)[i]
				);
			});
		});

		it('should add an "aria-current" attribute to the current step only', () => {
			routes.forEach((route, i) => {
				const expectedAriaCurrent =
					route.path === component.currentStep ? 'step' : null;
				expect(steps[i].nativeElement.getAttribute('aria-current')).toBe(
					expectedAriaCurrent
				);
			});
		});
	});
});
