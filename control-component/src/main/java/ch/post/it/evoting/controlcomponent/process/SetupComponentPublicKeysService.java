/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;

@Service
public class SetupComponentPublicKeysService {

	private static final String GROUP = "group";

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final SetupComponentPublicKeysRepository setupComponentPublicKeysRepository;

	public SetupComponentPublicKeysService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final SetupComponentPublicKeysRepository setupComponentPublicKeysRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.setupComponentPublicKeysRepository = setupComponentPublicKeysRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final String electionEventId, final SetupComponentPublicKeys setupComponentPublicKeys) {
		validateUUID(electionEventId);
		checkNotNull(setupComponentPublicKeys);

		// Save setup component public keys entity.
		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);

		final ImmutableByteArray serializedCombinedControlComponentPublicKeys;
		final ImmutableByteArray serializedElectoralBoardPublicKey;
		final ImmutableByteArray serializedElectoralBoardSchnorrProofs;
		final ImmutableByteArray serializedElectionPublicKey;
		final ImmutableByteArray serializedChoiceReturnCodesPublicKey;
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

		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize setup component public keys.", e);
		}

		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = new SetupComponentPublicKeysEntity.Builder()
				.setElectionEventEntity(electionEventEntity)
				.setCombinedControlComponentPublicKey(serializedCombinedControlComponentPublicKeys)
				.setElectoralBoardPublicKey(serializedElectoralBoardPublicKey)
				.setElectoralBoardSchnorrProofs(serializedElectoralBoardSchnorrProofs)
				.setElectionPublicKey(serializedElectionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(serializedChoiceReturnCodesPublicKey)
				.build();
		setupComponentPublicKeysRepository.save(setupComponentPublicKeysEntity);
	}

	public GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> getCcmElectionPublicKeys(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys = getCombinedControlComponentPublicKeys(electionEventId);
		return controlComponentPublicKeys.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
	}

	public ImmutableList<ControlComponentPublicKeys> getCombinedControlComponentPublicKeys(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = getSetupComponentPublicKeysEntity(electionEventId);
		final ImmutableByteArray controlComponentsPublicKeysBytes = setupComponentPublicKeysEntity.getCombinedControlComponentPublicKeys();
		final ControlComponentPublicKeys[] controlComponentsPublicKeys;
		try {
			controlComponentsPublicKeys = objectMapper
					.reader().withAttribute(GROUP, encryptionGroup)
					.readValue(controlComponentsPublicKeysBytes.elements(), ControlComponentPublicKeys[].class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize combined control component public keys. [electionEventId: %s]", electionEventId), e);
		}
		return ImmutableList.of(controlComponentsPublicKeys);
	}

	public ElGamalMultiRecipientPublicKey getElectoralBoardPublicKey(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = getSetupComponentPublicKeysEntity(electionEventId);
		final ImmutableByteArray electoralBoardPublicKeyBytes = setupComponentPublicKeysEntity.getElectoralBoardPublicKey();
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey;
		try {
			electoralBoardPublicKey = objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(electoralBoardPublicKeyBytes.elements(), ElGamalMultiRecipientPublicKey.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize electoral board public key. [electionEventId: %s]", electionEventId),
					e);
		}
		return electoralBoardPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getElectionPublicKey(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = getSetupComponentPublicKeysEntity(electionEventId);
		final ImmutableByteArray electionPublicKeyBytes = setupComponentPublicKeysEntity.getElectionPublicKey();
		final ElGamalMultiRecipientPublicKey electionPublicKey;
		try {
			electionPublicKey = objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(electionPublicKeyBytes.elements(), ElGamalMultiRecipientPublicKey.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize election public key. [electionEventId: %s]", electionEventId), e);
		}
		return electionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getChoiceReturnCodesEncryptionPublicKey(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = getSetupComponentPublicKeysEntity(electionEventId);
		final ImmutableByteArray choiceReturnCodesEncryptionPublicKeyBytes = setupComponentPublicKeysEntity.getChoiceReturnCodesEncryptionPublicKey();
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;
		try {
			choiceReturnCodesEncryptionPublicKey = objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(choiceReturnCodesEncryptionPublicKeyBytes.elements(), ElGamalMultiRecipientPublicKey.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize choice return codes encryption public key. [electionEventId: %s]", electionEventId), e);
		}
		return choiceReturnCodesEncryptionPublicKey;
	}

	SetupComponentPublicKeysEntity getSetupComponentPublicKeysEntity(final String electionEventId) {
		validateUUID(electionEventId);

		final Optional<SetupComponentPublicKeysEntity> setupComponentPublicKeysEntity = setupComponentPublicKeysRepository.findById(electionEventId);

		return setupComponentPublicKeysEntity.orElseThrow(
				() -> new IllegalStateException(
						String.format("Setup component public keys entity not found. [electionEventId: %s]", electionEventId)));
	}
}
