/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Vote} from "../protocol/voting-phase/send-vote/create-vote.types";
import {createVote} from "../protocol/voting-phase/send-vote/create-vote.algorithm";
import {validateUUID} from "../domain/validations/validations";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {SessionService} from "../session.service";
import {PrimitivesParams} from "../domain/primitives-params.types";
import {VotingServerService} from "./voting-server.service";
import {AuthenticationChallengeOutput} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.types";
import {VoterAuthenticationData} from "../domain/authenticate-voter.types";
import {getAuthenticationChallenge} from "../protocol/voting-phase/authenticate-voter/get-authentication-challenge.algorithm";
import {SendVoteRequestPayload, SendVoteResponse, SendVoteResponsePayload} from "../domain/send-vote.types";
import {
	serializeElGamalCiphertext,
	serializeExponentiationProof,
	serializeGqGroup,
	serializeImmutableBigInteger,
	serializePlaintextEqualityProof
} from "../domain/primitives-serializer";
import {VoterAnswers} from "e-voting-libraries-ui-kit";
import {getSelectedActualVotingOptions} from "../domain/election/selected-actual-voting-options-builder";
import {PrimesMappingTable} from "../domain/election/primes-mapping-table";
import {getSelectedWriteIns} from "../domain/election/selected-write-ins-builder";
import {getShortChoiceReturnCodes} from "../domain/short-choice-return-code-builder";
import {validateSendVoteResponsePayload} from "../domain/validations/send-vote-validation";

export class SendVoteProcess {
	private readonly sessionService: SessionService;
	private readonly votingServerService: VotingServerService;

	constructor() {
		this.sessionService = SessionService.getInstance();
		this.votingServerService = new VotingServerService();
	}

	/**
	 * This process implements the sendVote phase of the protocol:
	 *  - Receives the VoterAnswers from the Voter Portal.
	 *  - Builds the selected actual voting options and selected write-ins from the voter's answers.
	 *  - Calls the CreateVote algorithm.
	 *  - Calls the GetAuthenticationChallenge algorithm.
	 *  - Sends a SendVoteRequestPayload to the Voting Server.
	 *  - Returns a SendVoteResponse to the Voter Portal.
	 *
	 * @param {VoterAnswers} voterAnswers - the voter's answers.
	 *
	 * @returns {Promise<SendVoteResponse>}, the sendVote response expected by the Voter-Portal.
	 */
	public async sendVote(voterAnswers: VoterAnswers): Promise<SendVoteResponse> {
		const primitivesParams: PrimitivesParams = this.sessionService.primitivesParams();
		const primesMappingTable: PrimesMappingTable = primitivesParams.primesMappingTable;
		const voterAuthenticationData: VoterAuthenticationData = this.sessionService.voterAuthenticationData();

		const selectedActualVotingOptions: ImmutableArray<string> = getSelectedActualVotingOptions(voterAnswers, primesMappingTable);
		const selectedWriteIns: ImmutableArray<string> = getSelectedWriteIns(voterAnswers);

		// Call CreateVote algorithm
		const createVoteOutput: Vote = createVote(
			{
				encryptionGroup: primitivesParams.encryptionGroup,
				electionEventId: validateUUID(voterAuthenticationData.electionEventId),
				verificationCardSetId: validateUUID(voterAuthenticationData.verificationCardSetId),
				verificationCardId: validateUUID(voterAuthenticationData.verificationCardId),
				primesMappingTable: primitivesParams.primesMappingTable,
				electionPublicKey: primitivesParams.electionPublicKey,
				choiceReturnCodesEncryptionPublicKey: primitivesParams.choiceReturnCodesEncryptionPublicKey,
			},
			selectedActualVotingOptions,
			selectedWriteIns,
			this.sessionService.verificationCardSecretKey()
		);

		const extendedAuthenticationFactor: string = this.sessionService.extendedAuthenticationFactor();

		// Call GetAuthenticationChallenge algorithm
		const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
			{
				electionEventId: voterAuthenticationData.electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			"sendVote",
			this.sessionService.startVotingKey(),
			extendedAuthenticationFactor
		);

		// Prepare and post payload to the Voting Server
		const sendVoteRequestPayload: SendVoteRequestPayload = this.prepareSendVoteRequestPayload(createVoteOutput, authenticationChallengeOutput);
		const sendVoteResponsePayload: SendVoteResponsePayload = await this.votingServerService.sendVote(sendVoteRequestPayload);

		validateSendVoteResponsePayload(sendVoteResponsePayload);

		return {
			shortChoiceReturnCodes: getShortChoiceReturnCodes(primesMappingTable, ImmutableArray.from(sendVoteResponsePayload.shortChoiceReturnCodes))
		};
	}

	/**
	 * Prepares the sendVote request payload expected by the Voting-Server.
	 *
	 * @param {Vote} createVoteOutput - the output of the createVote algorithm.
	 * @param {AuthenticationChallengeOutput} authenticationChallengeOutput - the output of the authentication challenge algorithm.
	 *
	 * @returns {SendVoteRequestPayload} - the sendVote request payload expected by the Voting-Server.
	 */
	private prepareSendVoteRequestPayload(createVoteOutput: Vote, authenticationChallengeOutput: AuthenticationChallengeOutput): SendVoteRequestPayload {
		// Serialize vote elements for payload
		const serializedEncryptedVote: string = serializeElGamalCiphertext(createVoteOutput.encryptedVote);
		const serializedExponentiatedEncryptedVote: string = serializeElGamalCiphertext(createVoteOutput.exponentiatedEncryptedVote);
		const serializedEncryptedPartialChoiceReturnCodes: string = serializeElGamalCiphertext(createVoteOutput.encryptedPartialChoiceReturnCodes);
		const serializedExponentiationProof: string = serializeExponentiationProof(createVoteOutput.exponentiationProof);
		const serializedPlaintextEqualityProof: string = serializePlaintextEqualityProof(createVoteOutput.plaintextEqualityProof);

		const primitivesParams: PrimitivesParams = this.sessionService.primitivesParams();
		const serializedEncryptionGroup: string = serializeGqGroup(primitivesParams.encryptionGroup);
		const voterAuthenticationData: VoterAuthenticationData = this.sessionService.voterAuthenticationData();

		return {
			contextIds: {
				electionEventId: voterAuthenticationData.electionEventId,
				verificationCardSetId: voterAuthenticationData.verificationCardSetId,
				verificationCardId: voterAuthenticationData.verificationCardId
			},
			encryptionGroup: JSON.parse(serializedEncryptionGroup),
			encryptedVerifiableVote: {
				contextIds: {
					electionEventId: voterAuthenticationData.electionEventId,
					verificationCardSetId: voterAuthenticationData.verificationCardSetId,
					verificationCardId: voterAuthenticationData.verificationCardId
				},
				encryptedVote: JSON.parse(serializedEncryptedVote),
				exponentiatedEncryptedVote: JSON.parse(serializedExponentiatedEncryptedVote),
				encryptedPartialChoiceReturnCodes: JSON.parse(serializedEncryptedPartialChoiceReturnCodes),
				exponentiationProof: JSON.parse(serializedExponentiationProof),
				plaintextEqualityProof: JSON.parse(serializedPlaintextEqualityProof)
			},
			authenticationChallenge: {
				derivedVoterIdentifier: authenticationChallengeOutput.derivedVoterIdentifier,
				derivedAuthenticationChallenge: authenticationChallengeOutput.derivedAuthenticationChallenge,
				authenticationNonce: serializeImmutableBigInteger(authenticationChallengeOutput.authenticationNonce)
			}
		};
	}
}
