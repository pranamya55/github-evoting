/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { tap } from 'rxjs/operators';
import {
	ChooseActions,
	ConfirmActions,
	InitializationActions,
	LegalTermsActions,
	ReviewActions,
	SharedActions,
	StartVotingActions,
	VerifyActions,
} from '@vp/voter-portal-ui-state';

@Injectable()
export class NavigationEffects {
	private readonly actions$ = inject(Actions);
	private readonly router = inject(Router);

	navigateToError$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(InitializationActions.initializationFailed),
				tap(() => {
					this.navigate(['']);
				}),
			),
		{ dispatch: false },
	);

	navigateToLegalTerms$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(InitializationActions.initializationLoaded),
				tap(() => {
					this.navigate(['/legal-terms']);
				}),
			),
		{ dispatch: false },
	);

	navigateToStartVoting$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(SharedActions.loggedOut, LegalTermsActions.agreeClicked),
				tap(() => {
					this.navigate(['/start-voting']);
				}),
			),
		{ dispatch: false },
	);

	navigateToChoose$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(StartVotingActions.textsLoaded, ReviewActions.sealVoteCanceled),
				tap(() => {
					this.navigate(['/choose']);
				}),
			),
		{ dispatch: false },
	);

	navigateToReview$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(ChooseActions.reviewClicked),
				tap(() => {
					this.navigate(['/review']);
				}),
			),
		{ dispatch: false },
	);

	navigateToVerify$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(
					ReviewActions.sealedVoteLoaded,
					StartVotingActions.shortChoiceReturnCodesLoaded,
				),
				tap(() => {
					this.navigate(['/verify']);
				}),
			),
		{ dispatch: false },
	);

	navigateToConfirm$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(
					VerifyActions.castVoteLoaded,
				),
				tap(() => {
					this.navigate(['/confirm']);
				}),
			),
		{ dispatch: false },
	);

	navigateToFinalization$ = createEffect(
		() =>
			this.actions$.pipe(
				ofType(
					ConfirmActions.endClicked,
					StartVotingActions.voteCastReturnCodeLoaded,
				),
				tap(() => {
					this.navigate(['/end']);
				}),
			),
		{ dispatch: false },
	);

	private navigate(commands: readonly any[]) {
		this.router.navigate(commands, { info: { trusted: true } });
	}
}
