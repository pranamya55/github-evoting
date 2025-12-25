/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.cryptoprimitives.math.RandomFactory.createRandom;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateUtils;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.PreconfigureSummary;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.VerificationCardSetSummary;

public class SummaryGenerator {

	private static final Random random = createRandom();

	public ConfigurationSummary generateConfigurationSummary() {
		final String contestId = "contest-id";
		final ImmutableMap<String, String> contestDescription = ImmutableMap.of(
				Language.DE.name(), "Wahl",
				Language.FR.name(), "Élection",
				Language.IT.name(), "Elezione",
				Language.RM.name(), "Votaziun"
		);
		final LocalDate contestDate = LocalDateUtils.now();
		final String electionEventSeed = "NE_20241112_TT01";
		final LocalDateTime evotingFromDate = LocalDateTimeUtils.now();
		final LocalDateTime evotingToDate = evotingFromDate.plusSeconds(600);
		final int gracePeriod = 900;
		final int voterTotal = random.genRandomInteger(100) + 1;
		final ImmutableList<String> extendedAuthenticationType = ImmutableList.of("birthDate");
		final ElectoralBoardSummary electoralBoard = generateElectoralBoardSummary();
		final ImmutableList<ElectionGroupSummary> electionGroups = Stream.generate(this::generateElectionGroupSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<VoteSummary> votes = Stream.generate(this::generateVoteSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<AuthorizationSummary> authorizations = Stream.generate(this::generateAuthorizationSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final PreconfigureSummary preconfigureSummary = generatePreconfigureSummary();
		final String configurationSignature = "configuration-signature";

		return new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature)
				.build();
	}

	public ElectoralBoardSummary generateElectoralBoardSummary() {
		final String electoralBoardId = "electoral-board-id";
		final String electoralBoardName = "electoral board name";
		final String electoralBoardDescription = "electoral board description";
		final ImmutableList<String> members = IntStream.range(0, random.genRandomInteger(3) + 1)
				.mapToObj(i -> String.format("EB%d", i))
				.collect(ImmutableList.toImmutableList());

		return new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(electoralBoardDescription)
				.members(members)
				.build();
	}

	public ElectionGroupSummary generateElectionGroupSummary() {
		final String electionGroupId = "election-group-id";
		final ImmutableList<DescriptionSummary> electionGroupDescription = generateDescriptions(ElectionGroupSummary.class.getName());
		final int electionGroupPosition = random.genRandomInteger(10) + 1;
		final ImmutableList<AuthorizationSummary> authorizations = Stream.generate(this::generateAuthorizationSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<ElectionSummary> elections = Stream.generate(this::generateElectionSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new ElectionGroupSummary(electionGroupId, electionGroupDescription, electionGroupPosition, authorizations, elections);
	}

	public AuthorizationSummary generateAuthorizationSummary() {
		final String authorizationId = "authorization-id";
		final String authorizationName = "authorization name";
		final boolean isTest = random.genRandomInteger(10) % 2 == 0;
		final LocalDateTime fromDate = LocalDateTimeUtils.now();
		final LocalDateTime toDate = fromDate.plusSeconds(600);
		final long numberOfVoters = random.genRandomInteger(100) + 1;
		final ImmutableList<AuthorizationObjectSummary> authorizationObjects = Stream.generate(this::generateAuthorizationObjectSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects)
				.build();
	}

	public AuthorizationObjectSummary generateAuthorizationObjectSummary() {
		final String domainOfInfluenceId = "domain-of-influence-id";
		final String domainOfInfluenceType = "CH";
		final String domainOfInfluenceName = "domain of influence name";
		final String countingCircleId = "counting-circle-id";
		final String countingCircleName = "counting circle name";

		return new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(countingCircleId)
				.countingCircleName(countingCircleName)
				.build();
	}

	public ElectionSummary generateElectionSummary() {
		final String electionId = "election-id";
		final int electionPosition = random.genRandomInteger(10) + 1;
		final int electionType = random.genRandomInteger(2);
		final int primarySecondaryType = random.genRandomInteger(3);
		final ImmutableList<DescriptionSummary> electionDescription = generateDescriptions(ElectionSummary.class.getName());
		final int numberOfMandates = random.genRandomInteger(6) + 1;
		final boolean writeInsAllowed = random.genRandomInteger(10) % 2 == 0;
		final int candidateAccumulation = random.genRandomInteger(2);
		final ImmutableList<CandidateSummary> candidates = Stream.generate(this::generateCandidateSummary)
				.limit(random.genRandomInteger(7) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<ListSummary> lists = Stream.generate(this::generateListSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<ListUnionSummary> listUnions = Stream.generate(this::generateListUnionSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists)
				.listUnions(listUnions)
				.build();
	}

	public CandidateSummary generateCandidateSummary() {
		final String candidateId = "candidate-id";
		final String familyName = "family name";
		final String firstName = "first name";
		final String callName = "call name";
		final LocalDate dateOfBirth = LocalDateUtils.now();
		final boolean isIncumbent = random.genRandomInteger(10) % 2 == 0;
		final String referenceOnPosition = "01a.01";
		final String eligibility = "explicit";

		return new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility)
				.build();
	}

	public ListSummary generateListSummary() {
		final String listId = "list-id";
		final String listIndentureNumber = String.format("%d", random.genRandomInteger(10));
		final ImmutableList<DescriptionSummary> listDescription = generateDescriptions(ListSummary.class.getName());
		final int listOrderOfPrecedence = random.genRandomInteger(10) + 1;
		final ImmutableList<CandidatePositionSummary> candidatePositions = Stream.generate(
						() -> new CandidatePositionSummary("candidate-position-id"))
				.limit(random.genRandomInteger(5) + 1)
				.collect(ImmutableList.toImmutableList());

		return new ListSummary(listId, listIndentureNumber, listDescription, listOrderOfPrecedence, candidatePositions);
	}

	public ListUnionSummary generateListUnionSummary() {
		final String listUnionIdentification = "list-union-id";
		final ImmutableList<ListUnionDescriptionInfo> listUnionDescriptionInfo = generateListUnionDescriptionInfo();
		final int listUnionType = random.genRandomInteger(1) + 1;
		final ImmutableList<String> referencedLists = Stream.generate(() -> "referenced-list-id")
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new ListUnionSummary(listUnionIdentification, listUnionDescriptionInfo, listUnionType, referencedLists);
	}

	public VoteSummary generateVoteSummary() {
		final String voteId = "vote-id";
		final int votePosition = random.genRandomInteger(5);
		final ImmutableMap<String, String> voteDescription = ImmutableMap.of(
				Language.DE.name(), "Abstimmung",
				Language.FR.name(), "Vote",
				Language.IT.name(), "Voto",
				Language.RM.name(), "Votaziun"
		);
		final String domainOfInfluence = "domain-of-influence";
		final ImmutableList<AuthorizationSummary> authorizations = Stream.generate(this::generateAuthorizationSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<BallotSummary> ballots = Stream.generate(this::generateBallotSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new VoteSummary(voteId, votePosition, voteDescription, domainOfInfluence, authorizations, ballots);
	}

	public BallotSummary generateBallotSummary() {
		final String ballotId = "ballot-id";
		final int ballotPosition = random.genRandomInteger(8) + 1;
		final ImmutableList<DescriptionSummary> ballotDescription = generateDescriptions(BallotSummary.class.getName());
		final ImmutableList<QuestionSummary> questions = Stream.generate(this::generateQuestionSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new BallotSummary(ballotId, ballotPosition, ballotDescription, questions);
	}

	public QuestionSummary generateQuestionSummary() {
		final int questionPosition = random.genRandomInteger(10) + 1;
		final String questionNumber = String.format("%d)", questionPosition);
		final ImmutableList<DescriptionSummary> questionInfo = generateDescriptions(QuestionSummary.class.getName());
		final ImmutableList<AnswerSummary> answers = Stream.generate(this::generateAnswerSummary)
				.limit(random.genRandomInteger(3) + 1)
				.collect(ImmutableList.toImmutableList());

		return new QuestionSummary(questionPosition, questionNumber, questionInfo, answers);
	}

	public AnswerSummary generateAnswerSummary() {
		final int answerPosition = random.genRandomInteger(10) + 1;
		final ImmutableMap<String, String> answerInfo = ImmutableMap.of(
				Language.DE.name(), "Ja",
				Language.FR.name(), "Oui",
				Language.IT.name(), "Sì",
				Language.RM.name(), "Gea"
		);

		return new AnswerSummary(answerPosition, answerInfo);
	}

	public PreconfigureSummary generatePreconfigureSummary() {
		final GqGroup gqGroup = GroupTestData.getGqGroup();
		final int maximumNumberOfVotingOptions = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS) + 1; // range [1, n_sup]
		final int maximumNumberOfSelections = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS) + 1; // range [1, psi_sup]
		final int maximumNumberOfWriteInsPlusOne = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1) + 1; // range [1, delta_sup]
		final ImmutableList<VerificationCardSetSummary> verificationCardSets = Stream.generate(this::generateVerificationCardSetSummary)
				.limit(random.genRandomInteger(5) + 1)
				.collect(ImmutableList.toImmutableList());

		return new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets)
				.build();
	}

	public VerificationCardSetSummary generateVerificationCardSetSummary() {
		final String verificationCardSetAlias = "verificationCardSetAlias";
		final boolean testBallotBox = random.genRandomInteger(10) % 2 == 0;
		final int numberOfEligibleVoters = random.genRandomInteger(100) + 1;
		final int numberOfVotingOptions = random.genRandomInteger(4) + 1;
		final int gracePeriod = 900;

		return new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(gracePeriod)
				.build();
	}

	private ImmutableList<DescriptionSummary> generateDescriptions(final String type) {
		return Arrays.stream(Language.values())
				.map(language -> new DescriptionSummary(
						language.name(),
						String.format("%s: %s short description", language.name(), type),
						String.format("%s: %s long description", language.name(), type)
				))
				.collect(ImmutableList.toImmutableList());
	}

	private ImmutableList<ListUnionDescriptionInfo> generateListUnionDescriptionInfo() {
		return ImmutableList.of(
				new ListUnionDescriptionInfo("de", "Vereinigte Subliste 1a: Partei 02 - Partei 03"),
				new ListUnionDescriptionInfo("fr", "Sous-apparentement 1a : Partei 02 - Partei 03"),
				new ListUnionDescriptionInfo("it", "Sublistino unito 1a: Partei 02 - Partei 03"),
				new ListUnionDescriptionInfo("rm", "Sutglista unida 1a: Partei 02 - Partei 03")
		);
	}

	public CandidatePositionSummary generateCandidatePositionSummary() {
		final int length = random.genRandomInteger(MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH) + 1;
		final String candidateIdentification = random.genRandomString(length, Base16Alphabet.getInstance());

		return new CandidatePositionSummary(candidateIdentification);
	}
}
