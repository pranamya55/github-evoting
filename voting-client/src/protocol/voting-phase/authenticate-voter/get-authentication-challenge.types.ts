/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";

/**
 * @property {string} electionEventId - ee, the election event id.
 * @property {string} extendedAuthenticationFactorLength - l_EA, the extended authentication factor length.
 */
export interface AuthenticationChallengeContext {
	electionEventId: string;
	extendedAuthenticationFactorLength: number;
}

/**
 * @property {string} derivedVoterIdentifier - credentialID_id, The derived voter identifier.
 * @property {string} derivedAuthenticationChallenge - hhAuth_id, The derived authentication challenge.
 * @property {ImmutableBigInteger} authenticationNonce - nonce, The authentication nonce.
 */
export interface AuthenticationChallengeOutput {
	derivedVoterIdentifier: string;
	derivedAuthenticationChallenge: string;
	authenticationNonce: ImmutableBigInteger;
}