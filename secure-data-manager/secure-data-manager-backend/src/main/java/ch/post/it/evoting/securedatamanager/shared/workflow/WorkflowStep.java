/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static ch.post.it.evoting.securedatamanager.shared.workflow.ServerMode.SERVER_MODE_ONLINE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.ServerMode.SERVER_MODE_SETUP;
import static ch.post.it.evoting.securedatamanager.shared.workflow.ServerMode.SERVER_MODE_TALLY;
import static com.google.common.base.Preconditions.checkArgument;

public enum WorkflowStep {
	PRE_CONFIGURE(SERVER_MODE_SETUP),
	PRE_COMPUTE(SERVER_MODE_SETUP),
	EXPORT_TO_ONLINE_1(SERVER_MODE_SETUP),
	IMPORT_FROM_SETUP_1(SERVER_MODE_ONLINE),
	REQUEST_CC_KEYS(SERVER_MODE_ONLINE),
	COMPUTE(SERVER_MODE_ONLINE),
	DOWNLOAD(SERVER_MODE_ONLINE),
	EXPORT_TO_SETUP_2(SERVER_MODE_ONLINE),
	IMPORT_FROM_ONLINE_2(SERVER_MODE_SETUP),
	GENERATE(SERVER_MODE_SETUP),
	GENERATE_PRINT_FILE(SERVER_MODE_SETUP),
	EXPORT_TO_ONLINE_3(SERVER_MODE_SETUP),
	IMPORT_FROM_SETUP_3(SERVER_MODE_ONLINE),
	UPLOAD_CONFIGURATION_1(SERVER_MODE_ONLINE),
	CONSTITUTE_ELECTORAL_BOARD(SERVER_MODE_SETUP),
	COLLECT_DATA_VERIFIER_SETUP(SERVER_MODE_SETUP),
	EXPORT_TO_ONLINE_4(SERVER_MODE_SETUP),
	IMPORT_FROM_SETUP_4(SERVER_MODE_ONLINE),
	UPLOAD_CONFIGURATION_2(SERVER_MODE_ONLINE),
	CONFIGURE_VOTER_PORTAL(SERVER_MODE_ONLINE),
	MIX_DOWNLOAD(SERVER_MODE_ONLINE),
	MIX_BALLOT_BOX(SERVER_MODE_ONLINE),
	DOWNLOAD_BALLOT_BOX(SERVER_MODE_ONLINE),
	EXPORT_PARTIALLY_TO_TALLY_5(SERVER_MODE_ONLINE),
	EXPORT_TO_TALLY_5(SERVER_MODE_ONLINE),
	IMPORT_FROM_ONLINE_5(SERVER_MODE_TALLY),
	DECRYPT(SERVER_MODE_TALLY),
	DECRYPT_BALLOT_BOX(SERVER_MODE_TALLY),
	COLLECT_DATA_VERIFIER_TALLY(SERVER_MODE_TALLY);

	private final ServerMode mode;

	WorkflowStep(final ServerMode mode) {
		this.mode = mode;
	}

	public ServerMode getMode() {
		return mode;
	}

	public static WorkflowStep getExportStep(final int exchangeIndex) {
		validateExchangeIndex(exchangeIndex);
		return switch (exchangeIndex) {
			case 1 -> EXPORT_TO_ONLINE_1;
			case 2 -> EXPORT_TO_SETUP_2;
			case 3 -> EXPORT_TO_ONLINE_3;
			case 4 -> EXPORT_TO_ONLINE_4;
			case 5 -> EXPORT_TO_TALLY_5;
			case 50 -> EXPORT_PARTIALLY_TO_TALLY_5;
			default -> null;
		};
	}

	public static WorkflowStep getImportStep(final int exchangeIndex) {
		validateExchangeIndex(exchangeIndex);
		return switch (exchangeIndex) {
			case 1 -> IMPORT_FROM_SETUP_1;
			case 2 -> IMPORT_FROM_ONLINE_2;
			case 3 -> IMPORT_FROM_SETUP_3;
			case 4 -> IMPORT_FROM_SETUP_4;
			case 5 -> IMPORT_FROM_ONLINE_5;
			default -> null;
		};
	}

	public static WorkflowStep getUploadConfigurationStep(final int day) {
		checkArgument(day == 1 || day == 2, "Day value must be 1 or 2");
		if (day == 1) {
			return UPLOAD_CONFIGURATION_1;
		}
		return UPLOAD_CONFIGURATION_2;
	}

	public boolean isFractionable() {
		return this.equals(MIX_DOWNLOAD) || this.equals(DECRYPT);
	}

	public boolean isOptional() {
		return this.equals(EXPORT_PARTIALLY_TO_TALLY_5);
	}

	private static void validateExchangeIndex(final int exchangeIndex) {
		checkArgument((exchangeIndex >= 1 && exchangeIndex <= 5) || exchangeIndex == 50, "ExchangeIndex value must be between 1 and 5, or 50");
	}
}
