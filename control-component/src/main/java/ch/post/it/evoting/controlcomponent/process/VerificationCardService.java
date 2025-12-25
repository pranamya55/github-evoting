/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

@Service
public class VerificationCardService {

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final VerificationCardSetService verificationCardSetService;
	private final VerificationCardRepository verificationCardRepository;

	public VerificationCardService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final VerificationCardSetService verificationCardSetService,
			final VerificationCardRepository verificationCardRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.verificationCardSetService = verificationCardSetService;
		this.verificationCardRepository = verificationCardRepository;
	}

	@VisibleForTesting
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public VerificationCardEntity save(final VerificationCard verificationCard) {
		checkNotNull(verificationCard);

		final VerificationCardEntity verificationCardEntity = verificationCardToEntity(verificationCard);

		return verificationCardRepository.save(verificationCardEntity);
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void saveAll(final ImmutableList<VerificationCard> verificationCards) {
		checkNotNull(verificationCards);

		final ImmutableList<VerificationCardEntity> verificationCardEntities = verificationCards.stream()
				.map(this::verificationCardToEntity)
				.collect(toImmutableList());

		verificationCardRepository.saveAll(verificationCardEntities);
	}

	public boolean existsNone(final ImmutableList<String> verificationCardIds) {
		final ImmutableSet<String> ids = checkNotNull(verificationCardIds).stream()
				.map(Validations::validateUUID)
				.collect(toImmutableSet());

		return !verificationCardRepository.existsByVerificationCardIdIn(ids.asSet());
	}

	public int countNumberOfVerificationCards(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return verificationCardRepository.countByElectionEventIdAndVerificationCardSetId(electionEventId, verificationCardSetId);
	}

	@Transactional // Required due to the lazy loading of entities.
	public VerificationCard getVerificationCard(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardEntity verificationCardEntity = verificationCardRepository.findById(verificationCardId)
				.orElseThrow(
						() -> new IllegalStateException(String.format("Verification card not found. [verificationCardId: %s]", verificationCardId)));

		final String electionEventId = verificationCardEntity.getVerificationCardSetEntity().getElectionEventEntity().getElectionEventId();
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final ElGamalMultiRecipientPublicKey verificationCardPublicKey;
		try {
			verificationCardPublicKey = objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(verificationCardEntity.getVerificationCardPublicKey().elements(), ElGamalMultiRecipientPublicKey.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize verification card. [verificationCardId: %s]", verificationCardId), e);
		}

		final String verificationCardSetId = verificationCardEntity.getVerificationCardSetEntity().getVerificationCardSetId();

		return new VerificationCard(verificationCardEntity.getVerificationCardId(), verificationCardSetId, verificationCardPublicKey);
	}

	public VerificationCardEntity getVerificationCardEntity(final String verificationCardId) {
		validateUUID(verificationCardId);

		return verificationCardRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException("No corresponding verificationCard found. [verificationCardId: %s]"));
	}

	private VerificationCardEntity verificationCardToEntity(final VerificationCard verificationCard) {
		final String verificationCardId = verificationCard.verificationCardId();
		final String verificationCardSetId = verificationCard.verificationCardSetId();

		final ImmutableByteArray publicKeyBytes;
		try {
			publicKeyBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(verificationCard.verificationCardPublicKey()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize verification card public key. [verificationCardId: %s]", verificationCardId), e);
		}

		// Retrieve verification card set associated to this verification card.
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);

		final VerificationCardEntity verificationCardEntity = new VerificationCardEntity(verificationCardId, verificationCardSetEntity,
				publicKeyBytes);
		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity();

		// One to one bidirectional mapping.
		verificationCardStateEntity.setVerificationCardEntity(verificationCardEntity);
		verificationCardEntity.setVerificationCardStateEntity(verificationCardStateEntity);

		return verificationCardEntity;
	}
}
