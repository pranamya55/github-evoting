/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.PRE_CONFIGURE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AnswerInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationObjectType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.BallotQuestionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.BallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestDescriptionInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionGroupBallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectoralBoardType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.RegisterType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.StandardAnswerType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.StandardBallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VariantBallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteDescriptionInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteType;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.PreconfigureSummary;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.VerificationCardSetSummary;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;

@Service
public class SummaryService {

	private final String electionEventSeed;
	private final WorkflowService workflowService;
	private final EvotingConfigService evotingConfigService;
	private final ElectionEventService electionEventService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	public SummaryService(
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed,
			final WorkflowService workflowService,
			final EvotingConfigService evotingConfigService,
			final ElectionEventService electionEventService,
			final ElectionEventContextPayloadService electionEventContextPayloadService) {
		this.electionEventSeed = validateSeed(electionEventSeed);
		this.workflowService = workflowService;
		this.evotingConfigService = evotingConfigService;
		this.electionEventService = electionEventService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * Builds the {@link ConfigurationSummary} from the given {@code configuration}.
	 *
	 * @param configuration the configuration anonymized file.
	 * @return the configuration summary.
	 * @throws NullPointerException if {@code configuration} is null.
	 */
	public ConfigurationSummary getConfigurationSummary(final Configuration configuration) {
		checkNotNull(configuration);

		return buildConfigurationSummary(configuration);
	}

	/**
	 * Reads the configuration anonymized XML file and builds the {@link ConfigurationSummary}.
	 *
	 * @return the serialized ConfigurationSummary object.
	 */
	public ConfigurationSummary getConfigurationSummary() {
		final Configuration configuration = evotingConfigService.load();

		checkState(configuration.getHeader().getVoterTotal() == configuration.getRegister().getVoter().size(),
				"The counted number of voters in the configuration-anonymized does not equal the voter total.");

		return buildConfigurationSummary(configuration);
	}

	private ConfigurationSummary buildConfigurationSummary(final Configuration configuration) {
		final ContestType contest = configuration.getContest();
		final String contestIdentification = contest.getContestIdentification();
		final ImmutableMap<String, String> contestDescription = contest.getContestDescription().getContestDescriptionInfo().stream()
				.collect(toImmutableMap(
						contestDescriptionInfo -> contestDescriptionInfo.getLanguage().value(),
						ContestDescriptionInformationType.ContestDescriptionInfo::getContestDescription));
		final XMLGregorianCalendar contestDate = contest.getContestDate();
		final XMLGregorianCalendar evotingFromDate = contest.getEvotingFromDate();
		final XMLGregorianCalendar evotingToDate = contest.getEvotingToDate();
		final int voterTotal = configuration.getHeader().getVoterTotal();
		final ImmutableList<String> extendedAuthenticationType = ImmutableList.from(contest.getExtendedAuthenticationKeys().getKeyName());

		final ElectoralBoardType electoralBoard = contest.getElectoralBoard();
		final ElectoralBoardSummary electoralBoardSummary = new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoard.getElectoralBoardIdentification())
				.electoralBoardName(electoralBoard.getElectoralBoardName())
				.electoralBoardDescription(electoralBoard.getElectoralBoardDescription())
				.members(ImmutableList.from(electoralBoard.getElectoralBoardMembers().getElectoralBoardMemberName()))
				.build();

		final ImmutableList<AuthorizationType> authorizations = ImmutableList.from(configuration.getAuthorizations().getAuthorization());
		final ImmutableList<AuthorizationSummary> authorizationsList = authorizations.stream()
				.map(authorizationType -> getAuthorizationSummary(authorizationType, configuration.getRegister()))
				.collect(toImmutableList());
		final ImmutableList<ElectionGroupSummary> electionGroupList = contest.getElectionGroupBallot().stream()
				.map(electionGroupBallotType -> getElectionGroupSummary(electionGroupBallotType, authorizationsList)).collect(toImmutableList());

		final ImmutableList<VoteSummary> voteList = contest.getVoteInformation().stream()
				.map(voteInformationType -> getVoteSummary(voteInformationType, authorizationsList))
				.collect(toImmutableList());

		ConfigurationSummary.Builder configurationSummaryBuilder = new ConfigurationSummary.Builder()
				.withContestId(contestIdentification)
				.withContestDescription(contestDescription)
				.withContestDate(LocalDateUtils.fromXMLGregorianCalendar(contestDate))
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(LocalDateTimeUtils.fromXMLGregorianCalendar(evotingFromDate))
				.withEvotingToDate(LocalDateTimeUtils.fromXMLGregorianCalendar(evotingToDate))
				.withGracePeriod(authorizations.get(0).getAuthorizationGracePeriod())
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoardSummary)
				.withElectionGroups(electionGroupList)
				.withVotes(voteList)
				.withAuthorizations(authorizationsList)
				.withConfigurationSignature(new String(configuration.getSignature().getSignatureValue().getValue(), StandardCharsets.UTF_8));

		if (workflowService.isStepComplete(PRE_CONFIGURE)) {
			final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(
					electionEventService.findElectionEventId());
			final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
			final ImmutableList<VerificationCardSetSummary> verificationCardSetSummaryList = electionEventContext.verificationCardSetContexts()
					.stream()
					.map(this::getVerificationCardSetSummary)
					.collect(toImmutableList());

			final PreconfigureSummary preconfigureSummary = new PreconfigureSummary.Builder()
					.withEncryptionGroup(electionEventContextPayload.getEncryptionGroup())
					.withMaximumNumberOfVotingOptions(electionEventContext.maximumNumberOfVotingOptions())
					.withMaximumNumberOfSelections(electionEventContext.maximumNumberOfSelections())
					.withMaximumNumberOfWriteInsPlusOne(electionEventContext.maximumNumberOfWriteInsPlusOne())
					.withVerificationCardSets(verificationCardSetSummaryList)
					.build();

			configurationSummaryBuilder = configurationSummaryBuilder.withPreconfigureSummary(preconfigureSummary);
		}

		return configurationSummaryBuilder.build();
	}

	private AuthorizationSummary getAuthorizationSummary(final AuthorizationType authorizationType, final RegisterType registerType) {
		final ImmutableList<AuthorizationObjectType> authorizationObjects = ImmutableList.from(authorizationType.getAuthorizationObject());
		final ImmutableList<AuthorizationObjectSummary> authorizationObjectList = authorizationObjects.stream()
				.map(authorizationObject -> new AuthorizationObjectSummary.Builder()
						.domainOfInfluenceId(authorizationObject.getDomainOfInfluence().getDomainOfInfluenceIdentification())
						.domainOfInfluenceType(authorizationObject.getDomainOfInfluence().getDomainOfInfluenceType())
						.domainOfInfluenceName(authorizationObject.getDomainOfInfluence().getDomainOfInfluenceName())
						.countingCircleId(authorizationObject.getCountingCircle().getCountingCircleIdentification())
						.countingCircleName(authorizationObject.getCountingCircle().getCountingCircleName())
						.build())
				.collect(toImmutableList());

		final long voterCount = registerType.getVoter().stream()
				.filter(voterType -> voterType.getAuthorization().equals(authorizationType.getAuthorizationIdentification()))
				.count();

		return new AuthorizationSummary.Builder()
				.authorizationId(authorizationType.getAuthorizationIdentification())
				.authorizationName(authorizationType.getAuthorizationName())
				.isTest(authorizationType.isAuthorizationTest())
				.fromDate(LocalDateTimeUtils.fromXMLGregorianCalendar(authorizationType.getAuthorizationFromDate()))
				.toDate(LocalDateTimeUtils.fromXMLGregorianCalendar(authorizationType.getAuthorizationToDate()))
				.numberOfVoters(voterCount)
				.authorizationObjects(authorizationObjectList)
				.build();
	}

	private ElectionGroupSummary getElectionGroupSummary(final ElectionGroupBallotType electionGroupBallotType,
			final ImmutableList<AuthorizationSummary> authorizations) {
		final String domainOfInfluence = electionGroupBallotType.getDomainOfInfluence();
		final ImmutableList<AuthorizationSummary> electionGroupAuthorizations = authorizations.stream()
				.filter(authorizationSummary -> authorizationSummary.getAuthorizationObjects().stream()
						.anyMatch(authorizationObjectSummary -> authorizationObjectSummary.getDomainOfInfluenceId().equals(domainOfInfluence)))
				.collect(toImmutableList());
		final ImmutableList<DescriptionSummary> descriptionList = electionGroupBallotType.getElectionGroupDescription()
				.getElectionGroupDescriptionInfo()
				.stream()
				.map(electionGroupDescriptionInfo -> new DescriptionSummary(electionGroupDescriptionInfo.getLanguage().value(),
						electionGroupDescriptionInfo.getElectionGroupDescriptionShort(),
						electionGroupDescriptionInfo.getElectionGroupDescription()))
				.collect(toImmutableList());
		return new ElectionGroupSummary(electionGroupBallotType.getElectionGroupIdentification(), descriptionList,
				electionGroupBallotType.getElectionGroupPosition(), electionGroupAuthorizations,
				electionGroupBallotType.getElectionInformation().stream().map(this::getElectionSummary).collect(toImmutableList()));
	}

	private ElectionSummary getElectionSummary(final ElectionInformationType electionInformationType) {
		final ElectionType election = electionInformationType.getElection();
		final ImmutableList<CandidateSummary> candidateList = electionInformationType.getCandidate().stream()
				.map(candidateInformationType -> new CandidateSummary.Builder()
						.candidateId(candidateInformationType.getCandidateIdentification())
						.familyName(candidateInformationType.getFamilyName())
						.firstName(candidateInformationType.getFirstName())
						.callName(candidateInformationType.getCallName())
						.dateOfBirth(LocalDateUtils.fromXMLGregorianCalendar(candidateInformationType.getDateOfBirth()))
						.isIncumbent(candidateInformationType.getIncumbent().isIncumbent())
						.referenceOnPosition(candidateInformationType.getReferenceOnPosition())
						.eligibility(candidateInformationType.getEligibility().value())
						.build())
				.collect(toImmutableList());
		final ImmutableList<ListSummary> lists = electionInformationType.getList().stream()
				.map(listInformationType -> {
							final ImmutableList<DescriptionSummary> descriptionList = listInformationType.getListDescription().getListDescriptionInfo()
									.stream()
									.map(listDescriptionInfo -> new DescriptionSummary(listDescriptionInfo.getLanguage().value(),
											listDescriptionInfo.getListDescriptionShort(), listDescriptionInfo.getListDescription()))
									.collect(toImmutableList());

							final ImmutableList<CandidatePositionSummary> candidatePositions = candidateList.stream()
									.map(candidate -> new CandidatePositionSummary(
											candidate.getCandidateId()))
									.collect(toImmutableList());

							return new ListSummary(listInformationType.getListIdentification(), listInformationType.getListIndentureNumber(), descriptionList,
									listInformationType.getListOrderOfPrecedence(), candidatePositions);
						}
				)
				.collect(toImmutableList());

		final ImmutableList<ListUnionSummary> listUnions = electionInformationType.getListUnion().stream()
				.map(listUnion -> {
					final String listUnionIdentification = listUnion.getListUnionIdentification();
					final ImmutableList<ListUnionDescriptionInfo> listUnionDescriptions = listUnion.getListUnionDescription()
							.getListUnionDescriptionInfo().stream()
							.map(descriptionInfo -> new ListUnionDescriptionInfo(
									descriptionInfo.getLanguage().value(),
									descriptionInfo.getListUnionDescription()))
							.collect(toImmutableList());
					final int listUnionType = listUnion.getListUnionType().intValueExact();
					final ImmutableList<String> referencedLists = listUnion.getReferencedList().stream()
							.collect(toImmutableList());

					return new ListUnionSummary(listUnionIdentification, listUnionDescriptions, listUnionType, referencedLists);
				})
				.collect(toImmutableList());

		final ImmutableList<DescriptionSummary> descriptionList = election.getElectionDescription().getElectionDescriptionInfo().stream()
				.map(electionDescriptionInfo -> new DescriptionSummary(electionDescriptionInfo.getLanguage().value(),
						electionDescriptionInfo.getElectionDescriptionShort(), electionDescriptionInfo.getElectionDescription()))
				.collect(toImmutableList());

		final Integer primarySecondaryType = election.getReferencedElection().stream()
				// When the election is primary, the election relation is 2, when it is secondary, the election relation is 1.
				// Therefore, the primarySecondaryType is 1 when the election is primary and 2 when the election is secondary.
				.map(referencedElection -> 3 - referencedElection.getElectionRelation().intValueExact())
				.findFirst()
				.orElse(0);

		return new ElectionSummary.Builder()
				.electionId(election.getElectionIdentification())
				.electionPosition(election.getElectionPosition())
				.electionType(election.getTypeOfElection().intValueExact())
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(descriptionList)
				.numberOfMandates(election.getNumberOfMandates())
				.writeInsAllowed(election.isWriteInsAllowed())
				.candidateAccumulation(election.getCandidateAccumulation().intValueExact())
				.candidates(candidateList)
				.lists(lists)
				.listUnions(listUnions)
				.build();

	}

	private VoteSummary getVoteSummary(final VoteInformationType voteInformationType, final ImmutableList<AuthorizationSummary> authorizations) {
		final VoteType vote = voteInformationType.getVote();
		final ImmutableMap<String, String> voteDescription = vote.getVoteDescription().getVoteDescriptionInfo().stream()
				.collect(toImmutableMap(
						descriptionInfo -> descriptionInfo.getLanguage().value(),
						VoteDescriptionInformationType.VoteDescriptionInfo::getVoteDescription));

		final String domainOfInfluence = vote.getDomainOfInfluence();
		final ImmutableList<AuthorizationSummary> voteAuthorizations = authorizations.stream()
				.filter(authorizationSummary -> authorizationSummary.getAuthorizationObjects().stream()
						.anyMatch(authorizationObjectSummary -> authorizationObjectSummary.getDomainOfInfluenceId().equals(domainOfInfluence)))
				.collect(toImmutableList());

		final ImmutableList<BallotSummary> ballots = vote.getBallot().stream().map(this::getBallotSummary).collect(toImmutableList());

		return new VoteSummary(vote.getVoteIdentification(), vote.getVotePosition(), voteDescription, domainOfInfluence,
				voteAuthorizations, ballots);
	}

	private BallotSummary getBallotSummary(final BallotType ballotType) {
		final String ballotIdentification = ballotType.getBallotIdentification();
		final int ballotPosition = ballotType.getBallotPosition();
		final ImmutableList<DescriptionSummary> ballotDescription = ballotType.getBallotDescription().getBallotDescriptionInfo().stream()
				.map(ballotDescriptionInfo -> new DescriptionSummary(ballotDescriptionInfo.getLanguage().value(),
						ballotDescriptionInfo.getBallotDescriptionShort(), ballotDescriptionInfo.getBallotDescriptionLong()))
				.collect(toImmutableList());

		final List<QuestionSummary> questions = new ArrayList<>();
		final StandardBallotType standardBallot = ballotType.getStandardBallot();
		if (standardBallot != null) {
			final ImmutableList<DescriptionSummary> questionInfo = getQuestionInfo(standardBallot.getBallotQuestion());
			final ImmutableList<AnswerSummary> answerList = getAnswerSummaries(standardBallot.getAnswer());

			questions.add(new QuestionSummary(1, standardBallot.getQuestionNumber(), questionInfo, answerList));
		}

		final VariantBallotType variantBallot = ballotType.getVariantBallot();
		if (variantBallot != null) {
			variantBallot.getStandardQuestion().forEach(standardQuestionType -> {
				final ImmutableList<DescriptionSummary> questionInfo = getQuestionInfo(standardQuestionType.getBallotQuestion());
				final ImmutableList<AnswerSummary> answerList = getAnswerSummaries(standardQuestionType.getAnswer());

				questions.add(new QuestionSummary(standardQuestionType.getQuestionPosition(), standardQuestionType.getQuestionNumber(), questionInfo,
						answerList));
			});
			variantBallot.getTieBreakQuestion().forEach(tieBreakQuestionType -> {
				final ImmutableList<DescriptionSummary> questionInfo = getQuestionInfo(tieBreakQuestionType.getBallotQuestion());
				final ImmutableList<AnswerSummary> answerList = tieBreakQuestionType.getAnswer().stream()
						.map(tiebreakAnswerType -> {
							final ImmutableMap<String, String> answers = tiebreakAnswerType.getAnswerInfo().stream()
									.collect(toImmutableMap(
											answerInfo -> answerInfo.getLanguage().value(),
											AnswerInformationType::getAnswer));
							return new AnswerSummary(tiebreakAnswerType.getAnswerPosition(), answers);

						})
						.collect(toImmutableList());

				questions.add(new QuestionSummary(tieBreakQuestionType.getQuestionPosition(), tieBreakQuestionType.getQuestionNumber(), questionInfo,
						answerList));
			});
		}

		return new BallotSummary(ballotIdentification, ballotPosition, ballotDescription, ImmutableList.from(questions));
	}

	private ImmutableList<DescriptionSummary> getQuestionInfo(final BallotQuestionType ballotQuestionType) {
		return ballotQuestionType.getBallotQuestionInfo().stream()
				.map(ballotQuestionInfo -> new DescriptionSummary(ballotQuestionInfo.getLanguage().value(),
						ballotQuestionInfo.getBallotQuestionTitle(),
						ballotQuestionInfo.getBallotQuestion()))
				.collect(toImmutableList());
	}

	private ImmutableList<AnswerSummary> getAnswerSummaries(final List<StandardAnswerType> standardAnswers) {
		return standardAnswers.stream()
				.map(standardAnswerType -> {
					final ImmutableMap<String, String> answers = standardAnswerType.getAnswerInfo().stream()
							.collect(toImmutableMap(answerInfo -> answerInfo.getLanguage().value(), AnswerInformationType::getAnswer));
					return new AnswerSummary(standardAnswerType.getAnswerPosition(), answers);
				})
				.collect(toImmutableList());
	}

	private VerificationCardSetSummary getVerificationCardSetSummary(final VerificationCardSetContext verificationCardSetContext) {
		return new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetContext.getVerificationCardSetAlias())
				.setTestBallotBox(verificationCardSetContext.isTestBallotBox())
				.setNumberOfEligibleVoters(verificationCardSetContext.getNumberOfEligibleVoters())
				.setNumberOfVotingOptions(verificationCardSetContext.getNumberOfVotingOptions())
				.setGracePeriod(verificationCardSetContext.getGracePeriod())
				.build();
	}
}
