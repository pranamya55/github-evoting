/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { ConfirmationService } from '@vp/voter-portal-ui-confirmation';
import { SharedActions } from '@vp/voter-portal-ui-state';
import { CancelMode, ConfirmationModalConfig } from '@vp/voter-portal-util-types';
import { filter } from 'rxjs/operators';

export enum CancelState {
	NO_CANCEL_VOTE_OR_LEAVE_PROCESS = 'NO_CANCEL_OR_LEAVE_PROCESS',
	CANCEL_VOTE = 'CANCELLED',
	LEAVE_PROCESS = 'LEFT_PROCESS',
	QUIT = 'QUIT',
}

@Injectable({
	providedIn: 'root',
})
export class ProcessCancellationService {
	private readonly store = inject(Store);
	private readonly confirmationService = inject(ConfirmationService);

	public cancelState = CancelState.NO_CANCEL_VOTE_OR_LEAVE_PROCESS;
	public backButtonPressed = false;

	public confirm(cancelMode: CancelMode): void {
		switch (cancelMode) {
			case CancelMode.CancelVote:
				this.cancelVote();
				break;
			case CancelMode.LeaveProcess:
				this.leaveProcess();
				break;
			case CancelMode.QuitProcess:
				this.quitProcess();
				break;
		}
	}

	// Cancel vote can be used until the user has sent their vote.
	public cancelVote() {
		const processCancellationModalConfig: ConfirmationModalConfig = {
			title: 'cancelvote.title',
			content: 'cancelvote.note',
			confirmIcon: 'box-arrow-in-right',
			confirmLabel: 'cancelvote.yes',
			cancelLabel: 'cancelvote.no',
			modalOptions: { size: 'lg' },
		};

		this.confirmationService
			.confirm(processCancellationModalConfig)
			.pipe(filter((wasCancellationConfirmed) => wasCancellationConfirmed))
			.subscribe(() => {
				this.goToStartVoting(CancelState.CANCEL_VOTE);
			});
	}

	// Leave process vote can be used until the user has confirmed their vote.
	public leaveProcess() {
		const processExitModalConfig: ConfirmationModalConfig = {
			title: 'leaveprocess.title',
			content: 'leaveprocess.note',
			confirmIcon: 'box-arrow-in-right',
			confirmLabel: 'leaveprocess.yes',
			cancelLabel: 'leaveprocess.no',
			modalOptions: { size: 'lg' },
		};

		this.confirmationService
			.confirm(processExitModalConfig)
			.pipe(filter((wasExitConfirmed) => wasExitConfirmed))
			.subscribe(() => {
				this.goToStartVoting(CancelState.LEAVE_PROCESS);
			});
	}

	public quitProcess() {
		const processQuitModalConfig: ConfirmationModalConfig = {
			title: 'quitprocess.title',
			content: 'quitprocess.note',
			confirmIcon: 'box-arrow-in-right',
			confirmLabel: 'quitprocess.yes',
			cancelLabel: 'quitprocess.no',
			modalOptions: { size: 'lg' },
		};

		this.confirmationService
			.confirm(processQuitModalConfig)
			.pipe(filter((wasQuitConfirmed) => wasQuitConfirmed))
			.subscribe(() => {
				this.goToStartVoting(CancelState.QUIT);
			});
	}

	public reset() {
		this.cancelState = CancelState.NO_CANCEL_VOTE_OR_LEAVE_PROCESS;
	}

	private goToStartVoting(cancelState: CancelState) {
		this.cancelState = cancelState;
		this.store.dispatch(SharedActions.loggedOut());
	}
}
