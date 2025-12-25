/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, inject, Input } from '@angular/core';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import { ElectionAnswers, ElectionTexts } from 'e-voting-libraries-ui-kit';
import { ChooseFormService } from '../choose-form.service';

@Component({
	selector: 'vp-choose-election-group',
	templateUrl: './choose-election-group.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseElectionGroupComponent implements AfterContentInit {
	@Input({ required: true }) electionTexts!: ElectionTexts;

	electionAnswer!: FormGroupFrom<ElectionAnswers>;

	private readonly chooseFormService = inject(ChooseFormService);

	ngAfterContentInit() {
		this.electionAnswer = this.chooseFormService.createElectionAnswersFormGroup(
			this.electionTexts,
		);
	}
}
