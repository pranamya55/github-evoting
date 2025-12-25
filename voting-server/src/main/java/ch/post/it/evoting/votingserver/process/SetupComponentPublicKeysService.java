/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentPublicKeysResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class SetupComponentPublicKeysService {

	private static final String GROUP = "group";
	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentPublicKeysService.class);

	private final Serializer serializer;
	private final ObjectMapper objectMapper;
	private final MessageHandler messageHandler;
	private final ElectionEventService electionEventService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ResponseCompletionService responseCompletionService;
	private final SetupComponentPublicKeysRepository setupComponentPublicKeysRepository;

	public SetupComponentPublicKeysService(
			final Serializer serializer,
			final ObjectMapper objectMapper,
			final MessageHandler messageHandler,
			final ElectionEventService electionEventService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ResponseCompletionService responseCompletionService,
			final SetupComponentPublicKeysRepository setupComponentPublicKeysRepository) {
		this.serializer = serializer;
		this.objectMapper = objectMapper;
		this.messageHandler = messageHandler;
		this.electionEventService = electionEventService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.responseCompletionService = responseCompletionService;
		this.setupComponentPublicKeysRepository = setupComponentPublicKeysRepository;
	}

	/**
	 * Saves the setup component public keys.
	 *
	 * @param setupComponentPublicKeysPayload the request payload. Must be non null.
	 * @throws NullPointerException if {@code setupComponentPublicKeysPayload} is null.
	 */
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);
		verifyPayloadSignature(setupComponentPublicKeysPayload);

		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		setupComponentPublicKeysRepository.save(
				buildSetupComponentPublicKeysEntity(electionEventId, setupComponentPublicKeysPayload.getSetupComponentPublicKeys()));

		LOGGER.info("Setup component public keys successfully saved. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Sends the setup component public keys to the Control Components.
	 *
	 * @param setupComponentPublicKeysPayload the request payload. Must be non null.
	 * @return the correlation id of the request.
	 * @throws NullPointerException if {@code setupComponentPublicKeysPayload} is null.
	 */
	public String onRequest(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		final String correlationId = messageHandler.sendMessage(setupComponentPublicKeysPayload);

		LOGGER.info("Setup component public keys sent to the control components. [electionEventId: {}, correlationId: {}]",
				setupComponentPublicKeysPayload.getElectionEventId(), correlationId);

		return correlationId;
	}

	/**
	 * Waits for the response from the Control Components to ensure the setup component public keys have been successfully processed.
	 *
	 * @param correlationId the correlation id on which to wait. Must be non-null.
	 * @throws NullPointerException if {@code correlationId} is null.
	 */
	public void waitForResponse(final String correlationId) {
		checkNotNull(correlationId);

		final ResponseCompletionCompletableFuture<ImmutableList<SetupComponentPublicKeysResponsePayload>> completableFuture = responseCompletionService.registerForResponseCompletion(
				correlationId, new TypeReference<>() {});

		completableFuture.get();
	}

	/**
	 * Defines the behavior when the response from the Control Components is received.
	 *
	 * @param correlationId                            the correlation id of the request, to unblock it. Must be non-null.
	 * @param setupComponentPublicKeysResponsePayloads the response from the Control Components. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the size of the list of setup component public keys is not the size of {@link ControlComponentNode#ids}.
	 */
	public void onResponse(final String correlationId,
			final ImmutableList<SetupComponentPublicKeysResponsePayload> setupComponentPublicKeysResponsePayloads) {
		checkNotNull(correlationId);
		checkNotNull(setupComponentPublicKeysResponsePayloads);

		checkArgument(setupComponentPublicKeysResponsePayloads.size() == ControlComponentNode.ids().size());

		responseCompletionService.notifyResponseCompleted(correlationId, setupComponentPublicKeysResponsePayloads);

		final String electionEventId = setupComponentPublicKeysResponsePayloads.get(0).electionEventId();
		LOGGER.info("Setup component public keys successfully uploaded to the control components. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Recovers the voting client public keys from the setup component public keys entity using the given election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the voting client public keys.
	 * @throws FailedValidationException if {@code electionEventId} is not valid.
	 */
	public VotingClientPublicKeys getVotingClientPublicKeys(final String electionEventId) {
		validateUUID(electionEventId);

		// Retrieve the setup component public keys.
		final ElectionEventEntity electionEventEntity = electionEventService.retrieveElectionEventEntity(electionEventId);
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = setupComponentPublicKeysRepository
				.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Setup component public keys not found. [electionEventId: %s]", electionEventId)));
		LOGGER.info("Setup component public keys found. [electionEventId: {}]", electionEventId);

		// Retrieve group.
		final GqGroup encryptionGroup = electionEventEntity.getEncryptionGroup();

		// Deserialize the voting client public keys.
		final ElGamalMultiRecipientPublicKey electionPublicKey;
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;
		try {
			electionPublicKey = objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(setupComponentPublicKeysEntity.getElectionPublicKey().elements(), ElGamalMultiRecipientPublicKey.class);
			choiceReturnCodesEncryptionPublicKey = objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(setupComponentPublicKeysEntity.getChoiceReturnCodesEncryptionPublicKey().elements(),
							ElGamalMultiRecipientPublicKey.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize the voting client public keys. [electionEventId: %s]",
					setupComponentPublicKeysEntity.getElectionEventEntity().getElectionEventId()), e);
		}

		LOGGER.info("Voting client public keys recovered. [electionEventId: {}]", electionEventId);
		return new VotingClientPublicKeys(encryptionGroup, electionPublicKey, choiceReturnCodesEncryptionPublicKey);
	}

	/**
	 * Defines how to extract the node id from the response payload.
	 *
	 * @param setupComponentPublicKeysResponsePayload the payload from which to extract the node id. Must be non-null.
	 * @return the extracted node id.
	 * @throws NullPointerException if {@code setupComponentPublicKeysResponsePayload} is null.
	 */
	public int extractNodeId(final SetupComponentPublicKeysResponsePayload setupComponentPublicKeysResponsePayload) {
		checkNotNull(setupComponentPublicKeysResponsePayload);

		return setupComponentPublicKeysResponsePayload.nodeId();
	}

	/**
	 * Defines how to deserialize the response payload.
	 *
	 * @param messageBytes the bytes to deserialize. Must be non-null.
	 * @return the deserialized payload.
	 * @throws NullPointerException if {@code messageBytes} is null.
	 */
	public SetupComponentPublicKeysResponsePayload deserialize(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);

		return serializer.deserialize(messageBytes, SetupComponentPublicKeysResponsePayload.class);
	}

	/**
	 * Verifies the signature of the election event context payload.
	 *
	 * @param setupComponentPublicKeysPayload the setup component public keys payload to verify. Must be non-null.
	 * @throws NullPointerException             if {@code setupComponentPublicKeysPayload} is null.
	 * @throws IllegalStateException            if an error occurred while verifying the signature of the setup component public keys payload.
	 * @throws InvalidPayloadSignatureException if the signature of the setup component public keys payload is invalid.
	 */
	public void verifyPayloadSignature(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		final CryptoPrimitivesSignature signature = setupComponentPublicKeysPayload.getSignature();

		checkState(signature != null, "The signature of the setup component public keys payload is null. [electionEventId: %s]", electionEventId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentPublicKeys(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentPublicKeysPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the setup component public keys. [electionEventId: %s]", electionEventId));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentPublicKeysPayload.class,
					String.format("[electionEventId: %s]", electionEventId));
		}
	}

	private SetupComponentPublicKeysEntity buildSetupComponentPublicKeysEntity(final String electionEventId,
			final SetupComponentPublicKeys setupComponentPublicKeys) {

		final ImmutableByteArray serializedCombinedControlComponentPublicKeys;
		final ImmutableByteArray serializedElectoralBoardPublicKey;
		final ImmutableByteArray serializedElectoralBoardSchnorrProofs;
		final ImmutableByteArray serializedElectionPublicKey;
		final ImmutableByteArray serializedChoiceReturnCodesPublicKey;
		final ElectionEventEntity electionEventEntity;
		try {
			serializedCombinedControlComponentPublicKeys = new ImmutableByteArray(objectMapper.writeValueAsBytes(
					setupComponentPublicKeys.combinedControlComponentPublicKeys()));
			serializedElectoralBoardPublicKey = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(setupComponentPublicKeys.electoralBoardPublicKey()));
			serializedElectoralBoardSchnorrProofs = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(setupComponentPublicKeys.electoralBoardSchnorrProofs()));
			serializedElectionPublicKey = new ImmutableByteArray(objectMapper.writeValueAsBytes(setupComponentPublicKeys.electionPublicKey()));
			serializedChoiceReturnCodesPublicKey = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey()));
			electionEventEntity = electionEventService.retrieveElectionEventEntity(electionEventId);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize setup component public keys.", e);
		}

		return new SetupComponentPublicKeysEntity(electionEventEntity, serializedCombinedControlComponentPublicKeys,
				serializedElectoralBoardPublicKey, serializedElectoralBoardSchnorrProofs, serializedElectionPublicKey,
				serializedChoiceReturnCodesPublicKey);
	}

}
