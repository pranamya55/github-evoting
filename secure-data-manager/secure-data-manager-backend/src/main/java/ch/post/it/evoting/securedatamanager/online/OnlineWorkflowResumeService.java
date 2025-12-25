/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.securedatamanager.online.process.compute.ComputeService;
import ch.post.it.evoting.securedatamanager.online.process.download.DownloadService;
import ch.post.it.evoting.securedatamanager.online.process.mixdownload.MixDownloadService;
import ch.post.it.evoting.securedatamanager.online.process.requestcckeys.RequestCcKeysService;
import ch.post.it.evoting.securedatamanager.online.process.upload.UploadService;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class OnlineWorkflowResumeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OnlineWorkflowResumeService.class);

	private final UploadService uploadService;
	private final ComputeService computeService;
	private final DownloadService downloadService;
	private final WorkflowService workflowService;
	private final BallotBoxService ballotBoxService;
	private final MixDownloadService mixDownloadService;
	private final ElectionEventService electionEventService;
	private final RequestCcKeysService requestCcKeysService;

	public OnlineWorkflowResumeService(
			final UploadService uploadService,
			final ComputeService computeService,
			final DownloadService downloadService,
			final WorkflowService workflowService,
			final BallotBoxService ballotBoxService,
			final MixDownloadService mixDownloadService,
			final ElectionEventService electionEventService,
			final RequestCcKeysService requestCcKeysService) {
		this.uploadService = uploadService;
		this.computeService = computeService;
		this.downloadService = downloadService;
		this.workflowService = workflowService;
		this.ballotBoxService = ballotBoxService;
		this.mixDownloadService = mixDownloadService;
		this.electionEventService = electionEventService;
		this.requestCcKeysService = requestCcKeysService;
	}

	@SuppressWarnings("java:S3626") // Having a redundant return statement is the desired pattern in this case.
	public void resumeWorkflow() {
		final String electionEventId = electionEventService.findElectionEventId();

		if (workflowService.isStepInProgress(WorkflowStep.REQUEST_CC_KEYS)) {
			LOGGER.info("Resume the request of the CC keys step. [electionEventId: {}]", electionEventId);
			requestCcKeysService.requestCcKeys(electionEventId);
			return;
		}

		// If the compute step is in progress, resume the computation.
		if (workflowService.isStepInProgress(WorkflowStep.COMPUTE)) {
			LOGGER.info("Resume the computation step. [electionEventId: {}]", electionEventId);
			computeService.compute(electionEventId);
			return;
		}

		// If the download step is in progress, resume the download.
		if (workflowService.isStepInProgress(WorkflowStep.DOWNLOAD)) {
			LOGGER.info("Resume the computation step. [electionEventId: {}]", electionEventId);
			downloadService.download(electionEventId);
			return;
		}

		// If the upload configuration step 1 is in progress, resume the upload.
		if (workflowService.isStepInProgress(WorkflowStep.UPLOAD_CONFIGURATION_1)) {
			LOGGER.info("Resume the upload configuration step 1. [electionEventId: {}]", electionEventId);
			uploadService.upload(electionEventId, WorkflowStep.UPLOAD_CONFIGURATION_1);
			return;
		}

		// If the upload configuration step 2 is in progress, resume the upload.
		if (workflowService.isStepInProgress(WorkflowStep.UPLOAD_CONFIGURATION_2)) {
			LOGGER.info("Resume the upload configuration step 2. [electionEventId: {}]", electionEventId);
			uploadService.upload(electionEventId, WorkflowStep.UPLOAD_CONFIGURATION_2);
			return;
		}

		// If the mix_download step is in progress, resume for the ballot boxes in progress.
		if (workflowService.isStepInProgress(WorkflowStep.MIX_DOWNLOAD)) {
			final ImmutableList<String> ballotBoxIds = ballotBoxService.getBallotBoxesIdByStatus(electionEventId,
					ImmutableList.of(BallotBoxStatus.MIXING, BallotBoxStatus.MIXED, BallotBoxStatus.MIXING_ERROR));
			final String ballotBoxIdsString = String.join(", ", ballotBoxIds);
			LOGGER.info("Resume the mix download step. [electionEventId: {}, ballotBoxIds: {}]", electionEventId, ballotBoxIdsString);
			mixDownloadService.mixAndDownload(electionEventId, ballotBoxIds);
			return;
		}
	}
}
