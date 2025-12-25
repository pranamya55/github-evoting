/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.securedatamanager.shared.process.Status.PRECOMPUTED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.securedatamanager.setup.process.SetupKeyPairService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerDatOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerDatService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * Service that deals with the pre-computation of verification card sets.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class VerificationCardSetPreComputeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetPreComputeService.class);

	private final GenVerDatService genVerDatService;
	private final SetupKeyPairService setupKeyPairService;
	private final ElectionEventService electionEventService;
	private final VerificationCardSetService verificationCardSetService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final VerificationCardSetPreComputationPersistenceService verificationCardSetPrecomputationPersistenceService;
	private final int chunkSize;

	public VerificationCardSetPreComputeService(
			final GenVerDatService genVerDatService,
			final SetupKeyPairService setupKeyPairService,
			final ElectionEventService electionEventService,
			final VerificationCardSetService verificationCardSetService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final VerificationCardSetPreComputationPersistenceService verificationCardSetPrecomputationPersistenceService,
			@Value("${sdm.process.precompute.genVerDat-chunk-size:100}")
			final int chunkSize) {
		this.genVerDatService = genVerDatService;
		this.setupKeyPairService = setupKeyPairService;
		this.electionEventService = electionEventService;
		this.verificationCardSetService = verificationCardSetService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.verificationCardSetPrecomputationPersistenceService = verificationCardSetPrecomputationPersistenceService;
		this.chunkSize = chunkSize;
	}

	public void preComputeVerificationCardSet(final PrecomputeContext precomputeContext) {
		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		checkArgument(electionEventService.exists(electionEventId), "The election event id of the given context does not exist.");

		LOGGER.debug("Starting pre-computation of verification card set... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);
		final int numberOfEligibleVoters = verificationCardSetEntity.getNumberOfEligibleVoters();

		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);
		final ElGamalMultiRecipientPublicKey setupPublicKey = setupKeyPairService.load(electionEventId).getPublicKey();
		final PrimesMappingTable primesMappingTable = electionEventContextPayloadService.loadPrimesMappingTable(electionEventId,
				verificationCardSetId);

		LOGGER.debug("Generating the verification data... [numberOfEligibleVoters: {}, electionEventId: {}, verificationCardSetId: {}]",
				numberOfEligibleVoters, electionEventId, verificationCardSetId);

		final ImmutableList<GenVerDatOutput> genVerDatOutputs = genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, chunkSize,
				electionEventContextPayload, setupPublicKey, primesMappingTable);

		final GqGroup encryptionGroup = electionEventContextPayloadService.loadEncryptionGroup(electionEventId);
		checkState(genVerDatOutputs.stream().parallel().map(GenVerDatOutput::getGroup).allMatch(group -> group.equals(encryptionGroup)),
				"The group from the GenVerDat outputs does not correspond to the encryption group.");

		// Build and persist payloads to request the return code generation (choice return codes and vote cast return code) from the control components.
		verificationCardSetPrecomputationPersistenceService.persistPreComputationPayloads(precomputeContext, genVerDatOutputs);

		LOGGER.info("Successfully generated the verification data. [numberOfEligibleVoters: {}, electionEventId: {}, verificationCardSetId: {}]",
				numberOfEligibleVoters, electionEventId, verificationCardSetId);

		// Update the verification card set status to 'pre-computed'.
		verificationCardSetService.updateStatus(verificationCardSetId, PRECOMPUTED);

		LOGGER.info("Updated verification card set status to pre-computed. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

}

