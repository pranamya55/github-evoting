/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {WorkerWrapper} from "./worker-wrapper";
import {SendVoteResponse} from "./domain/send-vote.types";
import {ConfirmVoteResponse} from "./domain/confirm-vote.types";
import {AuthenticateVoterResponse} from "./domain/authenticate-voter.types";
import {VoterAnswers} from "e-voting-libraries-ui-kit";

/**
 * Provides an API to the Voter-Portal to communicate with the Voting-Server for all the voting phases described in the protocol.
 */

// Expose the OvApi object on the global window object
export class OnlineVotingApi {
	private static readonly workerWrapper: WorkerWrapper = WorkerWrapper.getInstance();

	/**
	 * Authenticates the voter to the Voting-Server.
	 *
	 * @param {string} startVotingKey - the start voting key of a voter.
	 * @param {string} extendedAuthenticationFactor - the extended authentication factor, such as a year of birth OR date of birth.
	 * @param {string} electionEventId - the related election event identifier.
	 * @param {string} identification - the identification type received from the Voter-Portal.
	 * @returns {Promise<AuthenticateVoterResponse>} the authenticateVoter response.
	 */
	static authenticateVoter(startVotingKey: string, extendedAuthenticationFactor: string, electionEventId: string, identification: string): Promise<AuthenticateVoterResponse> {
		// @ts-ignore
		return this.workerWrapper.invoke("authenticateVoter", startVotingKey, extendedAuthenticationFactor, electionEventId, identification);
	}

	/**
	 * Sends the vote to the Voting-Server.
	 *
	 * @param {VoterAnswers} voterAnswers - the voter's answers.
	 * @returns {Promise<SendVoteResponse>} - the sendVote response.
	 */
	static sendVote(voterAnswers: VoterAnswers): Promise<SendVoteResponse> {
		// @ts-ignore
		return this.workerWrapper.invoke("sendVote", voterAnswers);
	}

	/**
	 * Confirms the vote to the Voting-Server.
	 *
	 * @param {string} ballotCastingKey - the 'ballot casting key'.
	 * @returns {Promise<ConfirmVoteResponse>} - the confirmVote response.
	 */
	static confirmVote(ballotCastingKey: string): Promise<ConfirmVoteResponse> {
		// @ts-ignore
		return this.workerWrapper.invoke("confirmVote", ballotCastingKey);
	}

	/**
	 * Checks the browser compatibility	with the voting system.
	 *
	 * @returns {Promise<boolean>} - the compatibility flag.
	 */
	static isBrowserCompatible(): Promise<boolean> {
		// @ts-ignore
		return this.workerWrapper.invoke("isBrowserCompatible");
	}

	/**
	 * Gets the Voter Portal configuration.
	 *
	 * @returns {Promise<{}>} - the configuration.
	 */
	static configureVoterPortal(electionEventId: string): Promise<{}> {
		// @ts-ignore
		return this.workerWrapper.invoke("configureVoterPortal", electionEventId);
	}
}

(window as any).OvApi = OnlineVotingApi;
