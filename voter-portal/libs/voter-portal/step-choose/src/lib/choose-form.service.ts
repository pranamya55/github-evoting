/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject, Injectable } from '@angular/core';
import {
	ControlContainer,
	FormArray,
	FormBuilder,
	FormGroup,
} from '@angular/forms';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import {
	AnswerBase,
	ChosenAnswer,
	ChosenCandidate,
	ChosenList,
	ChosenWriteIn,
	ElectionAnswers,
	ElectionInformation,
	ElectionInformationAnswers,
	ElectionTexts,
	EmptyPositionId,
	QuestionBase,
	VoteAnswers,
	VoterAnswers,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';

@Injectable()
export class ChooseFormService {
	private readonly fb = inject(FormBuilder);
	private readonly controlContainer = inject(ControlContainer, {
		optional: true,
	});

	createVoterAnswersFormGroup(
		votesTexts: VoteTexts[],
		electionsTexts: ElectionTexts[],
	): FormGroupFrom<VoterAnswers> {
		const voterAnswersFormGroup: FormGroupFrom<VoterAnswers> =
			this.fb.nonNullable.group({});

		if (votesTexts.length) {
			voterAnswersFormGroup.setControl(
				'voteAnswers',
				this.fb.nonNullable.array<FormGroupFrom<VoteAnswers>>([]),
			);
		}

		if (electionsTexts.length) {
			voterAnswersFormGroup.setControl(
				'electionAnswers',
				this.fb.nonNullable.array<FormGroupFrom<ElectionAnswers>>([]),
			);
		}

		return voterAnswersFormGroup;
	}

	createVoteAnswersFormGroup(voteTexts: VoteTexts): FormGroupFrom<VoteAnswers> {
		const voteAnswerFormGroup = this.fb.nonNullable.group({
			voteIdentification: voteTexts.voteIdentification,
			votePosition: voteTexts.votePosition,
			chosenAnswers: this.fb.array<FormGroupFrom<ChosenAnswer>>([]),
		});

		const voteAnswersFormArray = this.controlContainer?.control;
		if (voteAnswersFormArray instanceof FormArray) {
			voteAnswersFormArray.push(voteAnswerFormGroup);
		}

		return voteAnswerFormGroup;
	}

	createChosenAnswerFormGroup<
		TQuestion extends QuestionBase<TAnswer>,
		TAnswer extends AnswerBase,
	>(
		ballotIdentification: string,
		question: TQuestion,
		emptyAnswer?: TAnswer,
	): FormGroupFrom<ChosenAnswer> {
		const chosenAnswerFormGroup = this.fb.nonNullable.group<ChosenAnswer>({
			ballotIdentification: ballotIdentification,
			questionIdentification: question.questionIdentification,
			answerIdentification: emptyAnswer?.answerIdentification ?? '',
		});

		const chosenAnswersFormArray = this.controlContainer?.control;
		if (chosenAnswersFormArray instanceof FormArray) {
			chosenAnswersFormArray.push(chosenAnswerFormGroup);
		}

		return chosenAnswerFormGroup;
	}

	createElectionAnswersFormGroup(
		electionTexts: ElectionTexts,
	): FormGroupFrom<ElectionAnswers> {
		const electionAnswerFormGroup = this.fb.nonNullable.group({
			electionGroupIdentification: electionTexts.electionGroupIdentification,
			electionGroupPosition: electionTexts.electionGroupPosition,
			electionsInformation: this.fb.array<
				FormGroupFrom<ElectionInformationAnswers>
			>([]),
		});

		const electionAnswersFormArray = this.controlContainer?.control;
		if (electionAnswersFormArray instanceof FormArray) {
			electionAnswersFormArray.push(electionAnswerFormGroup);
		}

		return electionAnswerFormGroup;
	}

	createElectionInformationAnswersFormGroup(
		electionInformation: ElectionInformation,
	): FormGroupFrom<ElectionInformationAnswers> {
		const emptyPositionIdFormGroups =
			electionInformation.emptyList.emptyPositions.map((emptyPosition) => {
				return this.fb.nonNullable.group({
					emptyPositionIdentification: emptyPosition.emptyPositionIdentification,
				});
			});

		const electionInformationAnswerFormGroup: FormGroupFrom<ElectionInformationAnswers> =
			this.fb.nonNullable.group({
				electionIdentification: electionInformation.election.electionIdentification,
				chosenCandidates: this.fb.array<FormGroupFrom<ChosenCandidate>>([]),
				emptyListIds: this.fb.nonNullable.group({
					listIdentification: electionInformation.emptyList.listIdentification,
					emptyPositionIds: this.fb.array<FormGroupFrom<EmptyPositionId>>(
						emptyPositionIdFormGroups,
					),
				}),
			});

		if (electionInformation.election.writeInsAllowed) {
			electionInformationAnswerFormGroup.addControl(
				'chosenWriteIns',
				this.fb.array<FormGroupFrom<ChosenWriteIn>>([]),
			);
		}

		const electionsInformationFormArray = this.controlContainer?.control;
		if (electionsInformationFormArray instanceof FormArray) {
			electionsInformationFormArray.push(electionInformationAnswerFormGroup);
		}

		return electionInformationAnswerFormGroup;
	}

	createChosenListFormGroup(
		electionInformation: ElectionInformation,
	): FormGroupFrom<ChosenList> {
		const chosenListFormGroup = this.fb.nonNullable.group<ChosenList>({
			listIdentification: electionInformation.emptyList.listIdentification,
		});

		const electionsInformationFormGroup = this.controlContainer?.control;
		if (electionsInformationFormGroup instanceof FormGroup) {
			electionsInformationFormGroup.addControl(
				'chosenList',
				chosenListFormGroup,
			);
		}

		return chosenListFormGroup;
	}

	createChosenCandidateFormGroup(
		electionInformation: ElectionInformation,
	): FormGroupFrom<ChosenCandidate> {
		const chosenCandidateFormGroup = this.fb.nonNullable.group<ChosenCandidate>(
			{
				candidateIdentification: null,
			},
		);

		if (electionInformation.lists.length > 0) {
			chosenCandidateFormGroup.addControl(
				'candidateListIdentification',
				this.fb.nonNullable.control(null),
			);
		}

		const chosenCandidatesFormArray =
			this.controlContainer?.control?.get('chosenCandidates');
		if (chosenCandidatesFormArray instanceof FormArray) {
			chosenCandidatesFormArray.push(chosenCandidateFormGroup);
		}

		return chosenCandidateFormGroup;
	}

	createChosenWriteInFormGroup(
		electionInformation: ElectionInformation,
		candidatePosition: number,
	): FormGroupFrom<ChosenWriteIn> | undefined {
		const writeInPosition = electionInformation.writeInPositions.find(
			(writeInPosition) => {
				return writeInPosition.position === candidatePosition;
			},
		);

		if (!writeInPosition) return;

		const chosenWriteInFormGroup = this.fb.nonNullable.group<ChosenWriteIn>({
			writeInPositionIdentification: writeInPosition.writeInPositionIdentification,
			writeIn: null,
		});

		const chosenWriteInFormArray =
			this.controlContainer?.control?.get('chosenWriteIns');
		if (chosenWriteInFormArray instanceof FormArray) {
			chosenWriteInFormArray.push(chosenWriteInFormGroup);
		}

		return chosenWriteInFormGroup;
	}
}
