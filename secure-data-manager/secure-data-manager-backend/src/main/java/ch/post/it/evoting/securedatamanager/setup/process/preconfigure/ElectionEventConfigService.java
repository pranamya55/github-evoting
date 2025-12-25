/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.securedatamanager.setup.process.SetupKeyPairService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenSetupDataOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenSetupDataService;

/**
 * This is a service for handling election event entities.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class ElectionEventConfigService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventConfigService.class);

	private final String electionEventSeed;
	private final GenSetupDataService genSetupDataService;
	private final SetupKeyPairService setupKeyPairService;
	private final ElectionEventContextPersistenceService electionEventContextPersistenceService;

	public ElectionEventConfigService(
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed,
			final GenSetupDataService genSetupDataService,
			final SetupKeyPairService setupKeyPairService,
			final ElectionEventContextPersistenceService electionEventContextPersistenceService) {
		this.electionEventSeed = validateSeed(electionEventSeed);
		this.genSetupDataService = genSetupDataService;
		this.setupKeyPairService = setupKeyPairService;
		this.electionEventContextPersistenceService = electionEventContextPersistenceService;
	}

	/**
	 * Creates an election event based on the given id and if everything ok, it sets its status to ready.
	 *
	 * @param electionEventId identifies the election event to be created. Must be non-null and a valid UUID.
	 */
	public void create(final String electionEventId) {
		validateUUID(electionEventId);

		// Generate setup data.
		final GenSetupDataOutput genSetupDataOutput = genSetupDataService.genSetupData(electionEventId, electionEventSeed);
		LOGGER.info("Successfully generated the setup data. [electionEventId: {}, seed: {}]", electionEventId, electionEventSeed);

		// Create and persist the election event context payload.
		electionEventContextPersistenceService.persist(electionEventId, electionEventSeed, genSetupDataOutput);
		LOGGER.info("Successfully persisted election event context payload. [electionEventId: {}]", electionEventId);

		// Persist the setup key pair.
		final ElGamalMultiRecipientKeyPair setupKeyPair = genSetupDataOutput.getSetupKeyPair();
		setupKeyPairService.save(electionEventId, setupKeyPair);
	}

}
