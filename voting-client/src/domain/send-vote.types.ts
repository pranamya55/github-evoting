/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ShortChoiceReturnCode} from "./short-choice-return-code.types";
import {AuthenticationChallenge} from "./authenticate-voter.types";

/**
 * @property {ShortChoiceReturnCode[]} shortChoiceReturnCodes - the short choice return codes.
 */
export interface SendVoteResponse {
	shortChoiceReturnCodes: ShortChoiceReturnCode[];
}

/**
 * @property {string[]} shortChoiceReturnCodes - the short choice return codes
 */
export interface SendVoteResponsePayload {
	shortChoiceReturnCodes: string[];
}

/**
 * @property {string} electionEventId - the election event id.
 * @property {string} verificationCardSetId - the verification card set id.
 * @property {string} verificationCardId - the verification card id.
 */
export interface ContextIds {
	electionEventId: string;
	verificationCardSetId: string;
	verificationCardId: string;
}

/**
 * @property {ContextIds} contextIds - the context identifiers.
 * @property {string} encryptedVote - the serialized encrypted vote.
 * @property {string} exponentiatedEncryptedVote - the serialized exponentiated encrypted vote.
 * @property {string} encryptedPartialChoiceReturnCodes - the serialized encrypted partial choice return codes.
 * @property {string} exponentiationProof - the serialized exponentiation proof.
 * @property {string} plaintextEqualityProof - the serialized plaintext equality proof.
 */
export interface EncryptedVerifiableVote {
	contextIds: ContextIds;
	encryptedVote: string;
	exponentiatedEncryptedVote: string;
	encryptedPartialChoiceReturnCodes: string;
	exponentiationProof: string;
	plaintextEqualityProof: string;
}

/**
 * @property {ContextIds} contextIds - the context identifiers.
 * @property {string} encryptionGroup - the serialized encryption group.
 * @property {EncryptedVerifiableVote} encryptedVerifiableVote - the encrypted verifiable vote.
 * @property {AuthenticationChallenge} authenticationChallenge - the authentication challenge.
 */
export interface SendVoteRequestPayload {
	contextIds: ContextIds;
	encryptionGroup: string;
	encryptedVerifiableVote: EncryptedVerifiableVote;
	authenticationChallenge: AuthenticationChallenge;
}
