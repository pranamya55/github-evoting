/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalCandidateComponent } from './modal-candidate.component';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { ClearableInputComponent } from '@vp/voter-portal-ui-components';
import {
	MockCandidate,
	MockElectionInformation,
	mockTranslateText,
	RandomBetween,
	RandomItem,
} from '@vp/shared-util-testing';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DestroyRef, SimpleChange } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import {
	CandidatePosition,
	Eligibility,
	List,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { ModalCandidateSelectorComponent } from '../modal-candidate-selector/modal-candidate-selector.component';
import {IconComponent} from "@vp/shared-ui-components";

describe('ModalCandidateComponent', () => {
	let component: ModalCandidateComponent;
	let fixture: ComponentFixture<ModalCandidateComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ModalCandidateComponent,
				MockComponent(ClearableInputComponent),
				MockComponent(ModalCandidateSelectorComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(TranslatePipe),
			],
			imports: [FormsModule, ReactiveFormsModule, IconComponent],
			providers: [MockProvider(DestroyRef), MockProvider(NgbActiveModal), MockComponent(IconComponent)],
		})
			.overrideComponent(ModalCandidateComponent, {
				set: {
					providers: [
						MockProvider(TranslateTextPipe, { transform: mockTranslateText }),
					],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ModalCandidateComponent);
		component = fixture.componentInstance;

		component.electionInformation = MockElectionInformation({ hasLists: true });
		component.chosenCandidate = MockCandidate();
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

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should update chosenCandidateIds when candidatesChosenInCurrentElection changes', () => {
		const chosenCandidate1 = {
			candidateIdentification: 'test-chosen-candidate-1',
		};
		const chosenCandidate2 = {
			candidateIdentification: 'test-chosen-candidate-2',
		};

		component.candidatesChosenInCurrentElection = [chosenCandidate1];
		component.ngOnChanges({
			candidatesChosenInCurrentElection: {} as SimpleChange,
		});

		expect(
			component['chosenCandidateIds'].has(
				chosenCandidate1.candidateIdentification,
			),
		).toBe(true);

		component.candidatesChosenInCurrentElection = [chosenCandidate2];
		component.ngOnChanges({
			candidatesChosenInCurrentElection: {} as SimpleChange,
		});

		expect(
			component['chosenCandidateIds'].has(
				chosenCandidate1.candidateIdentification,
			),
		).toBe(false);
		expect(
			component['chosenCandidateIds'].has(
				chosenCandidate2.candidateIdentification,
			),
		).toBe(true);
	});

	it('should sort candidates based on eligibility', () => {
		const candidateWithImplicitEligibility = MockCandidate({
			eligibility: Eligibility.IMPLICIT,
		});
		const candidateWithExplicitEligibility = MockCandidate({
			eligibility: Eligibility.EXPLICIT,
		});

		component.electionInformation.candidates = [
			candidateWithExplicitEligibility,
			candidateWithImplicitEligibility,
		];

		// Call ngAfterContentInit which sorts the candidates
		component.ngAfterContentInit();

		// Check that all non-explicit candidates are at the beginning
		expect(component['sortedCandidates']).toEqual([
			candidateWithImplicitEligibility,
			candidateWithExplicitEligibility,
		]);
	});

	it('should display all candidates when no search term is provided', () => {
		component.filters.controls.displayCandidateLine1.setValue('');

		expect(component.filteredCandidates.length).toEqual(
			component.electionInformation.candidates.length,
		);
	});

	describe('filters', () => {
		let randomList: List;

		const JohnDoe = MockCandidate({ displayCandidateLine1: 'John Doe' });
		const JaneSmith = MockCandidate({ displayCandidateLine1: 'Jane Smith' });

		beforeEach(() => {
			randomList = RandomItem(component.electionInformation.lists as List[]);
			randomList.candidatePositions = [
				{
					candidateIdentification: JaneSmith.candidateIdentification,
				} as CandidatePosition,
			];

			// Call ngAfterContentInit which gets the candidate identifications for each list
			component.ngAfterContentInit();

			component['sortedCandidates'] = [JohnDoe, JaneSmith];
		});

		it('should filter candidates based on displayCandidateLine1', () => {
			component.filters.controls.displayCandidateLine1.setValue('John');

			expect(component.filteredCandidates).toEqual([JohnDoe]);
		});

		it('should filter candidates based on onlyCandidatesChosenInCurrentElection', () => {
			component['chosenCandidateIds'] = new Set([
				JohnDoe.candidateIdentification,
			]);

			component.filters.controls.onlyCandidatesChosenInCurrentElection.setValue(
				true,
			);
			expect(component.filteredCandidates).toEqual([JohnDoe]);

			component.filters.controls.onlyCandidatesChosenInCurrentElection.setValue(
				false,
			);
			expect(component.filteredCandidates).toEqual([JohnDoe, JaneSmith]);
		});

		it('should filter candidates based on listIdentification', () => {
			component.filters.controls.listIdentification.setValue(
				randomList.listIdentification,
			);

			expect(component.filteredCandidates).toEqual([JaneSmith]);
		});
	});
});
