/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

/**
 * @property {string} electionEventId - ee, the election event id.
 * @property {string} extendedAuthenticationFactorLength - l_EA, the extended authentication factor length.
 */
export interface DeriveBaseAuthenticationChallengeContext {
	electionEventId: string;
	extendedAuthenticationFactorLength: number;
}