/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export enum WorkflowStep {
	PreConfigure = 'PRE_CONFIGURE',
	PreCompute = 'PRE_COMPUTE',
	ExportToOnline1 = 'EXPORT_TO_ONLINE_1',
	ImportFromSetup1 = 'IMPORT_FROM_SETUP_1',
	RequestCcKeys = 'REQUEST_CC_KEYS',
	Compute = 'COMPUTE',
	Download = 'DOWNLOAD',
	ExportToSetup2 = 'EXPORT_TO_SETUP_2',
	ImportFromOnline2 = 'IMPORT_FROM_ONLINE_2',
	Generate = 'GENERATE',
	GeneratePrintFile = 'GENERATE_PRINT_FILE',
	ExportToOnline3 = 'EXPORT_TO_ONLINE_3',
	ImportFromSetup3 = 'IMPORT_FROM_SETUP_3',
	UploadConfiguration1 = 'UPLOAD_CONFIGURATION_1',
	ConstituteElectoralBoard = 'CONSTITUTE_ELECTORAL_BOARD',
	CollectDataVerifierSetup = 'COLLECT_DATA_VERIFIER_SETUP',
	ExportToOnline4 = 'EXPORT_TO_ONLINE_4',
	ImportFromSetup4 = 'IMPORT_FROM_SETUP_4',
	UploadConfiguration2 = 'UPLOAD_CONFIGURATION_2',
	ConfigureVoterPortal = 'CONFIGURE_VOTER_PORTAL',
	MixAndDownload = 'MIX_DOWNLOAD',
	MixBallotBox = "MIX_BALLOT_BOX",
	DownloadBallotBox = "DOWNLOAD_BALLOT_BOX",
	ExportPartiallyToTally5 = 'EXPORT_PARTIALLY_TO_TALLY_5',
	ExportToTally5 = 'EXPORT_TO_TALLY_5',
	ImportFromOnline5 = 'IMPORT_FROM_ONLINE_5',
	Decrypt = 'DECRYPT',
	CollectDataVerifierTally = 'COLLECT_DATA_VERIFIER_TALLY',
	DecryptBallotBox = "DECRYPT_BALLOT_BOX",
}

export class WorkflowStepUtil {
	public static getExportStep(exchangeIndex: number): WorkflowStep {
		switch (exchangeIndex) {
			case 1:
				return WorkflowStep.ExportToOnline1;
			case 2:
				return WorkflowStep.ExportToSetup2;
			case 3:
				return WorkflowStep.ExportToOnline3;
			case 4:
				return WorkflowStep.ExportToOnline4;
			case 5:
				return WorkflowStep.ExportToTally5;
			case 50:
				return WorkflowStep.ExportPartiallyToTally5;
		}
		return WorkflowStep.ExportToOnline1;
	}

	public static getImportStep(exchangeIndex: number): WorkflowStep {
		switch (exchangeIndex) {
			case 1:
				return WorkflowStep.ImportFromSetup1;
			case 2:
				return WorkflowStep.ImportFromOnline2;
			case 3:
				return WorkflowStep.ImportFromSetup3;
			case 4:
				return WorkflowStep.ImportFromSetup4;
			case 5:
				return WorkflowStep.ImportFromOnline5;
		}
		return WorkflowStep.ImportFromSetup1;
	}

	public static getUploadStep(day: number): WorkflowStep {
		if (day === 1) {
			return WorkflowStep.UploadConfiguration1;
		}
		return WorkflowStep.UploadConfiguration2;
	}
}
