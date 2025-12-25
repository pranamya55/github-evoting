/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.process.Status.VCS_DOWNLOADED;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * This is an application service that manages verification card sets.
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class DownloadVerificationCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadVerificationCardSetService.class);

	private final boolean deleteControlComponentCodeShares;
	private final VerificationCardSetService verificationCardSetService;
	private final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService;
	private final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository;
	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public DownloadVerificationCardSetService(
			@Value("${sdm.process.download.delete}")
			final boolean deleteControlComponentCodeShares,
			final VerificationCardSetService verificationCardSetService,
			final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService,
			final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		this.deleteControlComponentCodeShares = deleteControlComponentCodeShares;
		this.verificationCardSetService = verificationCardSetService;
		this.downloadEncryptedLongReturnCodeSharesService = downloadEncryptedLongReturnCodeSharesService;
		this.controlComponentCodeSharesPayloadFileRepository = controlComponentCodeSharesPayloadFileRepository;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	/**
	 * Download the computed values for a verification card set.
	 */
	public void download(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.info("Downloading the computed values. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		if (deleteControlComponentCodeShares) {
			try {
				controlComponentCodeSharesPayloadFileRepository.delete(electionEventId, verificationCardSetId);
			} catch (final IOException e) {
				throw new UncheckedIOException(
						String.format(
								"Failed to delete the ControlComponentCodeShares payloads before verification cards download. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId), e);
			}
		}

		final int chunkCount = setupComponentVerificationDataPayloadFileRepository.getCount(electionEventId, verificationCardSetId);

		checkState(chunkCount > 0, "No chunk found for download. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
				verificationCardSetId);

		downloadEncryptedLongReturnCodeSharesService.download(electionEventId, verificationCardSetId, chunkCount)
				.doOnNext(controlComponentCodeSharesPayloads -> {
					final int chunkId = controlComponentCodeSharesPayloads.get(0).getChunkId();
					controlComponentCodeSharesPayloadFileRepository.save(electionEventId, verificationCardSetId, chunkId,
							controlComponentCodeSharesPayloads);
					LOGGER.debug("ControlComponentCodeShares payloads saved. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
							electionEventId,
							verificationCardSetId, chunkId);
				})
				.sequential()
				.doOnComplete(() -> {
					LOGGER.debug("Download of all chunks completed. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId);

					verificationCardSetService.updateStatus(verificationCardSetId, VCS_DOWNLOADED);

					LOGGER.debug("Verification card set updated. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId);
				})
				.blockLast();
	}
}
