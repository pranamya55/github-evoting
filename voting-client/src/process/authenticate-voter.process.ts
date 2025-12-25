/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {getKey} from "../protocol/voting-phase/authenticate-voter/get-key.algorithm";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {SessionService} from "../session.service";
import {PrimitivesParams} from "../domain/primitives-params.types";
import {VotingServerService} from "./voting-server.service";
import {parsePrimitivesParams} from "../domain/primitives-params-parser";
import {VoterPortalInputError} from "../domain/voter-portal-input-error";
import {AuthenticationChallengeOutput} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.types";
import {getAuthenticationChallenge} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.algorithm";
import {
	AuthenticateVoterRequestPayload,
	AuthenticateVoterResponse,
	AuthenticateVoterResponsePayload,
	VerificationCardState
} from "../domain/authenticate-voter.types";
import {validateSVK} from "../domain/validations/start-voting-key-validation";
import {validateIdentificationAndConvert} from "../domain/validations/extended-authentication-factor-validation";
import {validateUUID} from "../domain/validations/validations";
import {LATIN_ALPHABET} from "../domain/latin-alphabet";
import {validateVoteTexts} from "../domain/validations/vote-texts-validation";
import {validateElectionTexts} from "../domain/validations/election-texts-validation";
import {getShortChoiceReturnCodes} from "../domain/short-choice-return-code-builder";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {validateAuthenticateVoterResponsePayload} from "../domain/validations/authenticate-voter-response-validation";
import {serializeImmutableBigInteger} from "../domain/primitives-serializer";

export class AuthenticateVoterProcess {

	private static readonly START_VOTING_KEY_INVALID_ERROR_CODE: string = "START_VOTING_KEY_INVALID";

	private readonly sessionService: SessionService;
	private readonly votingServerService: VotingServerService;

	constructor() {
		this.sessionService = SessionService.getInstance();
		this.votingServerService = new VotingServerService();
	}

	/**
	 * This process implements the authenticateVoter phase of the protocol:
	 *  - Receives the StartVotingKey and ExtendedAuthenticationFactor from the Voter Portal.
	 *  - Calls the GetAuthenticationChallenge algorithm.
	 *  - Sends an AuthenticationChallenge to the Voting Server.
	 *  - Parses the AuthenticateVoterResponsePayload received from the Voting Server.
	 *  - Calls the GetKey algorithm if the verification card state is "INITIAL" or "SENT".
	 *  - Returns an AuthenticateVoterResponse to the Voter Portal.
	 *
	 * @param {string} startVotingKey - the voter start voting key.
	 * @param {string} extendedAuthenticationFactor - the voter extended authentication factor (i.e. birthdate).
	 * @param {string} electionEventId - the election event id.
	 * @param {string} identification - the identification type received from the Voter-Portal.
	 * @returns {Promise<AuthenticateVoterResponse>}, the authenticateVoter response expected by the Voter-Portal.
	 */
	public async authenticateVoter(
		startVotingKey: string,
		extendedAuthenticationFactor: string,
		electionEventId: string,
		identification: string
	): Promise<AuthenticateVoterResponse> {

		this.validateStartVotingKeyInput(startVotingKey);
		const extendedAuthenticationFactorLength = validateIdentificationAndConvert(identification);

		// Call GetAuthenticationChallenge algorithm.
		const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
			{
				electionEventId: electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactorLength
			},
			"authenticateVoter",
			startVotingKey,
			extendedAuthenticationFactor
		);

		// Store authentication challenge inputs in session.
		this.sessionService.startVotingKey(startVotingKey);
		this.sessionService.extendedAuthenticationFactor(extendedAuthenticationFactor);

		// Prepare and post payload to the Voting-Server.
		const authenticateVoterRequestPayload: AuthenticateVoterRequestPayload = this.prepareAuthenticateVoterRequestPayload(electionEventId, authenticationChallengeOutput);
		const authenticateVoterResponsePayload: AuthenticateVoterResponsePayload = await this.votingServerService.authenticateVoter(authenticateVoterRequestPayload);

		validateAuthenticateVoterResponsePayload(authenticateVoterResponsePayload);

		// If the verification card is already CONFIRMED (happens in a re-login), there is nothing left to calculate.
		const verificationCardState: VerificationCardState = authenticateVoterResponsePayload.verificationCardState;

		// Prepare the response
		const authenticateVoterResponse: AuthenticateVoterResponse = {
			verificationCardState: verificationCardState
		};

		if (verificationCardState === VerificationCardState.INITIAL || verificationCardState === VerificationCardState.SENT) {
			authenticateVoterResponsePayload.voterMaterial.votesTexts?.forEach(voteTexts => validateVoteTexts(voteTexts));
			authenticateVoterResponsePayload.voterMaterial.electionsTexts?.forEach(electionTexts => validateElectionTexts(electionTexts));

			// Store ids in session.
			this.sessionService.voterAuthenticationData(authenticateVoterResponsePayload.voterAuthenticationData);

			// Parse the primitives params.
			const primitivesParams: PrimitivesParams = parsePrimitivesParams(authenticateVoterResponsePayload.votingClientPublicKeys, authenticateVoterResponsePayload.primesMappingTable);
			this.sessionService.primitivesParams(primitivesParams);

			// Call GetKey algorithm.
			const verificationCardSecretKey: ZqElement = await getKey(
				{
					encryptionGroup: primitivesParams.encryptionGroup,
					electionEventId: validateUUID(authenticateVoterResponsePayload.voterAuthenticationData.electionEventId),
					verificationCardSetId: authenticateVoterResponsePayload.voterAuthenticationData.verificationCardSetId,
					verificationCardId: authenticateVoterResponsePayload.verificationCardKeystore.verificationCardId,
					primesMappingTable: primitivesParams.primesMappingTable,
					electionPublicKey: primitivesParams.electionPublicKey,
					choiceReturnCodesEncryptionPublicKey: primitivesParams.choiceReturnCodesEncryptionPublicKey
				},
				startVotingKey,
				authenticateVoterResponsePayload.verificationCardKeystore.verificationCardKeystore
			);

			// Store the verification card secret key in session.
			this.sessionService.verificationCardSecretKey(verificationCardSecretKey);

			// Add the vote texts, the election texts and the write-ins alphabet.
			authenticateVoterResponse.votesTexts = authenticateVoterResponsePayload.voterMaterial.votesTexts;
			authenticateVoterResponse.electionsTexts = authenticateVoterResponsePayload.voterMaterial.electionsTexts;
			authenticateVoterResponse.writeInAlphabet = LATIN_ALPHABET.join("");

			// In the SENT state, we also have the Choice Return Codes to return.
			if (verificationCardState === VerificationCardState.SENT) {
				authenticateVoterResponse.shortChoiceReturnCodes = getShortChoiceReturnCodes(primitivesParams.primesMappingTable,
					ImmutableArray.from(authenticateVoterResponsePayload.voterMaterial.shortChoiceReturnCodes));
			}
		}

		// In the CONFIRMED state, we do not need to perform getKey algorithm. Only the Cast Return Code is needed.
		if (verificationCardState === VerificationCardState.CONFIRMED) {
			authenticateVoterResponse.voteCastReturnCode = authenticateVoterResponsePayload.voterMaterial.shortVoteCastReturnCode;
		}

		return authenticateVoterResponse;
	}


	/**
	 * Throws a Voter Portal input error if the start voting key is not valid.
	 * @param {string} startVotingKey - the voter start voting key.
	 */
	private validateStartVotingKeyInput(startVotingKey: string): void {
		try {
			validateSVK(startVotingKey);
		} catch (_error) {
			throw new VoterPortalInputError(AuthenticateVoterProcess.START_VOTING_KEY_INVALID_ERROR_CODE, _error.message);
		}
	}

	/**
	 * Prepares the authenticateVoter request payload expected by the Voting-Server.
	 *
	 * @param {string} electionEventId - the election event id.
	 * @param {AuthenticationChallengeOutput} authenticationChallengeOutput - the output of the authentication challenge algorithm.
	 *
	 * @returns {AuthenticateVoterRequestPayload} - the authenticateVoter request payload expected by the Voting-Server.
	 */
	private prepareAuthenticateVoterRequestPayload(electionEventId: string, authenticationChallengeOutput: AuthenticationChallengeOutput): AuthenticateVoterRequestPayload {
		return {
			electionEventId: electionEventId,
			authenticationChallenge: {
				derivedVoterIdentifier: authenticationChallengeOutput.derivedVoterIdentifier,
				derivedAuthenticationChallenge: authenticationChallengeOutput.derivedAuthenticationChallenge,
				authenticationNonce: serializeImmutableBigInteger(authenticationChallengeOutput.authenticationNonce)
			}
		};
	}
}



