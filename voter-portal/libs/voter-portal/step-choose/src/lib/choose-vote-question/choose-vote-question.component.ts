/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, inject, Input } from '@angular/core';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import {
	AnswerBase,
	Ballot,
	ChosenAnswer,
	QuestionBase,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import { isEmptyAnswer } from '@vp/voter-portal-util-helpers';
import { ChooseFormService } from '../choose-form.service';

@Component({
	selector: 'vp-choose-vote-question',
	templateUrl: './choose-vote-question.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseVoteQuestionComponent<
	TQuestion extends QuestionBase<TAnswer>,
	TAnswer extends AnswerBase,
> implements AfterContentInit
{
	@Input({ required: true })
	voteIdentification!: VoteTexts['voteIdentification'];
	@Input({ required: true })
	ballotIdentification!: Ballot['ballotIdentification'];
	@Input({ required: true }) question!: TQuestion;

	chosenAnswerFormGroup!: FormGroupFrom<ChosenAnswer>;
	nonEmptyAnswers!: TAnswer[];
	emptyAnswer?: TAnswer;

	private readonly chooseFormService = inject(ChooseFormService);

	get chosenAnswerIdentification(): string | undefined {
		return this.chosenAnswerFormGroup?.controls.answerIdentification.value;
	}

	ngAfterContentInit(): void {
		this.emptyAnswer = this.question.answers.find((answer) =>
			isEmptyAnswer(answer),
		);
		this.nonEmptyAnswers = this.question.answers.filter(
			(answer) => !isEmptyAnswer(answer),
		);

		this.chosenAnswerFormGroup =
			this.chooseFormService.createChosenAnswerFormGroup(
				this.ballotIdentification,
				this.question,
				this.emptyAnswer,
			);
	}

	resetRadioButtons(): void {
		this.chosenAnswerFormGroup?.reset();

		const question = document.querySelector<HTMLInputElement>(
			`#answer-${this.chosenAnswerIdentification}`,
		);
		if (question) question.focus();
	}
}
