/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AfterViewInit, Component, computed, inject, Signal,} from '@angular/core';
import {Store} from '@ngrx/store';
import {CancelMode, FormGroupFrom} from '@vp/voter-portal-util-types';
import {ChooseActions, getAnswers, getElectionsTexts, getVotesTexts,} from '@vp/voter-portal-ui-state';
import {take} from 'rxjs/operators';
import {ChooseFormService} from '../choose-form.service';
import {ElectionTexts, isElectionTexts, isVoteTexts, VoterAnswers, VoteTexts,} from 'e-voting-libraries-ui-kit';
import {focusFirstInvalidControl} from "@vp/voter-portal-util-helpers";

@Component({
	selector: 'vp-choose',
	templateUrl: './choose.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseComponent implements AfterViewInit {
	private readonly store = inject(Store);
	private readonly chooseFormService = inject(ChooseFormService);
	protected readonly CancelMode = CancelMode;
	protected readonly isVoteTexts = isVoteTexts;
	protected readonly isElectionTexts = isElectionTexts;

	votesTexts: Signal<VoteTexts[]> = this.store.selectSignal(getVotesTexts);
	electionsTexts: Signal<ElectionTexts[]> =
		this.store.selectSignal(getElectionsTexts);
	voterAnswersFormGroup: Signal<FormGroupFrom<VoterAnswers>> = computed(() => {
		return this.chooseFormService.createVoterAnswersFormGroup(
			this.votesTexts(),
			this.electionsTexts(),
		);
	});

	ngAfterViewInit(): void {
		this.store
			.select(getAnswers)
			.pipe(take(1))
			.subscribe((voterAnswers) => {
				if (voterAnswers) this.voterAnswersFormGroup().patchValue(voterAnswers);
			});
	}

	review(): void {
		this.store.dispatch(ChooseActions.formSubmitted());

		if (this.voterAnswersFormGroup().invalid) {
			focusFirstInvalidControl();
			return;
		}

		this.store.dispatch(
			ChooseActions.reviewClicked({
				voterAnswers: this.voterAnswersFormGroup().value as VoterAnswers,
			}),
		);
	}
}
