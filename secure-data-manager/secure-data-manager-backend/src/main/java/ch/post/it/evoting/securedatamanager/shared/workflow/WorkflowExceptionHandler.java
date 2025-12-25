/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOX_NOT_CLOSED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CANNOT_READ_MANIFEST_FILE_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CANNOT_UNZIP_FILE_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_MISSING_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.DOWNLOAD_UNSUCCESSFUL_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.FAILED_TO_CREATE_XML_OUTPUT_FILE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.GET_STATUS_UNSUCCESSFUL_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_STEP_NOT_EXIST_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.MIX_BALLOT_BOX_FAILED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.PAYLOAD_SIGNATURE_IS_INVALID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.START_ONLINE_MIXING_FAILED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.UNSUCCESSFUL_RESPONSE_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.UPLOAD_ELECTION_EVENT_CONTEXT_FAILED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.USB_DIRECTORY_NOT_A_DIRECTORY_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.BAD_SDM_OUTPUT_FOLDER_PATH;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.BALLOT_BOX_NOT_CLOSED;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.CHECK_ZIP_PASSWORD;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.CONFIGURATION_ANONYMIZED_MISSING;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.CONFIGURE_VOTER_PORTAL_INVALID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.CONNECTION_ERROR;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.DEFAULT;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.IMPORT_STEP_NOT_EXIST;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.INVALID_SIGNATURE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.NEGATIVE_CHUNK_SIZE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.OUTPUT_FOLDER_MISSING;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.TIME_OUT;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.USB_DIRECTORY_NOT_EXIST;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ch.post.it.evoting.domain.InvalidPayloadSignatureException;

@Service
public class WorkflowExceptionHandler {

	public WorkflowExceptionCode handleException(final WorkflowStep workflowStep, final Throwable throwable) {
		final WorkflowStep step = checkNotNull(workflowStep);
		final Throwable toCheck = checkNotNull(throwable).getCause();

		return switch (step) {
			// case PRE_CONFIGURE is treated in default
			case PRE_COMPUTE -> handlePreComputeException(toCheck);
			case EXPORT_TO_ONLINE_1, EXPORT_TO_SETUP_2, EXPORT_TO_ONLINE_3, EXPORT_TO_ONLINE_4, EXPORT_TO_TALLY_5, COLLECT_DATA_VERIFIER_SETUP,
				 COLLECT_DATA_VERIFIER_TALLY -> handleExportException(toCheck);
			case IMPORT_FROM_SETUP_1, IMPORT_FROM_ONLINE_2, IMPORT_FROM_SETUP_3, IMPORT_FROM_SETUP_4, IMPORT_FROM_ONLINE_5 ->
					handleImportException(toCheck);
			case REQUEST_CC_KEYS -> handleConnectionException(toCheck, UPLOAD_ELECTION_EVENT_CONTEXT_FAILED_MESSAGE);
			case COMPUTE -> handleConnectionException(toCheck, GET_STATUS_UNSUCCESSFUL_MESSAGE);
			case DOWNLOAD, DOWNLOAD_BALLOT_BOX -> handleConnectionException(toCheck, DOWNLOAD_UNSUCCESSFUL_MESSAGE);
			// case GENERATE treated in default
			case GENERATE_PRINT_FILE -> handleGeneratePrintFileException(toCheck);
			case CONFIGURE_VOTER_PORTAL -> handleInvalidConfigureVoterPortalException(toCheck);
			case UPLOAD_CONFIGURATION_1, UPLOAD_CONFIGURATION_2 -> handleConnectionException(toCheck, UNSUCCESSFUL_RESPONSE_MESSAGE);
			// case CONSTITUTE_ELECTORAL_BOARD treated in default
			case MIX_DOWNLOAD -> handleMixDownloadException(toCheck);
			case MIX_BALLOT_BOX -> handleConnectionException(toCheck, MIX_BALLOT_BOX_FAILED_MESSAGE);
			case DECRYPT -> handleDecryptException(toCheck);
			default -> DEFAULT;
		};
	}

	private WorkflowExceptionCode handleInvalidConfigureVoterPortalException(final Throwable toCheck) {
		if (toCheck instanceof IllegalStateException) {
			return CONFIGURE_VOTER_PORTAL_INVALID;
		}
		return DEFAULT;
	}

	private WorkflowExceptionCode handlePreComputeException(final Throwable toCheck) {
		if (toCheck instanceof IllegalArgumentException) {
			return NEGATIVE_CHUNK_SIZE;
		}
		return DEFAULT;
	}

	private WorkflowExceptionCode handleExportException(final Throwable toCheck) {
		return switch (toCheck) {
			case final InvalidPathException ignored -> BAD_SDM_OUTPUT_FOLDER_PATH;
			case final IOException ignored -> OUTPUT_FOLDER_MISSING;
			case final UncheckedIOException ignored -> OUTPUT_FOLDER_MISSING;
			default -> DEFAULT;
		};
	}

	private WorkflowExceptionCode handleImportException(final Throwable toCheck) {
		return switch (toCheck) {
			case final UncheckedIOException e
					when e.getMessage().equals(CANNOT_UNZIP_FILE_MESSAGE) || e.getMessage().equals(CANNOT_READ_MANIFEST_FILE_MESSAGE) ->
					CHECK_ZIP_PASSWORD;
			case final IllegalArgumentException e
					when e.getMessage().startsWith(USB_DIRECTORY_NOT_A_DIRECTORY_MESSAGE) -> USB_DIRECTORY_NOT_EXIST;
			case final NullPointerException e
					when e.getMessage().startsWith(IMPORT_STEP_NOT_EXIST_MESSAGE) -> IMPORT_STEP_NOT_EXIST;
			case final IllegalStateException e
					when e.getMessage().startsWith(IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP_MESSAGE) ->
					IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP;
			case final IllegalStateException e
					when e.getMessage().startsWith(IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_MESSAGE) ->
					IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT;
			case final IllegalStateException e
					when e.getMessage().startsWith(IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED_MESSAGE) ->
					IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED;
			default -> DEFAULT;
		};
	}

	private WorkflowExceptionCode handleConnectionException(final Throwable toCheck, final String unsuccessfulResponse) {
		return switch (toCheck) {
			case final UncheckedIOException ignored -> CONNECTION_ERROR;
			case final IllegalStateException e when e.getMessage().startsWith(unsuccessfulResponse) -> TIME_OUT;
			case final IllegalStateException ignored -> CONNECTION_ERROR;
			case final WebClientRequestException ignored -> CONNECTION_ERROR;
			case final WebClientResponseException ignored -> CONNECTION_ERROR;
			default -> DEFAULT;
		};
	}

	private WorkflowExceptionCode handleGeneratePrintFileException(final Throwable toCheck) {
		return switch (toCheck) {
			case final IllegalStateException e when e.getMessage().startsWith(CONFIG_FILE_MISSING_MESSAGE) -> CONFIGURATION_ANONYMIZED_MISSING;
			case final IllegalStateException e when e.getMessage().equals(FAILED_TO_CREATE_XML_OUTPUT_FILE) -> OUTPUT_FOLDER_MISSING;
			default -> DEFAULT;
		};
	}

	private WorkflowExceptionCode handleMixDownloadException(final Throwable toCheck) {
		return handleConnectionException(toCheck, START_ONLINE_MIXING_FAILED_MESSAGE);
	}

	private WorkflowExceptionCode handleDecryptException(final Throwable toCheck) {
		if (toCheck instanceof IllegalStateException && (toCheck.getMessage().startsWith(BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE)
				|| toCheck.getMessage().startsWith(BALLOT_BOX_NOT_CLOSED_MESSAGE))) {
			return BALLOT_BOX_NOT_CLOSED;
		}
		if (toCheck instanceof InvalidPayloadSignatureException && toCheck.getMessage().startsWith(PAYLOAD_SIGNATURE_IS_INVALID)) {
			return INVALID_SIGNATURE;
		}
		return DEFAULT;
	}

}
