/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Saves and retrieves the partially decrypted encrypted PCCs of all control components.
 */
@Service
public class CombinedPartiallyDecryptedPCCService {

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final VerificationCardService verificationCardService;
	private final CombinedPartiallyDecryptedPCCRepository combinedPartiallyDecryptedPCCRepository;

	public CombinedPartiallyDecryptedPCCService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final CombinedPartiallyDecryptedPCCRepository combinedPartiallyDecryptedPCCRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.verificationCardService = verificationCardService;
		this.combinedPartiallyDecryptedPCCRepository = combinedPartiallyDecryptedPCCRepository;
	}

	/**
	 * Saves the list of control component partial decrypt payloads.
	 *
	 * @param verificationCardId                     the verification card id. Must be a valid UUID.
	 * @param controlComponentPartialDecryptPayloads the partially decrypted PCCs of all control components.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 * @throws NullPointerException      if {@code controlComponentPartialDecryptPayloads} is null.
	 * @throws IllegalArgumentException  if {@code controlComponentPartialDecryptPayloads} does not contain the expected number of partial decrypt
	 *                                   payloads.
	 */
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final String verificationCardId,
			final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) {

		validateUUID(verificationCardId);
		checkNotNull(controlComponentPartialDecryptPayloads);
		checkArgument(controlComponentPartialDecryptPayloads.size() == ControlComponentNode.ids().size(),
				"There must be %s partial decrypt payloads.", ControlComponentNode.ids().size());

		final ImmutableList<PartiallyDecryptedEncryptedPCC> combinedPartiallyDecryptedPCC = controlComponentPartialDecryptPayloads.stream()
				.map(ControlComponentPartialDecryptPayload::getPartiallyDecryptedEncryptedPCC)
				.collect(toImmutableList());

		final ImmutableByteArray serializedCombinedPartiallyDecryptedPCC;
		try {
			serializedCombinedPartiallyDecryptedPCC = new ImmutableByteArray(objectMapper.writeValueAsBytes(combinedPartiallyDecryptedPCC));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize the combined partially decrypted PCC.", e);
		}

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(verificationCardId);
		final CombinedPartiallyDecryptedPCCEntity combinedPartiallyDecryptedPCCEntity = new CombinedPartiallyDecryptedPCCEntity(
				verificationCardEntity, serializedCombinedPartiallyDecryptedPCC);

		combinedPartiallyDecryptedPCCRepository.save(combinedPartiallyDecryptedPCCEntity);
	}

	/**
	 * Gets the partially decrypted PCCs of all control components for the given verification card.
	 *
	 * @param verificationCardId the verification card id. Must be a valid UUID.
	 * @return the partially decrypted PCCs of all control components.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<PartiallyDecryptedEncryptedPCC> getCombinedPartiallyDecryptedPCC(final String verificationCardId) {
		validateUUID(verificationCardId);

		final CombinedPartiallyDecryptedPCCEntity combinedPartiallyDecryptedPCCEntity = combinedPartiallyDecryptedPCCRepository.findById(
						verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Combined partially decrypted PCC not found. [verificationCardId: %s]", verificationCardId)));

		final VerificationCardSetEntity verificationCardSetEntity = combinedPartiallyDecryptedPCCEntity.getVerificationCardEntity()
				.getVerificationCardSetEntity();
		final String electionEventId = verificationCardSetEntity.getElectionEventEntity().getElectionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		return deserializeCombinedPartiallyDecryptedPCC(encryptionGroup, combinedPartiallyDecryptedPCCEntity.getCombinedPartiallyDecryptedPCC());
	}

	private ImmutableList<PartiallyDecryptedEncryptedPCC> deserializeCombinedPartiallyDecryptedPCC(final GqGroup encryptionGroup,
			final ImmutableByteArray serializedCombinedPartiallyDecryptedPCC) {

		final ObjectReader reader = objectMapper.reader().withAttribute("group", encryptionGroup);

		final ImmutableList<PartiallyDecryptedEncryptedPCC> combinedPartiallyDecryptedEncryptedPCC;
		try {
			combinedPartiallyDecryptedEncryptedPCC =
					Arrays.stream(reader.readValue(serializedCombinedPartiallyDecryptedPCC.elements(), PartiallyDecryptedEncryptedPCC[].class))
							.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize the combined partially decrypted PCC.", e);
		}

		return combinedPartiallyDecryptedEncryptedPCC;
	}

}
