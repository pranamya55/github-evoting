/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	ControlContainer,
	FormArray,
	FormControl,
	FormGroup,
	ReactiveFormsModule,
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import {
	Candidate,
	ElectionInformationAnswers,
	ElectionRelation,
	SortByPipe,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { FAQSection } from '@vp/voter-portal-util-types';
import {
	MockElectionInformation,
	mockSortBy,
	MockStoreProvider,
} from '@vp/shared-util-testing';
import { ChooseElectionListComponent } from '../choose-election-list/choose-election-list.component';
import { ChooseElectionCandidateComponent } from '../choose-election-candidate/choose-election-candidate.component';
import { ChooseElectionComponent } from './choose-election.component';
import { AccordionElectionComponent } from '@vp/voter-portal-ui-components';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { DestroyRef } from '@angular/core';
import { ChooseFormService } from '../choose-form.service';
import {IconComponent} from "@vp/shared-ui-components";

const MockChooseFormService = {
	createElectionInformationAnswersFormGroup: jest.fn().mockReturnValue(
		new FormGroup({
			chosenCandidates: new FormArray([]),
			emptyListIds: new FormGroup({
				emptyPositionIds: new FormArray([]),
			}),
		}),
	),
};

describe('ChooseElectionComponent', () => {
	let component: ChooseElectionComponent;
	let fixture: ComponentFixture<ChooseElectionComponent>;
	let electionsInformation: FormArray;
	let faqService: FAQService;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ChooseElectionComponent,
				MockComponent(ChooseElectionListComponent),
				MockComponent(ChooseElectionCandidateComponent),
				MockComponent(AccordionElectionComponent),
				MockPipe(TranslateTextPipe),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [MockModule(ReactiveFormsModule), MockModule(TranslateModule), MockComponent(IconComponent),],
			providers: [
				MockProvider(DestroyRef),
				MockProvider(ControlContainer, {
					control: new FormArray([]),
				}),
				MockProvider(FAQService, {
					showFAQ: jest.fn(),
				}),
				MockStoreProvider({ hasSubmittedAnswer: false }),
			],
		})
			.overrideComponent(ChooseElectionComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseElectionComponent);
		component = fixture.componentInstance;
		electionsInformation = TestBed.inject(ControlContainer)
			.control as FormArray;
		faqService = TestBed.inject(FAQService);

		component.electionInformation = MockElectionInformation({
			hasLists: false,
		});

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('instructions', () => {
		const getAccumulationInstructions = () =>
			fixture.debugElement.query(
				By.css('[data-test="accumulation-instruction"]'),
			);
		const getWriteInsFaqButton = () =>
			fixture.debugElement.query(By.css('[data-test="writeins-faq-button"]'));

		it('should show accumulation instructions when candidateAccumulation is greater than 1', () => {
			component.electionInformation.election.candidateAccumulation = 1;
			fixture.detectChanges();
			expect(getAccumulationInstructions()).toBeNull();

			component.electionInformation.election.candidateAccumulation = 2;
			fixture.detectChanges();
			expect(getAccumulationInstructions()).toBeTruthy();
		});

		it('should show write-ins FAQ button when writeInsAllowed is true', () => {
			expect(getWriteInsFaqButton()).toBeNull();

			component.electionInformation.election.writeInsAllowed = true;
			fixture.detectChanges();

			expect(getWriteInsFaqButton()).toBeTruthy();
		});

		it('should call FAQService.showFAQ when clicking the write-ins FAQ button', () => {
			component.electionInformation.election.writeInsAllowed = true;
			fixture.detectChanges();
			getWriteInsFaqButton().nativeElement.click();

			expect(faqService.showFAQ).toHaveBeenNthCalledWith(
				1,
				FAQSection.HowToUseWriteIns,
			);
		});
	});

	describe('current election changes', () => {
		const mockCandidate = {
			candidateIdentification: 'test-candidate-id',
		};

		beforeEach(() => {
			const chosenCandidateFormGroups =
				component.electionInformation.emptyList.emptyPositions.map(
					() =>
						new FormGroup({
							candidateIdentification: new FormControl<string | null>(null),
						}),
				);

			component.electionInformationAnswerFormGroup.controls.chosenCandidates =
				new FormArray(chosenCandidateFormGroups);

			component.ngAfterViewInit();
		});

		function selectCandidateOnFirstPosition() {
			component.electionInformationAnswerFormGroup.controls.chosenCandidates
				.at(0)
				.patchValue({
					candidateIdentification: mockCandidate.candidateIdentification,
				});
		}

		it('should update candidatesChosenInCurrentElection when chosenCandidates change', () => {
			expect(component.candidatesChosenInCurrentElection).toEqual([]);

			selectCandidateOnFirstPosition();

			expect(component.candidatesChosenInCurrentElection).toEqual([
				{ candidateIdentification: mockCandidate.candidateIdentification },
			]);
		});

		it('should update chosenCandidateCount when chosenCandidates change', () => {
			expect(component.chosenCandidateCount).toBe(0);

			selectCandidateOnFirstPosition();

			expect(component.chosenCandidateCount).toBe(1);
		});

		it('should update nextAvailablePosition when chosenCandidates change', () => {
			expect(component.nextAvailablePosition).toEqual(0);

			selectCandidateOnFirstPosition();

			expect(component.nextAvailablePosition).toEqual(1);
		});

		it('should accumulate candidate at on the nextAvailablePosition', () => {
			selectCandidateOnFirstPosition();

			component.accumulateCandidate(mockCandidate as Candidate);

			const nextAvailablePositionControl =
				component.electionInformationAnswerFormGroup.controls.chosenCandidates.at(
					1,
				);
			expect(nextAvailablePositionControl.value).toEqual({
				candidateIdentification: mockCandidate.candidateIdentification,
			});
		});
	});

	describe('primary election changes', () => {
		const chosenCandidateWithId = {
			candidateIdentification: 'test-primary-election-candidate-id',
		};

		const chosenCandidateWithoutId = {
			candidateIdentification: null,
		};

		const primaryElectionInformationAnswers = {
			electionIdentification: 'test-primary-election-id',
			chosenCandidates: [chosenCandidateWithoutId, chosenCandidateWithId],
		} as ElectionInformationAnswers;

		const createPrimaryElection = () => {
			// create primary election control
			const chosenCandidateFormGroups =
				primaryElectionInformationAnswers.chosenCandidates.map(
					() =>
						new FormGroup({
							candidateIdentification: new FormControl(null),
						}),
				);

			electionsInformation.insert(
				0,
				new FormGroup({
					electionIdentification: new FormControl(
						primaryElectionInformationAnswers.electionIdentification,
					),
					chosenCandidates: new FormArray(chosenCandidateFormGroups),
				}),
			);

			// create primary election reference
			component.electionInformation.election.referencedElectionsInformation = [
				{
					electionRelation: ElectionRelation.PRIMARY,
					referencedElection:
						primaryElectionInformationAnswers.electionIdentification,
				},
			];

			component.ngAfterContentInit();
			component.ngAfterViewInit();
		};

		it('should not set candidatesChosenInPrimaryElection when there is no referenced primary election', () => {
			component.ngAfterViewInit();
			expect(component.candidatesChosenInPrimaryElection).toBeUndefined();
		});

		it('should set candidatesChosenInPrimaryElection to an empty array when there is a referenced primary election', () => {
			createPrimaryElection();

			expect(component.candidatesChosenInPrimaryElection).toEqual([]);
		});

		it('should add chosenCandidates with an actual candidateIdentification to the candidatesChosenInPrimaryElection', () => {
			createPrimaryElection();

			electionsInformation.patchValue([primaryElectionInformationAnswers]);

			expect(component.candidatesChosenInPrimaryElection).not.toContain(
				chosenCandidateWithId,
			);
		});

		it('should not add chosenCandidates with a null candidateIdentification to the candidatesChosenInPrimaryElection', () => {
			createPrimaryElection();

			expect(component.candidatesChosenInPrimaryElection).not.toContain(
				chosenCandidateWithoutId,
			);
		});
	});
});
