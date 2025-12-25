/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBox;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;

@Service
@ConditionalOnProperty("role.isTally")
public class TallyComponentFilesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TallyComponentFilesService.class);

	private final BallotBoxService ballotBoxService;
	private final EvotingConfigService evotingConfigService;
	private final TallyComponentVotesService tallyComponentVotesService;
	private final TallyComponentEch0222Service tallyComponentEch0222Service;

	public TallyComponentFilesService(
			final BallotBoxService ballotBoxService,
			final EvotingConfigService evotingConfigService,
			final TallyComponentVotesService tallyComponentVotesService,
			final TallyComponentEch0222Service tallyComponentEch0222Service) {
		this.ballotBoxService = ballotBoxService;
		this.evotingConfigService = evotingConfigService;
		this.tallyComponentVotesService = tallyComponentVotesService;
		this.tallyComponentEch0222Service = tallyComponentEch0222Service;
	}

	/**
	 * Generates and persists the tally output files.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public void generate(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Generating tally files... [electionEventId: {}]", electionEventId);

		final ImmutableMap<String, TallyComponentVotesPayload> authorizationNameToTallyComponentVotesPayloadMap =
				getAuthorizationNameToTallyComponentVotesPayloadMap(electionEventId);

		tallyComponentEch0222Service.generate(electionEventId, authorizationNameToTallyComponentVotesPayloadMap);

		LOGGER.info("Tally files successfully generated. [electionEventId: {}]", electionEventId);
	}

	/**
	 * @return the map of configuration authorizations to corresponding tally component votes payload for all ballot boxes that are decrypted and
	 * synchronized. The mapping is done through the field {@link AuthorizationType#getAuthorizationName()}.
	 */
	private ImmutableMap<String, TallyComponentVotesPayload> getAuthorizationNameToTallyComponentVotesPayloadMap(final String electionEventId) {

		final Configuration configuration = evotingConfigService.load();
		final ImmutableList<AuthorizationType> allAuthorizations = ImmutableList.from(configuration.getAuthorizations().getAuthorization());

		final ImmutableList<BallotBox> ballotBoxes = ballotBoxService.getBallotBoxes(electionEventId);

		record TallyComponentVotesTuple(String authorizationName, TallyComponentVotesPayload tallyComponentVotesPayload) {
		}

		return ballotBoxes.stream()
				.parallel()
				.filter(ballotBox -> BallotBoxStatus.DECRYPTED.name().equals(ballotBox.status()))
				.map(ballotBox -> {
					final String ballotBoxId = ballotBox.id();

					// Ballot box description corresponds to the authorization name.
					final String ballotBoxDescription = ballotBox.description();

					final String authorizationName = allAuthorizations.stream().parallel()
							.map(AuthorizationType::getAuthorizationName)
							.filter(name -> name.equals(ballotBoxDescription))
							.collect(MoreCollectors.onlyElement());

					return new TallyComponentVotesTuple(authorizationName, tallyComponentVotesService.load(electionEventId, ballotBoxId));
				})
				.collect(toImmutableMap(TallyComponentVotesTuple::authorizationName, TallyComponentVotesTuple::tallyComponentVotesPayload));
	}

}
