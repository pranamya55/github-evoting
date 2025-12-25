/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, inject, Input } from '@angular/core';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import { VoteAnswers, VoteTexts } from 'e-voting-libraries-ui-kit';
import { ChooseFormService } from '../choose-form.service';

@Component({
	selector: 'vp-choose-vote',
	templateUrl: './choose-vote.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseVoteComponent implements AfterContentInit {
	private readonly chooseFormService = inject(ChooseFormService);

	@Input({ required: true }) voteTexts!: VoteTexts;
	voteAnswer!: FormGroupFrom<VoteAnswers>;

	ngAfterContentInit() {
		this.voteAnswer = this.chooseFormService.createVoteAnswersFormGroup(
			this.voteTexts,
		);
	}
}
