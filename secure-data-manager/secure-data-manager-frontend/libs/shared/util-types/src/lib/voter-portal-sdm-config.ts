/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export interface VoterPortalSdmConfig {
	sourcePath: string | undefined;
	payload: VoterPortalConfig | undefined;
	state: VoterPortalConfigState | undefined;
	url: string | undefined;
}

export interface VoterPortalConfig {
	config: string | undefined;
	favicon: string | undefined;
	logo: string | undefined;
}

export interface VoterPortalConfigState {
	config: VoterPortalConfigStatus | undefined;
	favicon: VoterPortalConfigStatus | undefined;
	logo: VoterPortalConfigStatus | undefined;
}

export enum VoterPortalConfigStatus {
	NotFound = 'NOT_FOUND',
	Synchronized = 'SYNCHRONIZED',
	NotSynchronized = 'NOT_SYNCHRONIZED'
}
