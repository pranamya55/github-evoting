/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, inject, Input } from '@angular/core';
import { AnswerBase, QuestionBase } from 'e-voting-libraries-ui-kit';
import { isEmptyAnswer } from '@vp/voter-portal-util-helpers';
import { Store } from '@ngrx/store';
import { getChosenAnswer, getVoteShortChoiceReturnCode } from '@vp/voter-portal-ui-state';

@Component({
	selector: 'vp-verify-vote-question',
	templateUrl: './verify-vote-question.component.html',
	standalone: false,
})
export class VerifyVoteQuestionComponent<
	TQuestion extends QuestionBase<TAnswer>,
	TAnswer extends AnswerBase,
> implements AfterContentInit
{
	@Input({ required: true }) voteIdentification!: string;
	@Input({ required: true }) ballotIdentification!: string;
	@Input({ required: true }) question!: TQuestion;

	chosenAnswer?: TAnswer;
	isEmptyAnswer?: boolean;

	shortChoiceReturnCode?: string;

	private readonly store = inject(Store);

	ngAfterContentInit(): void {
		const chosenAnswerIdentification: string =
			this.store.selectSignal(
				getChosenAnswer({
					voteIdentification: this.voteIdentification,
					ballotIdentification: this.ballotIdentification,
					questionIdentification: this.question.questionIdentification,
				}),
			)()?.answerIdentification ?? '';

		this.chosenAnswer = this.question.answers.find(
			(answer) => answer.answerIdentification === chosenAnswerIdentification,
		);

		if (this.chosenAnswer)
			this.isEmptyAnswer = isEmptyAnswer(this.chosenAnswer);

		this.shortChoiceReturnCode = this.store.selectSignal(
			getVoteShortChoiceReturnCode(this.question.questionIdentification),
		)()?.shortChoiceReturnCode;
	}
}
