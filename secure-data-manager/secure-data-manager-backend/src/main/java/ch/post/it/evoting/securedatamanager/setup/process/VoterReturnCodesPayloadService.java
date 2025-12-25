/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.configuration.VoterReturnCodes;
import ch.post.it.evoting.domain.configuration.VoterReturnCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows to generate and persist {@link VoterReturnCodesPayload}.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class VoterReturnCodesPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoterReturnCodesPayloadService.class);

	private final VoterReturnCodesPayloadFileRepository voterReturnCodesPayloadFileRepository;

	public VoterReturnCodesPayloadService(final VoterReturnCodesPayloadFileRepository voterReturnCodesPayloadFileRepository) {
		this.voterReturnCodesPayloadFileRepository = voterReturnCodesPayloadFileRepository;
	}

	/**
	 * Persists a {@link VoterReturnCodesPayload}.
	 *
	 * @param payload               the payload to be saved. Must be non-null.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if the {@code payload} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if the {@code verificationCardSetId} format is not valid.
	 */
	public void save(final VoterReturnCodesPayload payload, final String verificationCardSetId) {
		checkNotNull(payload);
		validateUUID(verificationCardSetId);

		voterReturnCodesPayloadFileRepository.save(payload, verificationCardSetId);
		final String electionEventId = payload.electionEventId();
		LOGGER.info("Voter return codes payload successfully saved. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	/**
	 * Loads the {@link VoterReturnCodesPayload} for the given the election event and verification card set.
	 *
	 * @param electionEventId,       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId, the verification card set id. Must be non-null and a valid UUID.
	 * @return a {@link VoterReturnCodesPayload}.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws IllegalStateException     if the requested voter return codes payload is not present.
	 */
	public VoterReturnCodesPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return voterReturnCodesPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId, verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested voter return codes payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));
	}

	/**
	 * Loads all the voter return codes payloads for the given {@code electionEventId} and collects them as a map by verification card id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the voter return codes map for this {@code electionEventId}.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the event id is not a valid UUID.
	 */
	public ImmutableMap<String, VoterReturnCodes> loadVoterReturnCodesMap(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<VoterReturnCodesPayload> voterReturnCodesPayloads = voterReturnCodesPayloadFileRepository.findAllByElectionEventId(
				electionEventId);
		checkState(!voterReturnCodesPayloads.isEmpty(), "Requested voter return codes payload is not present. [electionEventId: %s]",
				electionEventId);

		final ImmutableMap<String, VoterReturnCodes> voterReturnCodesMap = voterReturnCodesPayloads.stream()
				.map(VoterReturnCodesPayload::voterReturnCodes)
				.flatMap(ImmutableList::stream)
				.collect(toImmutableMap(VoterReturnCodes::verificationCardId, Function.identity()));

		LOGGER.info("Loaded all voter return codes payloads. [electionEventId: {}]", electionEventId);

		return voterReturnCodesMap;
	}

}
