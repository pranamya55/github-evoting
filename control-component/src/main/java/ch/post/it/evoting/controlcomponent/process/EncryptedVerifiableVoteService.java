/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;

@Service
public class EncryptedVerifiableVoteService {

	private static final String GROUP = "group";

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final VerificationCardService verificationCardService;
	private final EncryptedVerifiableVoteRepository encryptedVerifiableVoteRepository;

	public EncryptedVerifiableVoteService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final EncryptedVerifiableVoteRepository encryptedVerifiableVoteRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.verificationCardService = verificationCardService;
		this.encryptedVerifiableVoteRepository = encryptedVerifiableVoteRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final EncryptedVerifiableVote encryptedVerifiableVote) {
		checkNotNull(encryptedVerifiableVote);

		final ContextIds contextIds = encryptedVerifiableVote.contextIds();

		final ImmutableByteArray serializedEncodeEncryptedVote;
		final ImmutableByteArray serializedExponentiatedEncryptedVote;
		final ImmutableByteArray serializedEncryptedPartialChoiceReturnCodes;
		final ImmutableByteArray serializedExponentiationProof;
		final ImmutableByteArray serializedPlaintextEqualityProof;
		final ImmutableByteArray serializedContextIds;
		try {
			serializedContextIds = new ImmutableByteArray(objectMapper.writeValueAsBytes(contextIds));
			serializedEncodeEncryptedVote = new ImmutableByteArray(objectMapper.writeValueAsBytes(encryptedVerifiableVote.encryptedVote()));
			serializedExponentiatedEncryptedVote = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(encryptedVerifiableVote.exponentiatedEncryptedVote()));
			serializedEncryptedPartialChoiceReturnCodes = new ImmutableByteArray(objectMapper.writeValueAsBytes(
					encryptedVerifiableVote.encryptedPartialChoiceReturnCodes()));
			serializedExponentiationProof = new ImmutableByteArray(objectMapper.writeValueAsBytes(encryptedVerifiableVote.exponentiationProof()));
			serializedPlaintextEqualityProof = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(encryptedVerifiableVote.plaintextEqualityProof()));

		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize encrypted verifiable vote.", e);
		}

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(contextIds.verificationCardId());

		final EncryptedVerifiableVoteEntity encryptedVerifiableVoteEntity = new EncryptedVerifiableVoteEntity.Builder()
				.setContextIds(serializedContextIds)
				.setEncryptedVote(serializedEncodeEncryptedVote)
				.setExponentiatedEncryptedVote(serializedExponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(serializedEncryptedPartialChoiceReturnCodes)
				.setExponentiationProof(serializedExponentiationProof)
				.setPlaintextEqualityProof(serializedPlaintextEqualityProof)
				.setVerificationCardEntity(verificationCardEntity)
				.build();
		encryptedVerifiableVoteRepository.save(encryptedVerifiableVoteEntity);
	}

	@Transactional // Required due to the lazy loading of entities.
	public EncryptedVerifiableVote getEncryptedVerifiableVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		final EncryptedVerifiableVoteEntity encryptedVerifiableVoteEntity = encryptedVerifiableVoteRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Encrypted verifiable vote not found. [verificationCardId: %s]", verificationCardId)));

		final VerificationCardSetEntity verificationCardSetEntity = encryptedVerifiableVoteEntity.getVerificationCardEntity()
				.getVerificationCardSetEntity();
		final String electionEventId = verificationCardSetEntity.getElectionEventEntity().getElectionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		return deserializeEncryptedVerifiableVote(encryptedVerifiableVoteEntity, encryptionGroup);
	}

	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<EncryptedVerifiableVote> getConfirmedVotes(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		final ImmutableList<EncryptedVerifiableVoteEntity> confirmedVoteEntities = ImmutableList.from(
				encryptedVerifiableVoteRepository.findAllConfirmedByVerificationCardSetId(verificationCardSetId));

		if (confirmedVoteEntities.isEmpty()) {
			return ImmutableList.emptyList();
		}

		final VerificationCardSetEntity verificationCardSetEntity = confirmedVoteEntities.get(0).getVerificationCardEntity()
				.getVerificationCardSetEntity();
		final String electionEventId = verificationCardSetEntity.getElectionEventEntity().getElectionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		return confirmedVoteEntities.stream()
				.map(encryptedVerifiableVoteEntity -> deserializeEncryptedVerifiableVote(encryptedVerifiableVoteEntity, encryptionGroup))
				.collect(toImmutableList());
	}

	public ImmutableList<EncryptedVerifiableVote> getSentVotes(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		return encryptedVerifiableVoteRepository.findAllSentByElectionEventId(electionEventId).stream()
				.map(encryptedVerifiableVoteEntity -> deserializeEncryptedVerifiableVote(encryptedVerifiableVoteEntity, encryptionGroup))
				.collect(toImmutableList());
	}

	private EncryptedVerifiableVote deserializeEncryptedVerifiableVote(final EncryptedVerifiableVoteEntity encryptedVerifiableVoteEntity,
			final GqGroup encryptionGroup) {
		final ObjectReader reader = objectMapper.reader().withAttribute(GROUP, encryptionGroup);

		final ContextIds contextIds;
		final ElGamalMultiRecipientCiphertext encryptedVote;
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
		final ExponentiationProof exponentiationProof;
		final PlaintextEqualityProof plaintextEqualityProof;
		try {
			contextIds = reader.readValue(encryptedVerifiableVoteEntity.getContextIds().elements(), ContextIds.class);
			encryptedVote = reader.readValue(encryptedVerifiableVoteEntity.getEncryptedVote().elements(), ElGamalMultiRecipientCiphertext.class);
			exponentiatedEncryptedVote = reader.readValue(encryptedVerifiableVoteEntity.getExponentiatedEncryptedVote().elements(),
					ElGamalMultiRecipientCiphertext.class);
			encryptedPartialChoiceReturnCodes = reader.readValue(encryptedVerifiableVoteEntity.getEncryptedPartialChoiceReturnCodes().elements(),
					ElGamalMultiRecipientCiphertext.class);
			exponentiationProof = reader.readValue(encryptedVerifiableVoteEntity.getExponentiationProof().elements(), ExponentiationProof.class);
			plaintextEqualityProof = reader.readValue(encryptedVerifiableVoteEntity.getPlaintextEqualityProof().elements(),
					PlaintextEqualityProof.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize encrypted verifiable vote.", e);
		}

		return new EncryptedVerifiableVote(contextIds, encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes,
				exponentiationProof, plaintextEqualityProof);
	}

}
