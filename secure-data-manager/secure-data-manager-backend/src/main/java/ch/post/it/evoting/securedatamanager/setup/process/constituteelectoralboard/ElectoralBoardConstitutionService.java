/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally.SetupTallyEBOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally.SetupTallyEBService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerCardSetKeysService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ElectoralBoardConstitutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralBoardConstitutionService.class);

	private final SetupTallyEBService setupTallyEBService;
	private final ElectionEventService electionEventService;
	private final GenVerCardSetKeysService genVerCardSetKeysService;
	private final ElectoralBoardPersistenceService electoralBoardPersistenceService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final ControlComponentPublicKeysConfigService controlComponentPublicKeysConfigService;
	private final SetupComponentVerificationCardKeystoresPayloadGenerationService setupComponentVerificationCardKeystoresPayloadGenerationService;

	public ElectoralBoardConstitutionService(
			final SetupTallyEBService setupTallyEBService, final ElectionEventService electionEventService,
			final GenVerCardSetKeysService genVerCardSetKeysService, final ElectoralBoardPersistenceService electoralBoardPersistenceService,
			final ElectionEventContextPayloadService electionEventContextPayloadService, final ControlComponentPublicKeysConfigService controlComponentPublicKeysConfigService,
			final SetupComponentVerificationCardKeystoresPayloadGenerationService setupComponentVerificationCardKeystoresPayloadGenerationService) {
		this.setupTallyEBService = setupTallyEBService;
		this.electionEventService = electionEventService;
		this.genVerCardSetKeysService = genVerCardSetKeysService;
		this.electoralBoardPersistenceService = electoralBoardPersistenceService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.controlComponentPublicKeysConfigService = controlComponentPublicKeysConfigService;
		this.setupComponentVerificationCardKeystoresPayloadGenerationService = setupComponentVerificationCardKeystoresPayloadGenerationService;
	}

	/**
	 * Constitutes the electoral board.
	 *
	 * @param electionEventId                the election event id. Must be non-null and a valid UUID.
	 * @param electoralBoardMembersPasswords the passwords of the electoral board members. Must be non-null and a valid EBPassword.
	 * @param electoralBoardMembersHashes    the hashes of the electoral board members' passwords. Must be non-null.
	 * @throws FailedValidationException if the election event id is invalid.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws IllegalStateException     if any hash is empty.
	 */
	public void constitute(final String electionEventId, final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords,
			final ImmutableList<ImmutableByteArray> electoralBoardMembersHashes) {
		validateUUID(electionEventId);
		checkArgument(electionEventService.exists(electionEventId));

		checkNotNull(electoralBoardMembersPasswords);
		checkArgument(electoralBoardMembersPasswords.size() >= 2, "There must be at least two passwords.");

		checkNotNull(electoralBoardMembersHashes).forEach(hash -> checkArgument(!hash.isEmpty()));

		checkArgument(electoralBoardMembersPasswords.size() == electoralBoardMembersHashes.size());

		LOGGER.debug("Loading control component public keys... [electionEventId: {}]", electionEventId);
		final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys = controlComponentPublicKeysConfigService.loadOrderByNodeId(
				electionEventId);
		LOGGER.debug("Loaded control component public keys. [electionEventId: {}]", electionEventId);

		LOGGER.debug("Loading election event context payload... [electionEventId: {}]", electionEventId);
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);
		LOGGER.debug("Loaded election event context payload. [electionEventId: {}]", electionEventId);

		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = genVerCardSetKeysService.genVerCardSetKeys(
				electionEventContextPayload, controlComponentPublicKeys);

		LOGGER.info("GenVerCardSetKeys algorithm successfully performed. [electionEventId: {}]", electionEventId);

		final SetupTallyEBOutput setupTallyEBOutput = setupTallyEBService.setupTallyEB(electionEventContextPayload, electoralBoardMembersPasswords,
				controlComponentPublicKeys);

		LOGGER.info("Setup Tally EB algorithm successfully performed. [electionEventId: {}]", electionEventId);

		final ElGamalMultiRecipientPublicKey electionPublicKey = setupTallyEBOutput.getElectionPublicKey();
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupTallyEBOutput.getElectoralBoardPublicKey();
		final GroupVector<SchnorrProof, ZqGroup> electoralBoardSchnorrProofs = setupTallyEBOutput.getElectoralBoardSchnorrProofs();

		LOGGER.debug("Persisting Electoral Board... [electionEventId: {}]", electionEventId);

		electoralBoardPersistenceService.persist(electionEventId, controlComponentPublicKeys, choiceReturnCodesEncryptionPublicKey, electionPublicKey,
				electoralBoardPublicKey, electoralBoardSchnorrProofs, electoralBoardMembersHashes);

		LOGGER.info("Electoral board successfully constituted and persisted. [electionEventId: {}]", electionEventId);

		setupComponentVerificationCardKeystoresPayloadGenerationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKey,
				electionPublicKey);

		LOGGER.info("Successfully generated and persisted setup component verification card keystores payloads. [electionEventId: {}]",
				electionEventId);
	}

}
