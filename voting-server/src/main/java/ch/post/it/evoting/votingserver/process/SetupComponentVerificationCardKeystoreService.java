/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Saves the setup component verification card keystores.
 */
@Service
public class SetupComponentVerificationCardKeystoreService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationCardKeystoreService.class);

	private final ObjectMapper objectMapper;
	private final VerificationCardService verificationCardService;
	private final SetupComponentVerificationCardKeystoreRepository setupComponentVerificationCardKeystoreRepository;
	private final ElectionEventContextService electionEventContextService;
	private final ElectionEventService electionEventService;
	private final IdentifierValidationService identifierValidationService;

	public SetupComponentVerificationCardKeystoreService(
			final ObjectMapper objectMapper,
			final VerificationCardService verificationCardService,
			final SetupComponentVerificationCardKeystoreRepository setupComponentVerificationCardKeystoreRepository,
			final ElectionEventContextService electionEventContextService,
			final ElectionEventService electionEventService,
			final IdentifierValidationService identifierValidationService) {
		this.objectMapper = objectMapper;
		this.verificationCardService = verificationCardService;
		this.setupComponentVerificationCardKeystoreRepository = setupComponentVerificationCardKeystoreRepository;
		this.electionEventContextService = electionEventContextService;
		this.electionEventService = electionEventService;
		this.identifierValidationService = identifierValidationService;
	}

	/**
	 * Saves the setup component verification card keystores.
	 *
	 * @param setupComponentVerificationCardKeystoresPayload the setup component verification card keystores payload. Must be non-null.
	 * @throws NullPointerException if the setup component verification card keystores is null.
	 * @throws UncheckedIOException if an error occurs while serializing the setup component verification card keystores.
	 */
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload) {
		checkNotNull(setupComponentVerificationCardKeystoresPayload);

		final String electionEventId = setupComponentVerificationCardKeystoresPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId();
		final ImmutableList<VerificationCardKeystore> verificationCardKeystores = setupComponentVerificationCardKeystoresPayload.getVerificationCardKeystores();

		final ImmutableList<SetupComponentVerificationCardKeystoreEntity> setupComponentVerificationCardKeystoreEntities = verificationCardKeystores.stream()
				.map(verificationCardKeystore -> {
					final String verificationCardId = verificationCardKeystore.verificationCardId();
					final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(verificationCardId);

					final ImmutableByteArray serializedVerificationCardKeystore;
					try {
						serializedVerificationCardKeystore = new ImmutableByteArray(objectMapper.writeValueAsBytes(verificationCardKeystore));
					} catch (final JsonProcessingException e) {
						throw new UncheckedIOException(String.format(
								"Failed to serialize the setup component verification card keystore. [electionEventId: %s, verificationCardSetId: %s, verificationCardId: %s]",
								electionEventId, verificationCardSetId, verificationCardId), e);
					}

					return new SetupComponentVerificationCardKeystoreEntity(verificationCardEntity, serializedVerificationCardKeystore);
				})
				.collect(toImmutableList());

		setupComponentVerificationCardKeystoreRepository.saveAll(setupComponentVerificationCardKeystoreEntities);
		LOGGER.info(
				"Setup component verification card keystores successfully saved in vote verification. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}

	/**
	 * Loads the verification card keystore related to the given ids.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @param verificationCardId    the verification card id. Must be non-null and a valid UUID.
	 * @return the verification card keystore.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 * @throws IllegalStateException     if the related election event is closed.
	 */
	public VerificationCardKeystore loadVerificationCardKeystore(final String electionEventId, final String verificationCardSetId,
			final String verificationCardId) {
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		identifierValidationService.validateContextIds(contextIds);

		return loadVerificationCardKeystore(electionEventId, verificationCardSetId, verificationCardId, LocalDateTime::now);
	}

	@VisibleForTesting
	protected VerificationCardKeystore loadVerificationCardKeystore(final String electionEventId, final String verificationCardSetId,
			final String verificationCardId, final Supplier<LocalDateTime> now) {
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		identifierValidationService.validateContextIds(contextIds);

		final ElectionEventEntity electionEventEntity = electionEventService.retrieveElectionEventEntity(electionEventId);
		final ElectionEventContextEntity electionEventContext = electionEventContextService.getElectionEventContextEntity(electionEventEntity);

		final LocalDateTime electionStartTime = electionEventContext.getStartTime();
		final LocalDateTime electionEndTime = electionEventContext.getFinishTime();
		final LocalDateTime currentTime = now.get();

		final boolean afterElectionStart = currentTime.isAfter(electionStartTime) || currentTime.isEqual(electionStartTime);
		final boolean beforeElectionEnd = currentTime.isBefore(electionEndTime) || currentTime.isEqual(electionEndTime);

		checkState(afterElectionStart && beforeElectionEnd,
				"Cannot load verification card keystore outside the opened election time window. [electionEventId: %s, verificationCardSetId: %s, "
						+ "verificationCardId: %s, startTime: %s, finishTime: %s]", electionEventId, verificationCardSetId, verificationCardId,
				electionStartTime, electionEndTime);

		final SetupComponentVerificationCardKeystoreEntity entity = load(electionEventId, verificationCardId);

		try {
			return objectMapper.readValue(entity.getVerificationCardKeystore().elements(), VerificationCardKeystore.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format(
					"Failed to deserialize the setup component verification card keystore. [electionEventId: %s, verificationCardSetId: %s, verificationCardId: %s]",
					electionEventId, verificationCardSetId, verificationCardId), e);
		}
	}

	/**
	 * Loads the setup component verification card keystore entity related to the given ids.
	 *
	 * @param electionEventId    the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardId the verification card id. Must be non-null and a valid UUID.
	 * @return the setup component verification card keystore entity.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	private SetupComponentVerificationCardKeystoreEntity load(final String electionEventId, final String verificationCardId) {

		validateUUID(electionEventId);
		validateUUID(verificationCardId);

		return setupComponentVerificationCardKeystoreRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Verification card keystore not found. [verificationCardId: %s]", verificationCardId)));
	}
}
