/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	ComponentFixture,
	fakeAsync,
	TestBed,
	tick,
} from '@angular/core/testing';
import { ChooseElectionCandidateComponent } from './choose-election-candidate.component';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
	Candidate,
	ChosenCandidate,
	ElectionInformation,
	Eligibility,
	EmptyPosition,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { DebugElement, DestroyRef, SimpleChange } from '@angular/core';
import { ChooseElectionComponent } from '../choose-election/choose-election.component';
import { ChooseFormService } from '../choose-form.service';
import { TranslateModule } from '@ngx-translate/core';
import { AnswerComponent, UiComponentsModule } from '@vp/voter-portal-ui-components';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { ModalCandidateComponent } from '../modal-candidate/modal-candidate.component';
import { ChooseElectionCandidateWriteInComponent } from '../choose-election-candidate-write-in/choose-election-candidate-write-in.component';
import {IconComponent} from "@vp/shared-ui-components";

const MockChooseFormService = {
	createChosenCandidateFormGroup: jest.fn().mockReturnValue(
		new FormGroup({
			candidateIdentification: new FormControl(),
			candidateListIdentification: new FormControl(),
		}),
	),
	createWriteInAnswer: jest.fn().mockReturnValue(
		new FormGroup({
			writeIn: new FormControl(),
		}),
	),
};

describe('ChooseElectionCandidateComponent', () => {
	let component: ChooseElectionCandidateComponent;
	let fixture: ComponentFixture<ChooseElectionCandidateComponent>;
	let modalOpen: jest.Mock;

	const emptyPosition = {
		positionOnList: 1,
		emptyPositionIdentification: 'test-empty-position-id',
	} as EmptyPosition;
	const candidate = {
		candidateIdentification: 'test-candidate-id',
		eligibility: Eligibility.EXPLICIT,
	} as Candidate;
	const implicitlyEligibleCandidate = {
		candidateIdentification: 'test-implicitly-eligible-candidate-id',
		eligibility: Eligibility.IMPLICIT,
	} as Candidate;
	const writeInPosition = {
		writeInPositionIdentification: 'test-write-in-id',
		position: emptyPosition.positionOnList,
	};
	const modalRef = {
		componentInstance: {},
		result: Promise.resolve(undefined),
	};

	beforeEach(async () => {
		modalOpen = jest.fn().mockReturnValue(modalRef);

		await TestBed.configureTestingModule({
			declarations: [
				ChooseElectionCandidateComponent,
				MockComponent(AnswerComponent),
				MockComponent(ChooseElectionCandidateWriteInComponent),
				MockPipe(TranslateTextPipe),
			],
			imports: [
				MockModule(ReactiveFormsModule),
				MockModule(TranslateModule),
				MockModule(UiComponentsModule),
				MockModule(UiDirectivesModule),
				MockModule(NgbModule),
				MockComponent(IconComponent)
			],
			providers: [
				MockProvider(NgbModal, {
					open: modalOpen,
				}),
				MockProvider(DestroyRef),
			],
		})
			.overrideComponent(ChooseElectionComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseElectionCandidateComponent);
		component = fixture.componentInstance;

		component.emptyPosition = emptyPosition;
		component.electionInformation = {
			election: {
				electionIdentification: 'test-election-id',
			},
			candidates: [candidate, implicitlyEligibleCandidate],
			lists: [],
			writeInPositions: [writeInPosition],
		} as unknown as ElectionInformation;

		fixture.detectChanges();
	});

	function allowWriteIns() {
		component.electionInformation.election.writeInsAllowed = true;
		component.ngAfterContentInit();
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should update chosenCandidate when chosenCandidateAnswer value changes', () => {
		component.chosenCandidateFormGroup.controls.candidateIdentification.setValue(
			candidate.candidateIdentification,
		);
		expect(component.chosenCandidate).toEqual(candidate);

		component.chosenCandidateFormGroup.controls.candidateIdentification.setValue(
			null,
		);
		expect(component.chosenCandidate).toBeUndefined();
	});

	it('should not initialize writeInAnswer form group', () => {
		expect(component.chosenWriteInFormGroup).toBeUndefined();
	});

	it('should initialize writeInAnswer form group when writeInsAllowed is true', () => {
		allowWriteIns();

		expect(component.chosenWriteInFormGroup).toBeDefined();
		expect(component.chosenWriteInFormGroup?.value).toEqual({
			writeInPositionIdentification:
				writeInPosition.writeInPositionIdentification,
			writeIn: null,
		});
	});

	describe('candidate eligibility', () => {
		function setChosenCandidate(candidate: Candidate) {
			component.chosenCandidateFormGroup.controls.candidateIdentification.setValue(
				candidate.candidateIdentification,
			);
		}

		it('should not reset the chosenCandidateAnswer and set isCandidateEligible to true when candidatesChosenInPrimaryElection is undefined', () => {
			component.candidatesChosenInPrimaryElection = undefined;
			setChosenCandidate(candidate);

			expect(component.isCandidateEligible).toBe(true);
			expect(
				component.chosenCandidateFormGroup.value.candidateIdentification,
			).toBe(candidate.candidateIdentification);
		});

		it('should not reset the chosenCandidateAnswer and set isCandidateEligible to true when chosenCandidate.eligibility is implicit', () => {
			component.candidatesChosenInPrimaryElection = [];
			setChosenCandidate(implicitlyEligibleCandidate);

			expect(component.isCandidateEligible).toBe(true);
			expect(
				component.chosenCandidateFormGroup.value.candidateIdentification,
			).toBe(implicitlyEligibleCandidate.candidateIdentification);
		});

		it('should not reset the chosenCandidateAnswer and set isCandidateEligible to true when the candidate is selected in the primary election', () => {
			component.candidatesChosenInPrimaryElection = [candidate];
			setChosenCandidate(candidate);

			expect(component.isCandidateEligible).toBe(true);
			expect(
				component.chosenCandidateFormGroup.value.candidateIdentification,
			).toBe(candidate.candidateIdentification);
		});

		it('should reset the chosenCandidateAnswer and set isCandidateEligible to false when the candidate is not selected in the primary election', () => {
			component.candidatesChosenInPrimaryElection = [];
			setChosenCandidate(candidate);

			expect(component.isCandidateEligible).toBe(false);
			expect(
				component.chosenCandidateFormGroup.value.candidateIdentification,
			).toBeNull();
		});
	});

	describe('candidateAccumulation', () => {
		function setCandidatesChosenInCurrentElection(
			candidatesChosenInCurrentElection: ChosenCandidate[],
		) {
			component.candidatesChosenInCurrentElection =
				candidatesChosenInCurrentElection;
			component.ngOnChanges({
				candidatesChosenInCurrentElection: {
					currentValue: candidatesChosenInCurrentElection,
				} as SimpleChange,
			});
		}

		it('should update the candidateAccumulation when candidatesChosenInCurrentElection changes', () => {
			component.chosenCandidate = candidate;

			setCandidatesChosenInCurrentElection([]);
			expect(component.candidateAccumulation).toBe(0);

			setCandidatesChosenInCurrentElection([
				{ candidateIdentification: candidate.candidateIdentification },
			]);
			expect(component.candidateAccumulation).toBe(1);

			setCandidatesChosenInCurrentElection([
				{ candidateIdentification: candidate.candidateIdentification },
				{ candidateIdentification: candidate.candidateIdentification },
			]);
			expect(component.candidateAccumulation).toBe(2);
		});
	});

	describe('controls', () => {
		let selectionButton: DebugElement;
		let deletionButton: DebugElement;
		let accumulationButton: DebugElement;

		beforeEach(() => {
			allowWriteIns();
		});

		function getControls() {
			selectionButton = fixture.debugElement.query(
				By.css(
					`#candidate-${component.positionOnList}-election-${component.electionIdentification}-selection`,
				),
			);
			deletionButton = fixture.debugElement.query(
				By.css(
					`#candidate-${component.positionOnList}-election-${component.electionIdentification}-deletion`,
				),
			);
			accumulationButton = fixture.debugElement.query(
				By.css(
					`#candidate-${component.positionOnList}-election-${component.electionIdentification}-accumulation`,
				),
			);
		}

		const getSelectionFeedback = () =>
			fixture.debugElement.query(
				By.css(
					`#feedback-${component.positionOnList}-election-${component.electionIdentification}-candidateSelected`,
				),
			);
		const getDeletionFeedback = () =>
			fixture.debugElement.query(
				By.css(
					`#feedback-${component.positionOnList}-election-${component.electionIdentification}-candidateDeleted`,
				),
			);

		const getAccumulationFeedback = () =>
			fixture.debugElement.query(
				By.css(
					`#feedback-${component.positionOnList}-election-${component.electionIdentification}-candidateAccumulated`,
				),
			);

		describe('position is empty', () => {
			beforeEach(() => {
				getControls();
			});

			it('should not display a deletion button', () => {
				expect(deletionButton).toBeNull();
			});

			it('should not display an accumulation button', () => {
				expect(accumulationButton).toBeNull();
			});

			it('should display a selection button', () => {
				expect(selectionButton).toBeTruthy();
			});

			it('should open ModalCandidateComponent when clicking the selection button', () => {
				selectionButton.nativeElement.click();
				expect(modalOpen).toHaveBeenNthCalledWith(
					1,
					ModalCandidateComponent,
					expect.any(Object),
				);
			});

			it('should select the candidate when the modal result resolves with a chosenCandidate', () => {
				modalOpen.mockReturnValueOnce({
					...modalRef,
					result: Promise.resolve(candidate),
				});
				selectionButton.nativeElement.click();

				expect(
					component.chosenCandidateFormGroup.value.candidateIdentification,
				).toBe(candidate.candidateIdentification);
				expect(component.chosenWriteInFormGroup?.value.writeIn).toBeNull();
			});

			it('should initiate a write-in when the modal result resolves with undefined', () => {
				modalOpen.mockReturnValueOnce({
					...modalRef,
					result: Promise.resolve(undefined),
				});
				selectionButton.nativeElement.click();

				expect(component.chosenWriteInFormGroup?.value.writeIn).toBe('');
				expect(
					component.chosenCandidateFormGroup.value.candidateIdentification,
				).toBeNull();
			});

			it('should show the selection feedback when the modal result resolves', () => {
				expect(getSelectionFeedback()).toBeNull();

				selectionButton.nativeElement.focus();
				selectionButton.nativeElement.click();
				fixture.detectChanges();
				expect(getSelectionFeedback()).toBeTruthy();
			});
		});

		describe('position is not empty', () => {
			beforeEach(() => {
				component.selectCandidate(candidate);
				fixture.detectChanges();
			});

			describe('accumulation is 1', () => {
				beforeEach(() => {
					getControls();
				});

				it('should not display an accumulation button', () => {
					expect(accumulationButton).toBeNull();
				});

				it('should display a selection button', () => {
					expect(selectionButton).toBeTruthy();
				});

				it('should display a deletion button', () => {
					expect(deletionButton).toBeTruthy();
				});

				it('should reset both chosenCandidateAnswer and writeInAnswer when the deletion button is clicked', () => {
					deletionButton.nativeElement.click();

					expect(
						component.chosenCandidateFormGroup.value.candidateIdentification,
					).toBeNull();
					expect(component.chosenWriteInFormGroup?.value.writeIn).toBeNull();
				});

				it('should show the deletion feedback when the deletion button is clicked until the selection button blurs', fakeAsync(() => {
					expect(getDeletionFeedback()).toBeNull();

					deletionButton.nativeElement.click();
					fixture.detectChanges();
					expect(getDeletionFeedback()).toBeTruthy();

					tick(); // wait for the selection button to receive focus

					selectionButton.nativeElement.blur();
					fixture.detectChanges();
					expect(getDeletionFeedback()).toBeNull();
				}));
			});

			describe('accumulation is greater than 1', () => {
				beforeEach(() => {
					component.electionInformation.election.candidateAccumulation = 2;
					component.isCandidateEligible = true;
					component.nextAvailablePosition = 0;
					fixture.detectChanges();

					getControls();
				});

				it('should display a selection button', () => {
					expect(selectionButton).toBeTruthy();
				});

				it('should display a deletion button', () => {
					expect(deletionButton).toBeTruthy();
				});

				it('should display an accumulation button', () => {
					expect(accumulationButton).toBeTruthy();
				});

				it('should emit a candidateAccumulated event when the accumulation button is clicked', (done) => {
					component.candidateAccumulated.subscribe((emittedCandidate) => {
						expect(emittedCandidate).toEqual(component.chosenCandidate);
						done();
					});

					accumulationButton.nativeElement.click();
				});

				it('should show the accumulation feedback when the accumulation button is clicked until it blurs', () => {
					expect(getAccumulationFeedback()).toBeNull();

					accumulationButton.nativeElement.focus();
					accumulationButton.nativeElement.click();
					fixture.detectChanges();
					expect(getAccumulationFeedback()).toBeTruthy();

					accumulationButton.nativeElement.blur();
					fixture.detectChanges();
					expect(getAccumulationFeedback()).toBeNull();
				});

				it('should disable the accumulation button and show a feedback when isCandidateEligible is false', () => {
					component.isCandidateEligible = false;
					fixture.detectChanges();

					const notEligibleFeedback = fixture.debugElement.query(
						By.css(
							`#feedback-${component.positionOnList}-election-${component.electionIdentification}-notEligible`,
						),
					);
					expect(notEligibleFeedback).toBeTruthy();
					expect(accumulationButton.nativeElement.disabled).toBe(true);
				});

				it('should disable the accumulation button and show a feedback when nextAvailablePosition is false', () => {
					component.nextAvailablePosition = -1;
					fixture.detectChanges();

					const noMoreSeatsFeedback = fixture.debugElement.query(
						By.css(
							`#feedback-${component.positionOnList}-election-${component.electionIdentification}-noMoreSeats`,
						),
					);
					expect(noMoreSeatsFeedback).toBeTruthy();
					expect(accumulationButton.nativeElement.disabled).toBe(true);
				});

				it('should disable the accumulation button and show a feedback when candidateAccumulation is equal to election.candidateAccumulation', () => {
					component.candidateAccumulation =
						component.electionInformation.election.candidateAccumulation;
					fixture.detectChanges();

					const maxAccumulationReachedFeedback = fixture.debugElement.query(
						By.css(
							`#feedback-${component.positionOnList}-election-${component.electionIdentification}-maxAccumulationReached`,
						),
					);
					expect(maxAccumulationReachedFeedback).toBeTruthy();
					expect(accumulationButton.nativeElement.disabled).toBe(true);
				});
			});
		});
	});
});
