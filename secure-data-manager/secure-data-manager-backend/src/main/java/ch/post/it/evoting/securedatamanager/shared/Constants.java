/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared;

public final class Constants {

	// ////////////////////////////////////
	//
	// file extensions
	//
	// ////////////////////////////////////

	public static final String JSON = ".json";
	public static final String XML = ".xml";
	public static final String P12 = ".p12";
	public static final String TXT = ".txt";

	// ////////////////////////////////////
	//
	// filenames
	//
	// ////////////////////////////////////

	public static final String BALLOT_BOXES_REPORT_FILE_NAME = "ballotBoxesReport_%s" + JSON; // ballotBoxesReport_{seed}.json
	public static final String CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD = "controlComponentCodeSharesPayload";
	public static final String CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED = "configuration-anonymized" + XML;
	public static final String CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD = "electionEventContextPayload" + JSON;
	public static final String CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_CM_TABLE_PAYLOAD = "setupComponentCMTablePayload.";
	public static final String CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_VERIFICATION_DATA_PAYLOAD = "setupComponentVerificationDataPayload.";
	public static final String CONFIG_FILE_NAME_SETUP_COMPONENT_PUBLIC_KEYS_PAYLOAD = "setupComponentPublicKeysPayload" + JSON;
	public static final String CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD = "setupComponentTallyDataPayload" + JSON;
	public static final String CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD =
			"setupComponentVerificationCardKeystoresPayload" + JSON;
	public static final String CONFIG_FILE_NAME_VERIFICATION_CARD_SECRET_KEY_PAYLOAD = "verificationCardSecretKeyPayload" + JSON;
	public static final String CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD = "voterInitialCodesPayload" + JSON;
	public static final String CONFIG_FILE_NAME_VOTER_RETURN_CODES_PAYLOAD = "voterReturnCodesPayload" + JSON;
	public static final String CONFIG_SETUP_COMPONENT_ELECTORAL_BOARD_HASHES_PAYLOAD = "setupComponentElectoralBoardHashesPayload" + JSON;
	public static final String DBDUMP_FILE_NAME = "db_dump" + JSON;
	public static final String IMPORT_EXPORT_MANIFEST = "manifest" + JSON;
	public static final String VERIFIER_DATASET_MANIFEST = "manifest" + JSON;
	public static final String SDM_CONFIG_FILE_NAME_ELECTIONS_CONFIG = "elections_config" + JSON;
	public static final String SETUP_COMPONENT_EVOTING_PRINT_XML = "evoting-print_%s" + XML; // evoting-print_{seed}.xml
	public static final String SETUP_KEY_PAIR_FILE_NAME = "setupKeyPair" + JSON;
	public static final String TALLY_COMPONENT_ECH_0222_XML = "eCH-0222_v%s_%s" + XML; // eCH-0222_v[MajorVersion]-[MinorVersion]_[seed].xml

	// ////////////////////////////////////
	//
	// directories
	//
	// ////////////////////////////////////

	public static final String BALLOT_BOXES = "ballotBoxes";
	public static final String CONFIGURATION = "configuration";
	public static final String VERIFICATION_CARD_SETS = "verificationCardSets";

	// ////////////////////////////////////
	//
	// exception messages
	//
	// ////////////////////////////////////

	public static final String BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE = "The ballot box can not be mixed.";
	public static final String BALLOT_BOX_NOT_CLOSED_MESSAGE = "The ballot box is not yet closed and cannot be mixed.";
	public static final String CANNOT_READ_MANIFEST_FILE_MESSAGE = "Cannot read the manifest file.";
	public static final String CANNOT_UNZIP_FILE_MESSAGE = "Cannot unzip the file.";
	public static final String CONFIG_FILE_MISSING_MESSAGE = "Could not find the requested canton config file.";
	public static final String DOWNLOAD_UNSUCCESSFUL_MESSAGE = "Download unsuccessful.";
	public static final String FAILED_TO_CREATE_XML_OUTPUT_FILE = "Failed to create xml output file.";
	public static final String GET_STATUS_UNSUCCESSFUL_MESSAGE = "Get status unsuccessful.";
	public static final String START_ONLINE_MIXING_FAILED_MESSAGE = "Failed to start online mixing.";
	public static final String MIX_BALLOT_BOX_FAILED_MESSAGE = "Failed to mix ballot box.";
	public static final String TOO_SMALL_CHUNK_SIZE_MESSAGE = "The chunk size must be strictly positive.";
	public static final String UNSUCCESSFUL_RESPONSE_MESSAGE = "Unsuccessful response";
	public static final String UPLOAD_ELECTION_EVENT_CONTEXT_FAILED_MESSAGE = "Upload Election Event Context and request for the Control Components keys failed.";
	public static final String USB_DIRECTORY_NOT_A_DIRECTORY_MESSAGE = "usbDirectory is not a directory.";
	public static final String PAYLOAD_SIGNATURE_IS_INVALID = "Signature of payload ElectoralBoardHashesPayload is invalid";

	public static final String IMPORT_STEP_NOT_EXIST_MESSAGE = "Import step not exist.";
	public static final String IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP_MESSAGE = "Import content not match current import step.";
	public static final String IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_MESSAGE = "Import content not match current election event.";
	public static final String IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED_MESSAGE = "Import content not match current election event seed.";

	// ////////////////////////////////////
	//
	// pattern
	//
	// ////////////////////////////////////
	public static final String DATE_TIME_FORMAT_PATTERN = "uuuuMMdd_HHmm";

	private Constants() {
	}

}
