/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, Signal } from '@angular/core';
import {
	getElectionsTexts,
	getLoading,
	getVotesTexts,
	ReviewActions,
} from '@vp/voter-portal-ui-state';
import { Store } from '@ngrx/store';
import {CancelMode, ConfirmationModalConfig} from '@vp/voter-portal-util-types';
import {
	ElectionTexts,
	isElectionTexts,
	isVoteTexts,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import { partition } from 'rxjs';
import { ConfirmationService } from '@vp/voter-portal-ui-confirmation';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SendVoteModalComponent } from '../send-vote-modal/send-vote-modal.component';

@Component({
	selector: 'vp-review',
	templateUrl: './review.component.html',
	standalone: false,
})
export class ReviewComponent {
	private readonly store = inject(Store);
	private readonly modalService = inject(NgbModal);
	private readonly confirmationService = inject(ConfirmationService);

	protected readonly isVoteTexts = isVoteTexts;
	protected readonly isElectionTexts = isElectionTexts;

	votesTexts: Signal<VoteTexts[]> = this.store.selectSignal(getVotesTexts);
	electionsTexts: Signal<ElectionTexts[]> =
		this.store.selectSignal(getElectionsTexts);
	isLoading: Signal<boolean> = this.store.selectSignal(getLoading);

	confirmSeal(): void {
		const sealingModalConfig: ConfirmationModalConfig = {
			title: 'review.confirm.title',
			content: [
				'review.confirm.questionsealvote',
				'review.confirm.hintconfirm',
			],
			confirmIcon: 'lock',
			confirmLabel: 'review.confirm.yes',
			cancelLabel: 'review.confirm.no',
			modalOptions: { size: 'lg' },
		};

		const [sealingConfirmed$, sealingRejected$] = partition(
			this.confirmationService.confirm(sealingModalConfig),
			(wasSealingConfirmed) => wasSealingConfirmed,
		);

		sealingConfirmed$.subscribe(() => this.seal());
		sealingRejected$.subscribe(() =>
			this.store.dispatch(ReviewActions.sealVoteCanceled()),
		);
	}

	private seal(): void {
		this.store.dispatch(ReviewActions.sealVoteClicked());

		this.modalService.open(SendVoteModalComponent, {
			size: 'lg',
			backdrop: 'static',
			keyboard: false,
		});
	}

	protected readonly CancelMode = CancelMode;
}
