/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;

/**
 * Service loading the chunk-wise control component code shares payloads.
 * <p>
 * For performance reasons, the GenVerDat algorithm splits the entire verification card set into smaller pieces (a process called chunking).
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class ControlComponentCodeSharesPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentCodeSharesPayloadService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;

	private final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository;

	public ControlComponentCodeSharesPayloadService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.controlComponentCodeSharesPayloadFileRepository = controlComponentCodeSharesPayloadFileRepository;
	}

	/**
	 * Retrieves all control component code shares payload's chunk paths corresponding to the given election event id and verification card set id.
	 *
	 * @param electionEventId       the payload's election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the payload's verification card set id. Must be non-null and a valid UUID.
	 * @return all control component code shares payload's chunk paths.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public ImmutableList<Path> findAllPathsOrderedByChunkId(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.debug("Loading all control component code shares payload's chunk paths. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId,
				verificationCardSetId);

		return controlComponentCodeSharesPayloadFileRepository.findAllPathsOrderedByChunkId(electionEventId, verificationCardSetId);
	}

	/**
	 * @param path the control component code shares payload chunk's path. Must be non-null.
	 * @return the chunk id in the given path.
	 */
	public int getChunkId(final Path path) {
		checkNotNull(path);
		return controlComponentCodeSharesPayloadFileRepository.getChunkId(path);
	}

	/**
	 * Loads a control component code shares payloads chunk for the given {@code path}.
	 *
	 * @param path the control component code shares payload chunk's path. Must be non-null.
	 * @return a control component code shares payloads chunk.
	 */
	public ControlComponentCodeSharesPayloadsChunk load(final Path path) {
		checkNotNull(path);

		LOGGER.debug("Loading a control component code shares payloads chunk. [path: {}]", path);

		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = controlComponentCodeSharesPayloadFileRepository.load(
				path);
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = controlComponentCodeSharesPayloads.get(0);
		final int chunkId = controlComponentCodeSharesPayload.getChunkId();
		final String electionEventId = controlComponentCodeSharesPayload.getElectionEventId();
		final String verificationCardSetId = controlComponentCodeSharesPayload.getVerificationCardSetId();
		checkState(controlComponentCodeSharesPayloads.stream().parallel().allMatch(this::verifySignature),
				"All control component code shares payloads must have a valid signature. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, chunkId);
		return new ControlComponentCodeSharesPayloadsChunk(controlComponentCodeSharesPayloads, chunkId);
	}

	private boolean verifySignature(final ControlComponentCodeSharesPayload payload) {
		final int nodeId = payload.getNodeId();
		final String electionEventId = payload.getElectionEventId();
		final String verificationCardSetId = payload.getVerificationCardSetId();
		final int chunkId = payload.getChunkId();

		final CryptoPrimitivesSignature signature = payload.getSignature();

		checkState(signature != null,
				"The signature of the control component code shares payload is null. [nodeId: %s, electionEventId: %s, verificationCardSetId: %s, chunkId: %s]",
				nodeId, electionEventId, verificationCardSetId, chunkId);

		try {
			return signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId), payload,
					ChannelSecurityContextData.controlComponentCodeShares(nodeId, electionEventId, verificationCardSetId),
					signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format(
					"Cannot verify the signature of the control component code shares payload. [nodeId: %s, electionEventId: %s, verificationCardSetId: %s, chunkId: %s]",
					nodeId, electionEventId, verificationCardSetId, chunkId), e);
		}
	}

	public record ControlComponentCodeSharesPayloadsChunk(ImmutableList<ControlComponentCodeSharesPayload> payloads, int chunkId) {
	}

}
