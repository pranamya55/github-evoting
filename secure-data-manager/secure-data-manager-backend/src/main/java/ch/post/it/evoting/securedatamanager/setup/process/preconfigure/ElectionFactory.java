/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSet;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationObjectType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestDescriptionInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.DomainOfInfluenceType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectoralBoardType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.LanguageType;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardEntity;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;

/**
 * Creates the elections_config.json from the configuration-anonymized.xml and generates the election ids.
 */
public class ElectionFactory {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base16Alphabet = Base16Alphabet.getInstance();

	private ElectionFactory() {
		// static usage only.
	}

	public static ElectionEventEntity createElectionEvent(final Configuration configuration) {
		final String electionEventId = random.genRandomString(ID_LENGTH, base16Alphabet);
		final ContestType contestType = configuration.getContest();

		// Get the contest description in German.
		final String contestDescription = contestType.getContestDescription().getContestDescriptionInfo().stream()
				.filter(info -> info.getLanguage().equals(LanguageType.DE))
				.map(ContestDescriptionInformationType.ContestDescriptionInfo::getContestDescription)
				.collect(MoreCollectors.onlyElement());

		final int gracePeriod = configuration.getAuthorizations().getAuthorization().getFirst().getAuthorizationGracePeriod();

		return new ElectionEventEntity(
				electionEventId,
				contestDescription,
				contestDescription,
				contestType.getContestIdentification(),
				LocalDateTimeUtils.fromXMLGregorianCalendar(contestType.getEvotingFromDate()),
				LocalDateTimeUtils.fromXMLGregorianCalendar(contestType.getEvotingToDate()),
				gracePeriod
		);

	}

	public static ImmutableList<BallotBoxEntity> createBallotBoxes(final Configuration configuration, final ElectionEventEntity electionEvent) {
		return configuration.getAuthorizations().getAuthorization().stream()
				.map(authorizationType -> createBallotBox(authorizationType, electionEvent, random.genRandomString(ID_LENGTH, base16Alphabet)))
				.collect(toImmutableList());
	}

	private static BallotBoxEntity createBallotBox(final AuthorizationType authorizationType, final ElectionEventEntity electionEventEntity,
			final String ballotBoxId) {

		return new BallotBoxEntity(ballotBoxId, electionEventEntity, authorizationType.getAuthorizationName(),
				LocalDateTimeUtils.fromXMLGregorianCalendar(authorizationType.getAuthorizationFromDate()),
				LocalDateTimeUtils.fromXMLGregorianCalendar(authorizationType.getAuthorizationToDate()),
				authorizationType.getAuthorizationGracePeriod(), authorizationType.isAuthorizationTest());
	}

	public static ImmutableList<VerificationCardSetEntity> createVerificationCardSets(final Configuration configuration,
			final ElectionEventEntity electionEventEntity, final ImmutableMap<String, BallotBoxEntity> authorizationTypeBallotBoxMap) {
		return configuration.getAuthorizations().getAuthorization().stream()
				.map(authorizationType -> {
					final BallotBoxEntity ballotBoxEntity = authorizationTypeBallotBoxMap.get(authorizationType.getAuthorizationName());
					final String verificationCardSetId = random.genRandomString(ID_LENGTH, base16Alphabet);
					return createVerificationCardSet(configuration, electionEventEntity, ballotBoxEntity, authorizationType, verificationCardSetId);
				})
				.collect(toImmutableList());
	}

	private static VerificationCardSetEntity createVerificationCardSet(final Configuration configuration,
			final ElectionEventEntity electionEventEntity, final BallotBoxEntity ballotBoxEntity, final AuthorizationType authorizationType,
			final String verificationCardSetId) {

		final int numberOfEligibleVoters = (int) configuration.getRegister().getVoter().stream()
				.filter(voterType -> voterType.getAuthorization().equals(authorizationType.getAuthorizationIdentification()))
				.count();

		final ImmutableList<String> domainsOfInfluence = authorizationType.getAuthorizationObject().stream()
				.map(AuthorizationObjectType::getDomainOfInfluence)
				.map(DomainOfInfluenceType::getDomainOfInfluenceIdentification)
				.collect(toImmutableList());

		return new VerificationCardSetEntity(verificationCardSetId, electionEventEntity,
				ballotBoxEntity, VerificationCardSet.PREFIX + authorizationType.getAuthorizationName(), authorizationType.getAuthorizationName(),
				VerificationCardSet.PREFIX + authorizationType.getAuthorizationIdentification(), numberOfEligibleVoters, domainsOfInfluence);
	}

	public static ElectoralBoardEntity createElectoralBoard(final Configuration configuration) {
		final String electoralBoardId = random.genRandomString(ID_LENGTH, base16Alphabet);
		final ElectoralBoardType electoralBoardType = configuration.getContest().getElectoralBoard();
		final ImmutableList<String> boardMembers = ImmutableList.from(electoralBoardType.getElectoralBoardMembers().getElectoralBoardMemberName());

		return new ElectoralBoardEntity(electoralBoardId, electoralBoardType.getElectoralBoardName(),
				electoralBoardType.getElectoralBoardDescription(),
				electoralBoardType.getElectoralBoardIdentification(), boardMembers, Status.READY.name());
	}

}
