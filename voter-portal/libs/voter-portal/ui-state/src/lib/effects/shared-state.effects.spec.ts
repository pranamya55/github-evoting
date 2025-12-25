/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { BackendService } from '@vp/voter-portal-data-access';
import {
	AuthenticateVoterResponse,
	BackendError,
	ExtendedFactor,
	VerificationCardState,
	Voter,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import { cold, hot } from 'jest-marbles';
import { Observable } from 'rxjs';
import { StartVotingActions } from '@vp/voter-portal-ui-state';

import { SharedStateEffects } from './shared-state.effects';
import { MockProvider } from 'ng-mocks';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { RandomElectionEventId } from '@vp/shared-util-testing';

describe('SharedStateEffects', () => {
	let actions$: Observable<Action>;
	let effects: SharedStateEffects;
	let backendService: BackendService;
	let voterPortalConfig: VoterPortalConfig;

	beforeEach(() => {
		voterPortalConfig = {
			identification: ExtendedFactor.YearOfBirth,
			contestsCapabilities: {
				writeIns: true,
			},
			requestTimeout: {
				authenticateVoter: 30000,
				sendVote: 120000,
				confirmVote: 120000,
			},
			header: {
				logo: '',
				logoHeight: { desktop: 0, mobile: 0 },
			},
			electionEventId: RandomElectionEventId(),
			favicon: '',
		};

		TestBed.configureTestingModule({
			providers: [
				SharedStateEffects,
				provideMockActions(() => actions$),
				provideMockStore(),
				BackendService,
				MockProvider(ConfigurationService, voterPortalConfig),
			],
		});

		effects = TestBed.inject(SharedStateEffects);
		backendService = TestBed.inject(BackendService);
	});

	describe('authenticateVoter$', () => {
		let authenticateVoter: jest.Mock;

		beforeEach(() => {
			backendService.authenticateVoter = authenticateVoter = jest.fn();

			// stream to simulate user clicking "Start" after entering authentication data
			actions$ = hot('-a', {
				a: StartVotingActions.startClicked({ voter: {} as Voter }),
			});
		});

		it('should load the texts when the voting card is in its initial state', () => {
			const mockAuthenticateVoterResponse: AuthenticateVoterResponse = {
				verificationCardState: VerificationCardState.INITIAL,
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
			};

			authenticateVoter.mockReturnValueOnce(
				cold('--a|', { a: mockAuthenticateVoterResponse }),
			);

			const expected = hot('---a', {
				a: StartVotingActions.textsLoaded({
					votesTexts: [],
					electionsTexts: [],
					writeInAlphabet: '',
				}),
			});

			expect(effects.authenticateVoter$).toBeObservable(expected);
		});

		it('should load the texts and the short choice return codes when the vote is sent', () => {
			const mockShortChoiceReturnCodes = [
				{
					questionIdentification: 'mockQuestionIdentification',
					shortChoiceReturnCode: 'mockChoiceReturnCode',
				},
			];
			const mockAuthenticateVoterResponse: AuthenticateVoterResponse = {
				verificationCardState: VerificationCardState.SENT,
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
				shortChoiceReturnCodes: mockShortChoiceReturnCodes,
			};

			authenticateVoter.mockReturnValueOnce(
				cold('--a|', { a: mockAuthenticateVoterResponse }),
			);

			const expected = hot('---a', {
				a: StartVotingActions.shortChoiceReturnCodesLoaded({
					votesTexts: [],
					electionsTexts: [],
					writeInAlphabet: '',
					shortChoiceReturnCodes: mockShortChoiceReturnCodes,
				}),
			});

			expect(effects.authenticateVoter$).toBeObservable(expected);
		});

		it('should load the vote cast return code when the vote is confirmed', () => {
			const mockVoteCastReturnCode = 'mockVoteCastReturnCode';
			const mockAuthenticateVoterResponse: AuthenticateVoterResponse = {
				verificationCardState: VerificationCardState.CONFIRMED,
				voteCastReturnCode: mockVoteCastReturnCode,
			};

			authenticateVoter.mockReturnValueOnce(
				cold('--a|', { a: mockAuthenticateVoterResponse }),
			);

			const expected = hot('---a', {
				a: StartVotingActions.voteCastReturnCodeLoaded({
					voteCastReturnCode: mockVoteCastReturnCode,
				}),
			});

			expect(effects.authenticateVoter$).toBeObservable(expected);
		});

		it('should error', () => {
			const mockBackendError = new BackendError();

			authenticateVoter.mockReturnValueOnce(
				cold('--a|', { a: mockBackendError }),
			);

			const expected = hot('---a', {
				a: StartVotingActions.authenticationFailed({
					error: mockBackendError,
				}),
			});

			expect(effects.authenticateVoter$).toBeObservable(expected);
		});
	});
});
