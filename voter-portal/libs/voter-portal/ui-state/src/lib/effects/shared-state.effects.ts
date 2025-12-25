/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { concatLatestFrom } from '@ngrx/operators';
import { Store } from '@ngrx/store';
import { BackendService } from '@vp/voter-portal-data-access';
import {
	AuthenticateVoterResponseForCastVote,
	AuthenticateVoterResponseForSentVote,
	AuthenticateVoterResponseForUnsentVote,
	BackendError,
	SharedState,
	VerificationCardState,
} from '@vp/voter-portal-util-types';
import { from, merge, of } from 'rxjs';
import { catchError, exhaustMap, filter, map, share } from 'rxjs/operators';
import {
	InitializationActions,
	LegalTermsActions,
	ReviewActions,
	SharedActions,
	StartVotingActions,
	VerifyActions,
} from '@vp/voter-portal-ui-state';
import { catchConnectionError } from '../operators/catch-connection-error.operator';

import * as SharedOperators from '../operators/shared-state.operators';
import * as SharedSelectors from '../selectors/shared-state.selectors';
import { ConfigurationService } from '@vp/voter-portal-ui-services';

@Injectable()
export class SharedStateEffects {
	private readonly actions$ = inject(Actions);
	private readonly store = inject<Store<SharedState>>(Store);
	private readonly backendService = inject(BackendService);
	private readonly configuration = inject(ConfigurationService);

	configureVoterPortal$ = createEffect(() =>
		this.actions$.pipe(
			ofType(InitializationActions.initializationLoading),
			exhaustMap((action) =>
				from(
					this.backendService.configureVoterPortal(action.electionEventId),
				).pipe(
					map((voterPortalConfig) =>
						InitializationActions.initializationLoaded({
							config: voterPortalConfig,
						}),
					),
					catchConnectionError(30000),
					catchError((error) =>
						of(InitializationActions.initializationFailed({ error })),
					),
				),
			),
		),
	);
	clearBackendError$ = createEffect(() =>
		this.actions$.pipe(
			ofType(LegalTermsActions.agreeClicked),
			map(() => SharedActions.serverErrorCleared()),
		),
	);
	authenticateVoter$ = createEffect(() => {
		const authenticateVoter$ = this.actions$.pipe(
			ofType(StartVotingActions.startClicked),
			concatLatestFrom(() => this.store.select(SharedSelectors.getConfig)),
			exhaustMap(([action, config]) =>
				from(this.backendService.authenticateVoter(action.voter, config)).pipe(
					catchConnectionError(
						this.configuration.requestTimeout.authenticateVoter,
					),
					catchError((error) => of(error)),
				),
			),
			share(),
		);

		const authenticationFailed$ = authenticateVoter$.pipe(
			filter((response) => response instanceof BackendError),
			map((error) => StartVotingActions.authenticationFailed({ error })),
		);

		const textsLoaded$ = authenticateVoter$.pipe(
			filter((response): response is AuthenticateVoterResponseForUnsentVote => {
				return response.verificationCardState === VerificationCardState.INITIAL;
			}),
			map(({ votesTexts, electionsTexts, writeInAlphabet }) => {
				return StartVotingActions.textsLoaded({
					votesTexts: votesTexts,
					electionsTexts: electionsTexts,
					writeInAlphabet,
				});
			}),
		);

		const shortChoiceReturnCodesLoaded$ = authenticateVoter$.pipe(
			filter((response): response is AuthenticateVoterResponseForSentVote => {
				return response.verificationCardState === VerificationCardState.SENT;
			}),
			map(
				({
					shortChoiceReturnCodes,
					votesTexts,
					electionsTexts,
					writeInAlphabet,
				}) => {
					return StartVotingActions.shortChoiceReturnCodesLoaded({
						shortChoiceReturnCodes,
						votesTexts: votesTexts,
						electionsTexts: electionsTexts,
						writeInAlphabet,
					});
				},
			),
		);

		const voteCastReturnCodeLoaded$ = authenticateVoter$.pipe(
			filter((response): response is AuthenticateVoterResponseForCastVote => {
				return (
					response.verificationCardState === VerificationCardState.CONFIRMED
				);
			}),
			map(({ voteCastReturnCode }) => {
				return StartVotingActions.voteCastReturnCodeLoaded({
					voteCastReturnCode,
				});
			}),
		);

		return merge(
			textsLoaded$,
			shortChoiceReturnCodesLoaded$,
			voteCastReturnCodeLoaded$,
			authenticationFailed$,
		);
	});
	sendVote$ = createEffect(() =>
		this.actions$.pipe(
			ofType(ReviewActions.sealVoteClicked),
			concatLatestFrom(() => [
				this.store.pipe(SharedOperators.getDefinedAnswers),
			]),
			exhaustMap(([_action, answers]) =>
				from(this.backendService.sendVote(answers)).pipe(
					map((shortChoiceReturnCodes) =>
						ReviewActions.sealedVoteLoaded({ shortChoiceReturnCodes }),
					),
					catchConnectionError(this.configuration.requestTimeout.sendVote),
					catchError((error) =>
						of(ReviewActions.sealedVoteLoadFailed({ error })),
					),
				),
			),
		),
	);
	confirmVote$ = createEffect(() =>
		this.actions$.pipe(
			ofType(VerifyActions.castVoteClicked),
			exhaustMap((action) =>
				from(this.backendService.confirmVote(action.confirmationKey)).pipe(
					map((voteCastReturnCode) =>
						VerifyActions.castVoteLoaded({ voteCastReturnCode }),
					),
					catchConnectionError(this.configuration.requestTimeout.confirmVote),
					catchError((error) =>
						of(VerifyActions.castVoteLoadFailed({ error })),
					),
				),
			),
		),
	);
}
