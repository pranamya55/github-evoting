/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export enum WorkflowExceptionCode {
  // Pre-configure
  // Pre-compute
  NegativeChunkSize = 'NEGATIVE_CHUNK_SIZE',
  // Export
  OutputFolderNotSet = 'OUTPUT_FOLDER_NOT_SET',
  BadSdmOutputFolder = 'BAD_SDM_OUTPUT_FOLDER_PATH',
  OutputFolderMissing = 'OUTPUT_FOLDER_MISSING',
  // Import
  CheckZipPassword = 'CHECK_ZIP_PASSWORD',
  // Generate print file
  OutputPrintFolderNotSet = 'OUTPUT_PRINT_FOLDER_NOT_SET',
  ConfigurationAnonymizedMissing = 'CONFIGURATION_ANONYMIZED_MISSING',
  // Collect
  OutputVerifierFolderNotSet = 'OUTPUT_VERIFIER_FOLDER_NOT_SET',
  OutputTallyFolderNotSet = 'OUTPUT_TALLY_FOLDER_NOT_SET',
  // Mix download
  VoterPortalNotEnabled = 'VOTER_PORTAL_NOT_ENABLED',
  // Decrypt
  BallotBoxNotClosed = 'BALLOT_BOX_NOT_CLOSED',
  // General
  TimeOut = 'TIME_OUT',
  ConnectionError = 'CONNECTION_ERROR',
  Default = 'DEFAULT',
  None = 'NONE',
}
