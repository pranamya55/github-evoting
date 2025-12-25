/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ElectionTexts, VoteTexts} from "e-voting-libraries-ui-kit";
import {ShortChoiceReturnCode} from "./short-choice-return-code.types";

/**
 * @property {VerificationCardState} verificationCardState - the verification card state.
 * @property {VoteTexts[]} [votesTexts] - the votes texts for the voter.
 * @property {ElectionTexts[]} [electionsTexts] - the elections texts for the voter.
 * @property {string} [writeInAlphabet] - the write-in alphabet.
 * @property {ShortChoiceReturnCode[]} [shortChoiceReturnCodes] - the short choice return codes.
 * @property {string} [voteCastReturnCode] - the short vote cast return code.
 */
export interface AuthenticateVoterResponse {
	verificationCardState: VerificationCardState;
	votesTexts?: VoteTexts[];
	electionsTexts?: ElectionTexts[];
	writeInAlphabet?: string;
	shortChoiceReturnCodes?: ShortChoiceReturnCode[];
	voteCastReturnCode?: string;
}

/**
 * @property {string} electionEventId - the election event id.
 * @property {AuthenticationChallenge} authenticationChallenge - the authentication challenge.
 */
export interface AuthenticateVoterRequestPayload {
	electionEventId: string;
	authenticationChallenge: AuthenticationChallenge;
}

/**
 * @property {string} derivedVoterIdentifier - credentialID_id, The derived voter identifier.
 * @property {string} derivedAuthenticationChallenge - hhAuth_id, The derived authentication challenge.
 * @property {string} authenticationNonce - nonce, The authentication nonce.
 */
export interface AuthenticationChallenge {
	derivedVoterIdentifier: string;
	derivedAuthenticationChallenge: string;
	authenticationNonce: string;
}

/**
 * @property {string} electionEventId - the election event id.
 * @property {string} verificationCardSetId - the verification card set id.
 * @property {string} ballotBoxId - the ballot box id.
 * @property {string} verificationCardId - the verification card id.
 * @property {string} ballotBoxId - the voting card id.
 * @property {string} credentialId - the credential id.
 */
export interface VoterAuthenticationData {
	electionEventId: string;
	verificationCardSetId: string;
	ballotBoxId: string;
	verificationCardId: string;
	votingCardId: string;
	credentialId: string;
}

/**
 * @property {string} verificationCardId - the verification card id.
 * @property {string} verificationCardKeystore - the verification card keystore representation.
 */
export interface VerificationCardKeystore {
	verificationCardId: string;
	verificationCardKeystore: string;
}

/**
 * @property {string} p - the serialized p.
 * @property {string} q - the serialized q.
 * @property {string} g - the serialized g.
 */
export interface EncryptionParameters {
	p: string;
	q: string;
	g: string;
}

/**
 * @property {EncryptionParameters} encryptionParameters - the serialized encryption parameters.
 * @property {string[]} electionPublicKey - the serialized election public key.
 * @property {string[]} choiceReturnCodesEncryptionPublicKey - the serialized choice return codes encryption public key.
 */
export interface VotingClientPublicKeys {
	encryptionParameters: EncryptionParameters;
	electionPublicKey: string[];
	choiceReturnCodesEncryptionPublicKey: string[];
}

/**
 * @property {VoteTexts[]} [votesTexts] - the votes texts for the voter.
 * @property {ElectionTexts[]} [electionsTexts] - the elections texts for the voter.
 * @property {string[]} [shortChoiceReturnCodes] - the short choice return codes.
 * @property {string} [shortVoteCastReturnCode] - the short vote cast return code.
 */
export interface VoterMaterial {
	votesTexts: VoteTexts[];
	electionsTexts: ElectionTexts[];
	shortChoiceReturnCodes?: string[];
	shortVoteCastReturnCode?: string;
}


/**
 * @property {string} actualVotingOption - the actual voting option entry.
 * @property {number} encodedVotingOption - the encoded voting option entry (prime number).
 * @property {string} semanticInformation - the semantic information entry.
 */
export interface PrimesMappingTableEntryRaw {
	actualVotingOption: string;
	encodedVotingOption: number;
	semanticInformation: string;
	correctnessInformation: string;
}

/**
 * @property {PrimesMappingTableEntryRaw[]} pTable - the prime mapping table entries.
 */
export interface PrimesMappingTableRaw {
	pTable: PrimesMappingTableEntryRaw[];
}

/**
 * @property {VerificationCardState} verificationCardState - the verification card state.
 * @property {VoterMaterial} voterMaterial - the voter material.
 * @property {VoterAuthenticationData} voterAuthenticationData - the voter context ids.
 * @property {VerificationCardKeystore} verificationCardKeystore - the verification card keystore.
 * @property {VotingClientPublicKeys} votingClientPublicKeys - contains the public keys.
 */
export interface AuthenticateVoterResponsePayload {
	verificationCardState: VerificationCardState;
	voterMaterial: VoterMaterial;
	voterAuthenticationData: VoterAuthenticationData;
	verificationCardKeystore: VerificationCardKeystore;
	votingClientPublicKeys: VotingClientPublicKeys;
	primesMappingTable: PrimesMappingTableRaw;
}

export enum VerificationCardState {
	INITIAL = 'INITIAL',
	SENT = 'SENT',
	CONFIRMED = 'CONFIRMED',
}
