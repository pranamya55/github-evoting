/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

/**
 * @property {string} electionEventId - the election event id.
 * @property {string} config - the voter portal config json.
 * @property {string} favicon - the voter portal favicon.
 * @property {string} logo - the voter portal logo.
 * @property {number} votingServerTime - the voting server time.
 */
export interface ConfigureVoterPortalResponsePayload {
	electionEventId: string;
	config: string;
	favicon: string;
	logo: string;
	votingServerTime: number;
}