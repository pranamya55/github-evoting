/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import {
	AbstractControl,
	ControlContainer,
	FormArray,
	FormBuilder,
	FormGroup,
} from '@angular/forms';
import {
	MockElectionInformation,
	MockElectionTexts,
	MockFormArray,
	MockStandardAnswer,
	MockStandardQuestion,
	MockVoteTexts,
	RandomArray,
	RandomInt,
	RandomString,
} from '@vp/shared-util-testing';
import {
	ChosenCandidate,
	ChosenList,
	ChosenWriteIn,
	ElectionAnswers,
	ElectionInformation,
	ElectionInformationAnswers,
	ElectionTexts,
	StandardAnswer,
	StandardQuestion,
	VoteAnswers,
	VoteTexts,
	WriteInPosition,
} from 'e-voting-libraries-ui-kit';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import { MockProvider } from 'ng-mocks';

import { ChooseFormService } from './choose-form.service';

describe('ChooseFormService', () => {
	let service: ChooseFormService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				ChooseFormService,
				FormBuilder,
				MockProvider(ControlContainer),
			],
		});
	});

	function updateControlContainer(control: AbstractControl) {
		TestBed.overrideProvider(
			ControlContainer,
			MockProvider(ControlContainer, { control }),
		);
		service = TestBed.inject(ChooseFormService);
	}

	describe('createVoterAnswersFormGroup', () => {
		beforeEach(() => {
			service = TestBed.inject(ChooseFormService);
		});

		it('should not create a voteAnswers if there is no voteTexts', () => {
			const voterAnswersFormGroup = service.createVoterAnswersFormGroup(
				[],
				expect.any(Array),
			);
			expect(voterAnswersFormGroup.get('voteAnswers')).toBeNull();
		});

		it('should create a voteAnswers if there is voteTexts', () => {
			const voterAnswersFormGroup = service.createVoterAnswersFormGroup(
				RandomArray(MockVoteTexts),
				expect.any(Array),
			);
			expect(voterAnswersFormGroup.get('voteAnswers')).toBeTruthy();
		});

		it('should not create a electionAnswers if there is no electionTexts', () => {
			const voterAnswersFormGroup = service.createVoterAnswersFormGroup(
				expect.any(Array),
				[],
			);
			expect(voterAnswersFormGroup.get('electionAnswers')).toBeNull();
		});

		it('should create a electionAnswers if there is electionTexts', () => {
			const voterAnswersFormGroup = service.createVoterAnswersFormGroup(
				expect.any(Array),
				RandomArray(MockElectionTexts),
			);
			expect(voterAnswersFormGroup.get('electionAnswers')).toBeTruthy();
		});
	});

	describe('createVoteAnswersFormGroup', () => {
		let voteTexts: VoteTexts;
		let voteAnswerFormGroup: FormGroupFrom<VoteAnswers>;
		let parentControl: FormArray;

		beforeEach(() => {
			voteTexts = MockVoteTexts();

			parentControl = MockFormArray(voteTexts.votePosition);
			updateControlContainer(parentControl);

			voteAnswerFormGroup = service.createVoteAnswersFormGroup(voteTexts);
		});

		it('should initialize voteAnswer form group correctly', () => {
			expect(voteAnswerFormGroup).toBeDefined();
			expect(voteAnswerFormGroup.value).toEqual({
				voteIdentification: voteTexts.voteIdentification,
				votePosition: voteTexts.votePosition,
				chosenAnswers: [],
			});
		});

		it('should add voteAnswer form group to the parent FormArray', () => {
			expect(parentControl.at(voteTexts.votePosition)).toBe(
				voteAnswerFormGroup,
			);
		});
	});

	describe('createChosenAnswerFormGroup', () => {
		let ballotIdentification: string;
		let question: StandardQuestion;
		let emptyAnswer: StandardAnswer;
		let parentControl: FormArray;

		beforeEach(() => {
			ballotIdentification = RandomString();
			question = MockStandardQuestion();
			emptyAnswer = MockStandardAnswer();

			parentControl = MockFormArray(question.questionPosition);
			updateControlContainer(parentControl);
		});

		it('should initialize chosenAnswer form group correctly', () => {
			const chosenAnswerFormGroup = service.createChosenAnswerFormGroup(
				ballotIdentification,
				question,
			);

			expect(chosenAnswerFormGroup).toBeDefined();
			expect(chosenAnswerFormGroup.value).toEqual({
				ballotIdentification: ballotIdentification,
				questionIdentification: question.questionIdentification,
				answerIdentification: '',
			});
		});

		it('should initialize answerIdentification with the empty answer identification if there is one', () => {
			const chosenAnswerFormGroup = service.createChosenAnswerFormGroup(
				ballotIdentification,
				question,
				emptyAnswer,
			);

			expect(chosenAnswerFormGroup.controls.answerIdentification.value).toBe(
				emptyAnswer.answerIdentification,
			);
		});

		it('should add chosenAnswer form group to the parent FormArray', () => {
			const chosenAnswerFormGroup = service.createChosenAnswerFormGroup(
				ballotIdentification,
				question,
				emptyAnswer,
			);

			expect(parentControl.at(question.questionPosition)).toBe(
				chosenAnswerFormGroup,
			);
		});
	});

	describe('createElectionAnswersFormGroup', () => {
		let electionTexts: ElectionTexts;
		let electionAnswerFormGroup: FormGroupFrom<ElectionAnswers>;
		let parentControl: FormArray;

		beforeEach(() => {
			electionTexts = MockElectionTexts();

			parentControl = MockFormArray(electionTexts.electionGroupPosition);
			updateControlContainer(parentControl);

			electionAnswerFormGroup =
				service.createElectionAnswersFormGroup(electionTexts);
		});

		it('should initialize electionAnswer form group correctly', () => {
			expect(electionAnswerFormGroup).toBeDefined();
			expect(electionAnswerFormGroup.value).toEqual({
				electionGroupIdentification: electionTexts.electionGroupIdentification,
				electionGroupPosition: electionTexts.electionGroupPosition,
				electionsInformation: [],
			});
		});

		it('should add voteAnswer form group to the parent FormArray', () => {
			expect(parentControl.at(electionTexts.electionGroupPosition)).toBe(
				electionAnswerFormGroup,
			);
		});
	});

	describe('createElectionInformationAnswersFormGroup', () => {
		let electionInformation: ElectionInformation;
		let electionInformationAnswerFormGroup: FormGroupFrom<ElectionInformationAnswers>;
		let parentControl: FormArray;

		const createElectionInformationAnswersFormGroup = (options = {}) => {
			electionInformation = MockElectionInformation(options);

			parentControl = MockFormArray(
				electionInformation.election.electionPosition,
			);
			updateControlContainer(parentControl);

			electionInformationAnswerFormGroup =
				service.createElectionInformationAnswersFormGroup(electionInformation);
		};

		it('should initialize electionInformationAnswer form group correctly', () => {
			createElectionInformationAnswersFormGroup();

			expect(electionInformationAnswerFormGroup).toBeDefined();
			expect(electionInformationAnswerFormGroup.value).toEqual({
				electionIdentification: electionInformation.election.electionIdentification,
				chosenCandidates: [],
				emptyListIds: {
					listIdentification: electionInformation.emptyList.listIdentification,
					emptyPositionIds: expect.any(Array),
				},
			});
		});

		it('should initialize emptyPositionIds form group correctly', () => {
			createElectionInformationAnswersFormGroup();

			const emptyPositionIds = electionInformation.emptyList.emptyPositions.map(
				(emptyPosition) => ({
					emptyPositionIdentification: emptyPosition.emptyPositionIdentification,
				}),
			);

			expect(
				electionInformationAnswerFormGroup.value.emptyListIds?.emptyPositionIds,
			).toEqual(emptyPositionIds);
		});

		it('should add chosenWriteIns control if writeInsAllowed is true', () => {
			createElectionInformationAnswersFormGroup({ writeInsAllowed: true });

			expect(
				electionInformationAnswerFormGroup.controls.chosenWriteIns,
			).toBeDefined();
		});

		it('should add electionInformationAnswer form group to the parent FormArray', () => {
			createElectionInformationAnswersFormGroup();

			expect(
				parentControl.at(electionInformation.election.electionPosition),
			).toBe(electionInformationAnswerFormGroup);
		});
	});

	describe('createChosenListFormGroup', () => {
		let electionInformation: ElectionInformation;
		let chosenListFormGroup: FormGroupFrom<ChosenList>;
		let parentControl: FormGroup;

		beforeEach(() => {
			electionInformation = MockElectionInformation();

			parentControl = new FormGroup<any>({});
			updateControlContainer(parentControl);

			chosenListFormGroup =
				service.createChosenListFormGroup(electionInformation);
		});

		it('should initialize chosenListAnswer form group correctly', () => {
			expect(chosenListFormGroup).toBeDefined();
			expect(chosenListFormGroup.value).toEqual({
				listIdentification: electionInformation.emptyList.listIdentification,
			});
		});

		it('should add chosenListAnswer form group to the parent FormGroup', () => {
			expect(parentControl.get('chosenList')).toBeDefined();
		});
	});

	describe('createChosenCandidateFormGroup', () => {
		let electionInformation: ElectionInformation;
		let candidatePosition: number = 0; // Initialize candidatePosition with a default value
		let chosenCandidateFormGroup: FormGroupFrom<ChosenCandidate>;
		let parentControl: FormArray;

		const createChosenCandidateFormGroup = (options = {}) => {
			electionInformation = MockElectionInformation(options);

			parentControl = MockFormArray(candidatePosition);
			updateControlContainer(
				new FormGroup({ chosenCandidates: parentControl }),
			);

			chosenCandidateFormGroup =
				service.createChosenCandidateFormGroup(electionInformation);
		};

		it('should initialize chosenCandidateAnswer form group correctly', () => {
			createChosenCandidateFormGroup();

			expect(chosenCandidateFormGroup).toBeDefined();
			expect(chosenCandidateFormGroup.value).toEqual({
				candidateIdentification: null,
			});
		});

		it('should add candidateListIdentification control when there are lists', () => {
			createChosenCandidateFormGroup({ hasLists: true });

			expect(
				chosenCandidateFormGroup.controls.candidateListIdentification,
			).toBeDefined();
			expect(
				chosenCandidateFormGroup.value.candidateListIdentification,
			).toBeNull();
		});

		it('should add chosenCandidateAnswer form group to the parent FormGroup', () => {
			createChosenCandidateFormGroup();

			expect(parentControl.at(0)).toBe(chosenCandidateFormGroup);
		});
	});

	describe('createChosenWriteInFormGroup', () => {
		let electionInformation: ElectionInformation;
		let candidatePosition: number;
		let writeInPosition: WriteInPosition | undefined;
		let chosenWriteInFormGroup: FormGroupFrom<ChosenWriteIn> | undefined;
		let parentControl: FormArray;

		const createWriteInAnswer = (
			options: object = { writeInsAllowed: true },
		) => {
			electionInformation = MockElectionInformation(options);

			if (electionInformation.writeInPositions.length === 0) {
				return;
			}

			candidatePosition = RandomInt(
				electionInformation.writeInPositions.length,
				1,
			);
			writeInPosition = electionInformation.writeInPositions.find(
				(writeInPosition) => {
					return writeInPosition.position === candidatePosition;
				},
			);

			parentControl = MockFormArray(candidatePosition - 1);
			updateControlContainer(new FormGroup({ chosenWriteIns: parentControl }));

			chosenWriteInFormGroup = service.createChosenWriteInFormGroup(
				electionInformation,
				candidatePosition,
			);
		};

		it('should not initialize writeInAnswer form group when there is no writeInPosition', () => {
			createWriteInAnswer({ writeInsAllowed: false });

			expect(writeInPosition).toBeUndefined();
			expect(chosenWriteInFormGroup).toBeUndefined();
		});

		it('should initialize writeInAnswer form group when there is a writeInPosition', () => {
			createWriteInAnswer();

			expect(writeInPosition).toBeDefined();
			expect(chosenWriteInFormGroup).toBeDefined();
			expect(chosenWriteInFormGroup?.value).toEqual({
				writeInPositionIdentification: writeInPosition?.writeInPositionIdentification,
				writeIn: null,
			});
		});

		it('should add writeInAnswer form group to the parent FormGroup', () => {
			createWriteInAnswer();

			expect(parentControl.at(candidatePosition - 1)).toBe(
				chosenWriteInFormGroup,
			);
		});
	});
});
