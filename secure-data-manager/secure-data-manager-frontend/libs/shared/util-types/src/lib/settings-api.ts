/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export interface SettingsApi {
	getSettingsList: () => Settings[];
	saveSettings: (settings: Settings, startSDM: boolean) => void;
	deleteSettings: (id: number) => void;
	isValidPath: (path: string) => boolean;
}

declare global {
	interface Window {
		SettingsApi: SettingsApi;
	}
}

export interface Settings {
	id: number;
	mode: string;
	electionEventSeed: string;
	voterPortalHost: string;
	votingServerHost: string;
	twoWaySSLLocation: string;
	twoWaySSLPwdLocation: string;
	workspaceFolder: string;
	outputFolder: string;
	externalConfigurationFolder: string;
	printFolder: string;
	verifierFolder: string;
	tallyFolder: string;
	voterPortalConfigurationFolder: string;
	exportPwd: string;
	verifierDatasetPwd: string;
	choiceCodeGenerationChunkSize: number;
	directTrustLocation: string;
	directTrustPwdLocation: string;
	electionEventDescription?: string;
	electionEventId?: string;
	proxyConfig?: string;
}
