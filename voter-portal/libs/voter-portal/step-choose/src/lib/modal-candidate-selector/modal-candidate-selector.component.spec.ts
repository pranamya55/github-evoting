/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalCandidateSelectorComponent } from './modal-candidate-selector.component';
import { MockComponent, MockPipe } from 'ng-mocks';
import { TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { ModalCandidateSelectorEnabledComponent } from '../modal-candidate-selector-enabled/modal-candidate-selector-enabled.component';
import { ModalCandidateSelectorDisabledComponent } from '../modal-candidate-selector-disabled/modal-candidate-selector-disabled.component';
import {
	MockElectionInformation,
	RandomBetween,
	RandomInt,
	RandomItem,
} from '@vp/shared-util-testing';
import { getAccumulation, isEligible } from '@vp/voter-portal-util-helpers';
import { By } from '@angular/platform-browser';

jest.mock('@vp/voter-portal-util-helpers', () => ({
	getAccumulation: jest.fn(),
	isEligible: jest.fn(),
}));

describe('ModalCandidateSelectorComponent', () => {
	let component: ModalCandidateSelectorComponent;
	let fixture: ComponentFixture<ModalCandidateSelectorComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ModalCandidateSelectorComponent,
				MockPipe(TranslateTextPipe),
				MockComponent(ModalCandidateSelectorEnabledComponent),
				MockComponent(ModalCandidateSelectorDisabledComponent),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalCandidateSelectorComponent);
		component = fixture.componentInstance;

		component.electionInformation = MockElectionInformation({
			minimumCandidateAccumulation: 2,
		});
		component.candidate = RandomItem(component.electionInformation.candidates);
		component.candidatesChosenInCurrentElection =
			component.electionInformation.candidates.map((candidate) => {
				return {
					candidateIdentification: RandomBetween(
						candidate.candidateIdentification,
						null,
					),
				};
			});

		fixture.detectChanges();
	});

	function getEnabledSelector() {
		return fixture.debugElement.query(
			By.directive(ModalCandidateSelectorEnabledComponent),
		);
	}

	function getDisabledSelector() {
		return fixture.debugElement.query(
			By.directive(ModalCandidateSelectorDisabledComponent),
		);
	}

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('should initialize with correct eligibility and accumulation', () => {
		const randomBoolean = RandomBetween(true, false);
		const randomNumber = RandomInt();
		(isEligible as jest.Mock).mockReturnValue(randomBoolean);
		(getAccumulation as jest.Mock).mockReturnValue(randomNumber);

		component.ngAfterContentInit();

		expect(component.isEligible).toBe(randomBoolean);
		expect(component.candidateAccumulation).toBe(randomNumber);
	});

	it('should set hasReachedMaximumAccumulation to true when accumulation matches maximum', () => {
		const maxCandidateAccumulation =
			component.electionInformation.election.candidateAccumulation;
		(getAccumulation as jest.Mock).mockReturnValue(maxCandidateAccumulation);

		component.ngAfterContentInit();

		expect(component.hasReachedMaximumAccumulation).toBe(true);
	});

	it('should set hasReachedMaximumAccumulation to false when accumulation is less than the maximum', () => {
		const maxCandidateAccumulation =
			component.electionInformation.election.candidateAccumulation;
		(getAccumulation as jest.Mock).mockReturnValue(
			maxCandidateAccumulation - 1,
		);

		component.ngAfterContentInit();

		expect(component.hasReachedMaximumAccumulation).toBe(false);
	});

	it('should render the enabled selector when the candidate is eligible, not selected on the current position and has not reached the maximum accumulation', () => {
		component.isEligible = true;
		component.isSelectedOnCurrentPosition = false;
		component.hasReachedMaximumAccumulation = false;

		fixture.detectChanges();

		expect(getEnabledSelector()).toBeTruthy();
		expect(getDisabledSelector()).toBeNull();
	});

	it('should render the disabled selector when the candidate is not eligible', () => {
		component.isEligible = false;

		fixture.detectChanges();

		expect(getDisabledSelector()).toBeTruthy();
		expect(getEnabledSelector()).toBeNull();
	});

	it('should render the disabled selector when the candidate is selected on the current position', () => {
		component.isSelectedOnCurrentPosition = true;

		fixture.detectChanges();

		expect(getDisabledSelector()).toBeTruthy();
		expect(getEnabledSelector()).toBeNull();
	});

	it('should render the disabled selector when the candidate has reached the maximum accumulation', () => {
		component.hasReachedMaximumAccumulation = true;

		fixture.detectChanges();

		expect(getDisabledSelector()).toBeTruthy();
		expect(getEnabledSelector()).toBeNull();
	});
});
