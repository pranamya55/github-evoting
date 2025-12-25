/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.setup.process.SetupEvotingConfigFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxEntity;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxRepository;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxStateEntity;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxStateRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardRepository;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetStateEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetStateRepository;

/**
 * Saves the election configuration information to the corresponding repositories.
 */
@Repository
@ConditionalOnProperty("role.isSetup")
public class PreConfigureRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreConfigureRepository.class);

	private final BallotBoxRepository ballotBoxRepository;
	private final BallotBoxStateRepository ballotBoxStateRepository;
	private final ElectionEventRepository electionEventRepository;
	private final ElectoralBoardRepository electoralBoardRepository;
	private final EvotingConfigFileRepository evotingConfigFileRepository;
	private final VerificationCardSetRepository verificationCardSetRepository;
	private final VerificationCardSetStateRepository verificationCardSetStateRepository;

	public PreConfigureRepository(
			final BallotBoxRepository ballotBoxRepository,
			final BallotBoxStateRepository ballotBoxStateRepository,
			final ElectionEventRepository electionEventRepository,
			final ElectoralBoardRepository electoralBoardRepository,
			final VerificationCardSetRepository verificationCardSetRepository,
			final SetupEvotingConfigFileRepository setupEvotingConfigFileRepository,
			final VerificationCardSetStateRepository verificationCardSetStateRepository) {
		this.ballotBoxRepository = ballotBoxRepository;
		this.ballotBoxStateRepository = ballotBoxStateRepository;
		this.electionEventRepository = electionEventRepository;
		this.electoralBoardRepository = electoralBoardRepository;
		this.evotingConfigFileRepository = setupEvotingConfigFileRepository;
		this.verificationCardSetRepository = verificationCardSetRepository;
		this.verificationCardSetStateRepository = verificationCardSetStateRepository;
	}

	/**
	 * Creates the elections_config using the configuration-anonymized and saves it. Saves the input files: configuration-anonymized and
	 * encryptionParameters.
	 *
	 * @return the generated election event id.
	 */
	public String createElectionsConfig() {
		LOGGER.debug("Creating the elections_config...");

		// Get input file (configuration-anonymized.xml)
		final Configuration configuration = evotingConfigFileRepository.load()
				.orElseThrow(() -> new IllegalArgumentException(
						"A configuration-anonymized.xml file is needed for the election event pre-configuration."));
		checkState(configuration.getHeader().getVoterTotal() == configuration.getRegister().getVoter().size(),
				"The counted number of voters in the configuration-anonymized does not equal the voter total.");

		final ElectionEventEntity electionEventEntity = ElectionFactory.createElectionEvent(configuration);
		electionEventRepository.save(electionEventEntity);

		final ElectoralBoardEntity electoralBoardEntity = ElectionFactory.createElectoralBoard(configuration);
		electoralBoardRepository.save(electoralBoardEntity);

		final ImmutableList<BallotBoxEntity> ballotBoxEntities = ElectionFactory.createBallotBoxes(configuration, electionEventEntity);
		ballotBoxRepository.saveAll(ballotBoxEntities);

		ballotBoxEntities.forEach(ballotBoxEntity -> {
			final BallotBoxStateEntity ballotBoxStateEntity = new BallotBoxStateEntity(ballotBoxEntity.getBallotBoxId(), BallotBoxStatus.READY.name());
			ballotBoxStateRepository.save(ballotBoxStateEntity);
		});

		final ImmutableMap<String, BallotBoxEntity> authorizationTypeBallotBoxMap = getAuthorizationTypeBallotBoxMap(configuration);
		final ImmutableList<VerificationCardSetEntity> verificationCardSetEntities = ElectionFactory.createVerificationCardSets(configuration,
				electionEventEntity, authorizationTypeBallotBoxMap);
		verificationCardSetRepository.saveAll(verificationCardSetEntities);

		verificationCardSetEntities.forEach(verificationCardSetEntity -> {
			final VerificationCardSetStateEntity verificationCardSetStateEntity = new VerificationCardSetStateEntity(
					verificationCardSetEntity.getVerificationCardSetId(), Status.READY.name());
			verificationCardSetStateRepository.save(verificationCardSetStateEntity);
		});

		LOGGER.info("Election pre-configuration completed.");

		return electionEventEntity.getElectionEventId();
	}

	public ImmutableMap<String, BallotBoxEntity> getAuthorizationTypeBallotBoxMap(final Configuration configuration) {
		return configuration.getAuthorizations().getAuthorization().stream()
				.collect(toImmutableMap(
						AuthorizationType::getAuthorizationName,
						authorizationType -> ballotBoxRepository.findByDescription(authorizationType.getAuthorizationName()))
				);
	}
}
