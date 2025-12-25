/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {AuthenticationChallenge} from "./authenticate-voter.types";

/**
 * @property {string} voteCastReturnCode - the vote cast return code.
 */
export interface ConfirmVoteResponse {
	voteCastReturnCode: string;
}

/**
 * @property {string} shortVoteCastReturnCode - the short vote cast return code.
 */
export interface ConfirmVoteResponsePayload {
	shortVoteCastReturnCode: string;
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
 * @property {ContextIds} contextIds - the context ids.
 * @property {AuthenticationChallenge} authenticationChallenge - the authentication challenge.
 * @property {string} confirmationKey - the serialized confirmation key.
 * @property {string} encryptionGroup - the serialized encryption group.
 */
export interface ConfirmVoteRequestPayload {
	contextIds: ContextIds;
	authenticationChallenge: AuthenticationChallenge;
	confirmationKey: string;
	encryptionGroup: string;
}