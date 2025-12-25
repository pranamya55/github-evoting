/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable } from '@angular/core';
import {
	AuthenticateVoterResponse,
	BackendError,
	OvApi,
	ShortChoiceReturnCode,
	Voter,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import { BackendService } from '@vp/voter-portal-data-access';
import { VoterAnswers } from 'e-voting-libraries-ui-kit';

declare global {
	const OvApi: () => OvApi;

	interface Window {
		OvApi: OvApi;
	}
}

@Injectable({
	providedIn: 'root',
})
export class OvBackendService implements BackendService {
	private get ovApi(): OvApi {
		return window.OvApi;
	}

	async authenticateVoter(
		voter: Voter,
		config: VoterPortalConfig,
	): Promise<AuthenticateVoterResponse> {
		let authenticateVoterResponse: AuthenticateVoterResponse;
		try {
			authenticateVoterResponse = await this.ovApi.authenticateVoter(
				voter.startVotingKey.toLowerCase(),
				voter.extendedFactor,
				config.electionEventId,
				config.identification,
			);
		} catch (error) {
			throw new BackendError(error);
		}

		return authenticateVoterResponse;
	}

	async sendVote(answers: VoterAnswers): Promise<ShortChoiceReturnCode[]> {
		try {
			const { shortChoiceReturnCodes } = await this.ovApi.sendVote(answers);
			return shortChoiceReturnCodes;
		} catch (error) {
			throw new BackendError(error);
		}
	}

	async confirmVote(confirmationKey: string): Promise<string> {
		try {
			const { voteCastReturnCode } =
				await this.ovApi.confirmVote(confirmationKey);
			return voteCastReturnCode;
		} catch (error) {
			throw new BackendError(error);
		}
	}

	async isBrowserCompatible(): Promise<boolean> {
		try {
			return await this.ovApi.isBrowserCompatible();
		} catch (error) {
			throw new BackendError(error);
		}
	}

	async configureVoterPortal(
		electionEventId: string,
	): Promise<VoterPortalConfig> {
		try {
			return await this.ovApi.configureVoterPortal(electionEventId);
		} catch (error) {
			throw new BackendError(error);
		}
	}
}
