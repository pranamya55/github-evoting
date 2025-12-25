/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {SessionService} from "../session.service";
import {VotingServerService} from "./voting-server.service";
import {createConfirmMessage} from "../protocol/voting-phase/confirm-vote/create-confirm-message.algorithm";
import {VoterPortalInputError} from "../domain/voter-portal-input-error";
import {AuthenticationChallengeOutput} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.types";
import {getAuthenticationChallenge} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.algorithm";
import {validateBallotCastingKey} from "../domain/validations/validations";
import {serializeGqGroup, serializeGroupElement, serializeImmutableBigInteger} from "../domain/primitives-serializer";
import {ConfirmVoteRequestPayload, ConfirmVoteResponse, ConfirmVoteResponsePayload} from "../domain/confirm-vote.types";
import {VoterAuthenticationData} from "../domain/authenticate-voter.types";
import {PrimitivesParams} from "../domain/primitives-params.types";
import {validateConfirmVoteResponsePayload} from "../domain/validations/confirm-vote-validation";

export class ConfirmVoteProcess {

	private static readonly CONFIRMATION_KEY_INCORRECT_ERROR_CODE: string = "CONFIRMATION_KEY_INCORRECT";

	private readonly sessionService: SessionService;
	private readonly votingServerService: VotingServerService;

	constructor() {
		this.sessionService = SessionService.getInstance();
		this.votingServerService = new VotingServerService();
	}

	/**
	 * This process implements the confirmVote phase of the protocol:
	 *  - Receives the BallotCastingKey from the Voter Portal.
	 *  - Calls the CreateConfirmMessage algorithm.
	 *  - Calls the GetAuthenticationChallenge algorithm.
	 *  - Sends a ConfirmVoteRequestPayload to the Voting Server.
	 *  - Returns a ConfirmVoteResponse to the Voter Portal.
	 *
	 * @param {string} ballotCastingKey - the voter ballot casting key.
	 *
	 * @returns {Promise<ConfirmVoteResponse>} - the confirmVote response expected by the Voter-Portal.
	 */
	public async confirmVote(ballotCastingKey: string): Promise<ConfirmVoteResponse> {

		this.validateBallotCastingKeyInput(ballotCastingKey);

		const primitivesParams = this.sessionService.primitivesParams();
		const voterAuthenticationData = this.sessionService.voterAuthenticationData();

		// Call CreateConfirmMessage algorithm
		const confirmationKey: GqElement = createConfirmMessage(
			{
				encryptionGroup: primitivesParams.encryptionGroup
			},
			ballotCastingKey,
			this.sessionService.verificationCardSecretKey()
		);

		const extendedAuthenticationFactor: string = this.sessionService.extendedAuthenticationFactor();

		// Call GetAuthenticationChallenge algorithm
		const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
			{
				electionEventId: voterAuthenticationData.electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			"confirmVote",
			this.sessionService.startVotingKey(),
			extendedAuthenticationFactor
		);

		// Prepare and post payload to the Voting Server
		const confirmVoteRequestPayload: ConfirmVoteRequestPayload = this.prepareConfirmVoteRequestPayload(voterAuthenticationData, authenticationChallengeOutput, confirmationKey, primitivesParams);
		const confirmVoteResponsePayload: ConfirmVoteResponsePayload = await this.votingServerService.confirmVote(confirmVoteRequestPayload);

		validateConfirmVoteResponsePayload(confirmVoteResponsePayload);

		return {
			voteCastReturnCode: confirmVoteResponsePayload.shortVoteCastReturnCode
		};
	}

	/**
	 * Throws a Voter Portal input error if the ballot casting key is not valid.
	 * @param {string} ballotCastingKey - the voter ballot casting key.
	 */
	private validateBallotCastingKeyInput(ballotCastingKey: string) {
		try {
			validateBallotCastingKey(ballotCastingKey);
		} catch (_error) {
			throw new VoterPortalInputError(ConfirmVoteProcess.CONFIRMATION_KEY_INCORRECT_ERROR_CODE, _error.message);
		}
	}

	/**
	 * Prepares the confirmVote request payload expected by the Voting-Server.
	 *
	 * @param {VoterAuthenticationData} voterAuthenticationData - the voter authentication data.
	 * @param {AuthenticationChallengeOutput} authenticationChallengeOutput - the output of the authentication challenge algorithm.
	 * @param {GqElement} confirmationKey - the confirmation key.
	 * @param {PrimitivesParams} primitivesParams - the primitives parameters.
	 *
	 * @returns {ConfirmVoteRequestPayload} - the confirmVote request payload expected by the Voting-Server.
	 */
	private prepareConfirmVoteRequestPayload(voterAuthenticationData: VoterAuthenticationData, authenticationChallengeOutput: AuthenticationChallengeOutput, confirmationKey: GqElement, primitivesParams: PrimitivesParams): ConfirmVoteRequestPayload {
		return {
			contextIds: {
				electionEventId: voterAuthenticationData.electionEventId,
				verificationCardSetId: voterAuthenticationData.verificationCardSetId,
				verificationCardId: voterAuthenticationData.verificationCardId
			},
			authenticationChallenge: {
				derivedVoterIdentifier: authenticationChallengeOutput.derivedVoterIdentifier,
				derivedAuthenticationChallenge: authenticationChallengeOutput.derivedAuthenticationChallenge,
				authenticationNonce: serializeImmutableBigInteger(authenticationChallengeOutput.authenticationNonce)
			},
			confirmationKey: serializeGroupElement(confirmationKey),
			encryptionGroup: JSON.parse(serializeGqGroup(primitivesParams.encryptionGroup))
		};
	}

}
