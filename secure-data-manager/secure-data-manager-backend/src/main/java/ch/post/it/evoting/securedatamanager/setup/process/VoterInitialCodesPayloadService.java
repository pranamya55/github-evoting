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
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving and retrieving voter initial codes payloads.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class VoterInitialCodesPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoterInitialCodesPayloadService.class);

	private final VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepository;

	public VoterInitialCodesPayloadService(final VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepository) {
		this.voterInitialCodesPayloadFileRepository = voterInitialCodesPayloadFileRepository;
	}

	/**
	 * Saves a voter initial codes payload in the corresponding election event folder.
	 *
	 * @param voterInitialCodesPayload the voter initial codes payload to save. Must be non-null.
	 * @param verificationCardSetId    the verification card set id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the {@code verificationCardSetId} is not a valid UUID.
	 */
	public void save(final VoterInitialCodesPayload voterInitialCodesPayload, final String verificationCardSetId) {
		checkNotNull(voterInitialCodesPayload);
		validateUUID(verificationCardSetId);

		final String electionEventId = voterInitialCodesPayload.electionEventId();

		voterInitialCodesPayloadFileRepository.save(voterInitialCodesPayload, verificationCardSetId);

		LOGGER.info("Saved voter initial codes payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);
	}

	/**
	 * Loads the voter initial codes payload for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the voter initial codes payload for this {@code electionEventId} and {@code verificationCardSetId}.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if any parameter is not a valid UUID.
	 */
	public VoterInitialCodesPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final VoterInitialCodesPayload voterInitialCodesPayload = voterInitialCodesPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(
						electionEventId, verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested voter initial codes payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));

		LOGGER.info("Loaded voter initial codes payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		return voterInitialCodesPayload;
	}

	/**
	 * Loads all the voter initial codes payloads for the given {@code electionEventId} and collects them as a map by voter identification.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the voter initial codes map for this {@code electionEventId}.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the event id is not a valid UUID.
	 */
	public ImmutableMap<String, VoterInitialCodesByVcs> loadVoterInitialCodesMap(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<VoterInitialCodesPayload> voterInitialCodesPayloads = voterInitialCodesPayloadFileRepository.findAllByElectionEventId(
				electionEventId);
		checkState(!voterInitialCodesPayloads.isEmpty(), "Requested voter initial codes payloads are not present. [electionEventId: %s]",
				electionEventId);

		final ImmutableMap<String, VoterInitialCodesByVcs> voterInitialCodesMap = voterInitialCodesPayloads.stream()
				.flatMap(voterInitialCodesPayload -> voterInitialCodesPayload.voterInitialCodes().stream()
						.map(voterInitialCodes -> new VoterInitialCodesByVcs(voterInitialCodesPayload.verificationCardSetId(), voterInitialCodes)))
				.collect(toImmutableMap(voterInitialCodesByVcs -> voterInitialCodesByVcs.voterInitialCodes.voterIdentification(),
						Function.identity()));

		LOGGER.info("Loaded all voter initial codes payloads. [electionEventId: {}]", electionEventId);

		return voterInitialCodesMap;
	}

	public record VoterInitialCodesByVcs(String verificationCardSetId, VoterInitialCodes voterInitialCodes) {
	}
}
