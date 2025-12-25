/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows retrieving existing control component public keys.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class ControlComponentPublicKeysConfigService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentPublicKeysConfigService.class);
	private final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository;
	private final SignatureKeystore<Alias> signatureKeystoreService;

	public ControlComponentPublicKeysConfigService(
			final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository,
			final SignatureKeystore<Alias> signatureKeystoreService) {
		this.controlComponentPublicKeysPayloadFileRepository = controlComponentPublicKeysPayloadFileRepository;
		this.signatureKeystoreService = signatureKeystoreService;
	}

	/**
	 * Loads all {@link ControlComponentPublicKeys} for the given {@code electionEventId}. Upon retrieving the public keys, the signatures of their
	 * respective payloads are first verified.
	 *
	 * @param electionEventId the election event id for which to get the public keys.
	 * @return the control component public keys for this {@code electionEventId}.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws IllegalStateException     if
	 *                                   <ul>
	 *                                       <li>There is not the correct number of payloads.</li>
	 *                                       <li>If any verification of signature fails.</li>
	 *                                       <li>If any signature is invalid.</li>
	 *                                   </ul>
	 */
	public ImmutableList<ControlComponentPublicKeys> loadOrderByNodeId(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads =
				controlComponentPublicKeysPayloadFileRepository.findAllOrderByNodeId(electionEventId);

		LOGGER.debug("Retrieved all control component public keys payloads are valid. [electionEventId: {}]", electionEventId);

		// Ensure we received all payloads corresponds to the node ids.
		final ImmutableList<Integer> payloadsNodeIds = controlComponentPublicKeysPayloads.stream().parallel()
				.map(ControlComponentPublicKeysPayload::getControlComponentPublicKeys)
				.map(ControlComponentPublicKeys::nodeId)
				.collect(toImmutableList());

		checkState(ControlComponentNode.ids().size() == payloadsNodeIds.size() && payloadsNodeIds.containsAll(ControlComponentNode.ids()),
				"Wrong number of control component public keys payloads. [required node ids: %s, found: %s]", ControlComponentNode.ids(),
				payloadsNodeIds);

		// Check signatures of payloads.
		controlComponentPublicKeysPayloads
				.forEach(controlComponentPublicKeysPayload -> validateSignature(controlComponentPublicKeysPayload, electionEventId));

		LOGGER.debug("Signature of all control component public keys payloads are valid. [electionEventId: {}]", electionEventId);

		return controlComponentPublicKeysPayloads.stream().parallel()
				.map(ControlComponentPublicKeysPayload::getControlComponentPublicKeys)
				.sorted(Comparator.comparingInt(ControlComponentPublicKeys::nodeId))
				.collect(toImmutableList());
	}

	private void validateSignature(final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload, final String electionEventId) {

		final CryptoPrimitivesSignature signature = controlComponentPublicKeysPayload.getSignature();
		final int nodeId = controlComponentPublicKeysPayload.getControlComponentPublicKeys().nodeId();

		checkState(signature != null, "The signature of the control component public keys payload is null. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentPublicKeys(nodeId, electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId),
					controlComponentPublicKeysPayload, additionalContextData, signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException("Cannot verify the signature.", e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ControlComponentPublicKeysPayload.class,
					String.format("[electionEventId: %s, nodeId: %s]", electionEventId, nodeId));
		}
	}
}
