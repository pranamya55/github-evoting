/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {validateUUID} from "../domain/validations/validations";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {BASE16_ALPHABET} from "crypto-primitives-ts/lib/esm/math/base16-alphabet";
import {VotingServerResponseError} from "../domain/voting-server-response-error";
import {VotingServerConnectionError} from "../domain/voting-server-connection-error";
import {SendVoteRequestPayload, SendVoteResponsePayload} from "../domain/send-vote.types";
import {ConfirmVoteRequestPayload, ConfirmVoteResponsePayload} from "../domain/confirm-vote.types";
import {AuthenticateVoterRequestPayload, AuthenticateVoterResponsePayload} from "../domain/authenticate-voter.types";
import {ConfigureVoterPortalResponsePayload} from "../domain/configure-voter-portal.types";

export class VotingServerService {
	private static readonly host: string = "/vs-ws-rest/api/v1/processor/voting";

	/**
	 * Authenticates the voter to the Voting-Server.
	 *
	 * @param {AuthenticateVoterRequestPayload} authenticateVoterRequestPayload - the authenticateVoter request payload.
	 * @returns {Promise<AuthenticateVoterResponsePayload>} - the authenticateVoter response payload produced by the Voting-Server.
	 */
	public async authenticateVoter(authenticateVoterRequestPayload: AuthenticateVoterRequestPayload): Promise<AuthenticateVoterResponsePayload> {
		checkNotNull(authenticateVoterRequestPayload);

		const authenticateVoterEndpoint: string = this.getAuthenticateVoterEndpoint(
			authenticateVoterRequestPayload.electionEventId,
			authenticateVoterRequestPayload.authenticationChallenge.derivedVoterIdentifier
		);

		return this.postRequest(authenticateVoterEndpoint, authenticateVoterRequestPayload);
	}

	/**
	 * Sends the vote to the Voting-Server.
	 *
	 * @param {SendVoteRequestPayload} sendVoteRequestPayload - the sendVote request payload.
	 *
	 * @returns {Promise<SendVoteResponsePayload>} - the sendVote response payload produced by the Voting-Server.
	 */
	public async sendVote(sendVoteRequestPayload: SendVoteRequestPayload): Promise<SendVoteResponsePayload> {
		checkNotNull(sendVoteRequestPayload);

		// Prepare endpoint
		const sendVoteEndpoint: string = this.getSendVoteEndpoint(
			sendVoteRequestPayload.contextIds.electionEventId,
			sendVoteRequestPayload.contextIds.verificationCardSetId,
			sendVoteRequestPayload.authenticationChallenge.derivedVoterIdentifier,
			sendVoteRequestPayload.contextIds.verificationCardId
		);

		return this.postRequest(sendVoteEndpoint, sendVoteRequestPayload);
	}

	/**
	 * Confirms the vote to the Voting-Server.
	 *
	 * @param {ConfirmVoteRequestPayload} confirmVoteRequestPayload - the confirmVote request payload.
	 *
	 * @returns {Promise<ConfirmVoteResponsePayload>}- the confirmVote response payload produced by the Voting-Server.
	 */
	public async confirmVote(confirmVoteRequestPayload: ConfirmVoteRequestPayload): Promise<ConfirmVoteResponsePayload> {
		checkNotNull(confirmVoteRequestPayload);

		// Prepare endpoint
		const confirmVoteEndpoint: string = this.getConfirmVoteEndpoint(
			confirmVoteRequestPayload.contextIds.electionEventId,
			confirmVoteRequestPayload.contextIds.verificationCardSetId,
			confirmVoteRequestPayload.authenticationChallenge.derivedVoterIdentifier,
			confirmVoteRequestPayload.contextIds.verificationCardId
		);

		return this.postRequest(confirmVoteEndpoint, confirmVoteRequestPayload);
	}

	/**
	 * Retrieves the Voter Portal Config from the Voting-Server.
	 *
	 * @param {string} electionEventId - the election event id.
	 *
	 * @returns {Promise<ConfigureVoterPortalResponsePayload>}- the Voter Portal Config response payload produced by the Voting-Server.
	 */
	public async configureVoterPortal(electionEventId: string): Promise<ConfigureVoterPortalResponsePayload> {
		validateUUID(electionEventId);

		// Prepare endpoint
		const configureVoterPortalEndpoint: string = this.getConfigureVoterPortalEndpoint(electionEventId);

		return this.getRequest(configureVoterPortalEndpoint);
	}

	/**
	 * Provides the 'authenticateVoter' rest endpoint.
	 * @param {string} electionEventId - the election event id.
	 * @param {string} credentialId - the credential id.
	 * @returns {string} - the 'authenticateVoter' endpoint.
	 */
	private getAuthenticateVoterEndpoint(electionEventId: string, credentialId: string): string {
		const endpoint: string = `authenticatevoter/electionevent/${electionEventId}/credentialId/${credentialId}/authenticate`;
		return `${VotingServerService.host}/${endpoint}`;
	}

	/**
	 * Provides the 'sendVote' rest endpoint.
	 * @param {string} electionEventId - the election event id.
	 * @param {string} verificationCardSetId - the verification card set id.
	 * @param {string} credentialId - the credential id.
	 * @param {string} verificationCardId - the verification card id.
	 * @returns {string} - the 'sendVote' endpoint.
	 */
	private getSendVoteEndpoint(electionEventId: string, verificationCardSetId: string, credentialId: string, verificationCardId: string): string {
		const endpoint: string = `sendvote/electionevent/${electionEventId}/verificationcardset/${verificationCardSetId}/credentialId/${credentialId}/verificationcard/${verificationCardId}`;
		return `${VotingServerService.host}/${endpoint}`;
	}

	/**
	 * Provides the 'confirmVote' rest endpoint.
	 * @param {string} electionEventId - the election event id.
	 * @param {string} verificationCardSetId - the verification card set id.
	 * @param {string} credentialId - the credential id.
	 * @param {string} verificationCardId - the verification card id.
	 * @returns {string} - the 'confirmVote' endpoint.
	 */
	private getConfirmVoteEndpoint(electionEventId: string, verificationCardSetId: string, credentialId: string, verificationCardId: string): string {
		const endpoint: string = `confirmvote/electionevent/${electionEventId}/verificationcardset/${verificationCardSetId}/credentialId/${credentialId}/verificationcard/${verificationCardId}`;
		return `${VotingServerService.host}/${endpoint}`;
	}

	/**
	 * Provides the 'configureVoterPortal' rest endpoint.
	 * @param {string} electionEventId - the election event id.
	 * @returns {string} - the 'configureVoterPortal' endpoint.
	 */
	private getConfigureVoterPortalEndpoint(electionEventId: string): string {
		const endpoint: string = `configurevoterportal/electionevent/${electionEventId}`;
		return `${VotingServerService.host}/${endpoint}`;
	}

	/**
	 * Post a request payload to the specified endpoint.
	 * @param {string} endpoint - the voting server endpoint.
	 * @param {AuthenticateVoterRequestPayload | SendVoteRequestPayload | ConfirmVoteRequestPayload} requestPayload - the request payload.
	 * @returns {Promise<any>} - the response payload.
	 */
	private async postRequest(endpoint: string, requestPayload: AuthenticateVoterRequestPayload | SendVoteRequestPayload | ConfirmVoteRequestPayload): Promise<any> {

		// Generate an idempotency key.
		const randomService: RandomService = new RandomService()
		const idempotencyKey: string = randomService.genRandomString(32, BASE16_ALPHABET);

		let response: Response;
		try {
			response = await fetch(endpoint,
				{
					method: "POST",
					headers: {
						"Accept": "application/json",
						"Content-Type": "application/json;charset=UTF-8",
						"Idempotency-Key": idempotencyKey // Unique key for each request.
					},
					cache: "no-store",  // Prevent browser from caching or prefetching the request.
					body: JSON.stringify(requestPayload)
				}
			);
		} catch (error){
			throw new VotingServerConnectionError(error);
		}

		// Check if the response contains an error.
		if (!response.ok) {
			let errorJson: {};
			try {
				errorJson = await response.json();
			} catch {
				// The parsing as JSON of the response failed because is empty or HTML.
				// Provide an empty error json.
				errorJson = {};
			}
			throw new VotingServerResponseError(response.status, errorJson);
		}

		return await this.handleResponse(response);
	}

	/**
	 * Get a payload from the specified endpoint.
	 * @param {string} endpoint - the voting server endpoint.
	 * @returns {Promise<any>} - the response payload.
	 */
	private async getRequest(endpoint: string): Promise<any> {

		// Generate an idempotency key.
		const randomService: RandomService = new RandomService()
		const idempotencyKey: string = randomService.genRandomString(32, BASE16_ALPHABET);

		let response: Response;
		try {
			response = await fetch(endpoint,
				{
					method: "GET",
					headers: {
						"Accept": "application/json",
						"Content-Type": "application/json;charset=UTF-8",
						"Idempotency-Key": idempotencyKey // Unique key for each request.
					},
					cache: "no-store",  // Prevent browser from caching or prefetching the request.
				}
			);
		} catch (error){
			throw new VotingServerConnectionError(error);
		}

		return await this.handleResponse(response);
	}

	private async handleResponse(response: Response): Promise<any> {
		// Check if the response contains an error.
		if (!response.ok) {
			let errorJson: {};
			try {
				errorJson = await response.json();
			} catch {
				// The parsing as JSON of the response failed because is empty or HTML.
				// Provide an empty error json.
				errorJson = {};
			}
			throw new VotingServerResponseError(response.status, errorJson);
		}

		return response.json();
	}
}