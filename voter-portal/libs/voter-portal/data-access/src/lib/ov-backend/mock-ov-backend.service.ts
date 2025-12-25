/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RandomInt} from '@vp/shared-util-testing';
import {
	AuthenticateVoterResponse,
	BackendError,
	ErrorStatus,
	ShortChoiceReturnCode,
	VerificationCardState,
	Voter,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import {firstValueFrom, of} from 'rxjs';
import {delay, map} from 'rxjs/operators';
import {BackendService} from '@vp/voter-portal-data-access';
import * as mockVoteTexts from './mock-vote-texts.json';
import * as mockElectionTexts from './mock-election-texts.json';
import * as mockVoterPortalConfig from './mock-voter-portal-config.json';
import {ElectionTexts, isVariantBallot, isVoteTexts, Texts, VoterAnswers, VoteTexts,} from 'e-voting-libraries-ui-kit';

function generateId(length: number) {
	return Array.from({length}, () => RandomInt(10)).join('');
}

function throwBackendError(errorProperties: object) {
	const backendError = new BackendError();
	Object.assign(backendError, errorProperties);
	throw backendError;
}

class MockOvBackendService implements BackendService {
	get texts(): Texts {
		return {
			votesTexts: Array.from(mockVoteTexts) as VoteTexts[],
			electionsTexts: Array.from(mockElectionTexts) as ElectionTexts[],
		} as Texts;
	}

	get shortChoiceReturnCodes(): ShortChoiceReturnCode[] {
		const shortChoiceReturnCodes: ShortChoiceReturnCode[] = [];

		const texts: (VoteTexts | ElectionTexts)[] = [
			...(this.texts.votesTexts ?? []),
			...(this.texts.electionsTexts ?? []),
		];
		texts.forEach((text) => {
			if (isVoteTexts(text)) {
				text.ballots.forEach((ballot) => {
					if (isVariantBallot(ballot)) {
						ballot.standardQuestions.forEach((question) =>
							shortChoiceReturnCodes.push({
								questionIdentification: question.questionIdentification,
								shortChoiceReturnCode: generateId(4),
							}),
						);
						ballot.tieBreakQuestions?.forEach((question) =>
							shortChoiceReturnCodes.push({
								questionIdentification: question.questionIdentification,
								shortChoiceReturnCode: generateId(4),
							}),
						);
					} else {
						shortChoiceReturnCodes.push({
							questionIdentification: ballot.questionIdentification,
							shortChoiceReturnCode: generateId(4),
						});
					}
				});
			} else {
				text.electionsInformation.forEach((electionInformation) => {
					if (electionInformation.lists.length > 0) {
						shortChoiceReturnCodes.push({
							electionIdentification:
							electionInformation.election.electionIdentification,
							shortChoiceReturnCode: generateId(4),
						});
					}
					electionInformation.emptyList.emptyPositions.forEach(
						(emptyPosition) =>
							shortChoiceReturnCodes.push({
								electionIdentification:
								electionInformation.election.electionIdentification,
								position: emptyPosition.positionOnList,
								shortChoiceReturnCode: generateId(4),
							}),
					);
				});
			}
		});

		return shortChoiceReturnCodes;
	}

	get writeInAlphabet(): string {
		return "# '(),-./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ¢ŠšŽžŒœŸÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ";
	}

	async authenticateVoter(
		// eslint-disable-next-line @typescript-eslint/no-unused-vars
		voter: Voter,
		config: VoterPortalConfig,
	): Promise<AuthenticateVoterResponse> {
		if (voter.startVotingKey.startsWith('sent')) {
			const verifyResponse$ = of({
				verificationCardState: VerificationCardState.SENT,
				votesTexts: this.texts.votesTexts,
				electionsTexts: this.texts.electionsTexts,
				writeInAlphabet: this.writeInAlphabet,
				shortChoiceReturnCodes: this.shortChoiceReturnCodes,
			}).pipe(delay(500));

			return firstValueFrom(verifyResponse$);
		}

		if (voter.startVotingKey.startsWith('confirmed')) {
			const confirmResponse$ = of({
				verificationCardState: VerificationCardState.CONFIRMED,
				voteCastReturnCode: generateId(8),
			}).pipe(delay(500));

			return firstValueFrom(confirmResponse$);
		}

		const chooseResponse$ = of({
			verificationCardState: VerificationCardState.INITIAL,
			votesTexts: this.texts.votesTexts,
			electionsTexts: this.texts.electionsTexts,
			writeInAlphabet: this.writeInAlphabet,
		}).pipe(
			map((ballot) => {
				if (voter.startVotingKey.startsWith('ea')) {
					throwBackendError({
						numberOfRemainingAttempts: 3,
						errorStatus: ErrorStatus.ExtendedFactorInvalid,
					});
				} else if (voter.startVotingKey.startsWith('max')) {
					throwBackendError({
						errorStatus: ErrorStatus.AuthenticationAttemptsExceeded,
					});
				} else if (voter.startVotingKey.startsWith('svk')) {
					throwBackendError({
						errorStatus: ErrorStatus.StartVotingKeyInvalid,
					});
				}

				return ballot;
			}),
			delay(500),
		);

		return firstValueFrom(chooseResponse$);
	}

	async sendVote(answers: VoterAnswers): Promise<ShortChoiceReturnCode[]> {
		console.log(answers);
		const shortChoiceReturnCodes$ = of(this.shortChoiceReturnCodes).pipe(
			delay(3000),
			map((sendVoteResponse) => {
				return sendVoteResponse;
			}),
		);

		return firstValueFrom(shortChoiceReturnCodes$);
	}

	confirmVote(confirmationKey: string): Promise<string> {
		const confirmationCode$ = of(generateId(8)).pipe(
			delay(500),
			map((voteCastReturnCode) => {
				if (confirmationKey.startsWith('9')) {
					throwBackendError({
						errorStatus: ErrorStatus.ConnectionError,
					});
				} else if (confirmationKey.startsWith('8')) {
					throwBackendError({
						numberOfRemainingAttempts: 3,
						errorStatus: ErrorStatus.ConfirmationKeyInvalid,
					});
				} else if (confirmationKey.startsWith('7')) {
					throwBackendError({
						errorStatus: ErrorStatus.ConfirmationAttemptsExceeded,
					});
				}
				return voteCastReturnCode;
			}),
		);

		return firstValueFrom(confirmationCode$);
	}

	isBrowserCompatible(): Promise<boolean> {
		return Promise.resolve(false);
	}

	configureVoterPortal(electionEventId: string): Promise<VoterPortalConfig> {
		const voterPortalConfig$ = of(
			Object.assign({}, mockVoterPortalConfig as VoterPortalConfig, {
				electionEventId: electionEventId,
			}),
		).pipe(
			delay(1500),
			map((voterPortalConfig) => {
				if (electionEventId.startsWith('9')) {
					throwBackendError({
						errorStatus: ErrorStatus.ConnectionError,
					});
				}
				if (electionEventId.startsWith('8')) {
					throwBackendError({
						errorStatus: ErrorStatus.VotingClientTimeError,
					});
				}
				return voterPortalConfig;
			}),
		);

		return firstValueFrom(voterPortalConfig$);
	}
}

export {MockOvBackendService as OvBackendService};
