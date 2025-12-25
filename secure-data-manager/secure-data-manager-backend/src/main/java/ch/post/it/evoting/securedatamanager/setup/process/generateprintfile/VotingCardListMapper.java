/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.election.ActualVotingOptionUtils.getAnswerActualVotingOption;
import static ch.post.it.evoting.evotinglibraries.domain.election.ActualVotingOptionUtils.getCandidateActualVotingOption;
import static ch.post.it.evoting.evotinglibraries.domain.election.ActualVotingOptionUtils.getEmptyPositionActualVotingOption;
import static ch.post.it.evoting.evotinglibraries.domain.election.ActualVotingOptionUtils.getListActualVotingOption;
import static ch.post.it.evoting.evotinglibraries.domain.election.ActualVotingOptionUtils.getWriteInPositionActualVotingOption;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.domain.configuration.ChoiceReturnCodeToEncodedVotingOptionEntry;
import ch.post.it.evoting.domain.configuration.VoterReturnCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.election.VotingOptionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationObjectType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.BallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.CandidatePositionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.DomainOfInfluenceType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.EmptyPositionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VariantBallotType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteInformationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.AnswerType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.CandidateListType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.CandidateType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.ElectionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.EmptyListType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.ListType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.QuestionType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.VoteType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.VotingCardList;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.VotingCardType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.WriteInPositionType;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService.VoterInitialCodesByVcs;

/**
 * Maps to {@link VotingCardList}.
 */
public class VotingCardListMapper {

	private VotingCardListMapper() {
		// static usage only.
	}

	/**
	 * Returns the voting card list to be output in evoting-print.
	 *
	 * @param configuration               the configuration of the event.
	 * @param voterInitialCodesPayloadMap the map of voter identification and voter initial codes payload.
	 * @param primesMappingTableMap       the map of verificationCardSetId and primes map table.
	 * @return the root object VotingCardList of the evoting-print file.
	 * @throws NullPointerException if any input is null.
	 */
	public static VotingCardList toVotingCardList(final Configuration configuration,
			final ImmutableMap<String, VoterInitialCodesByVcs> voterInitialCodesPayloadMap,
			final ImmutableMap<String, VoterReturnCodes> voterReturnCodesMap, final ImmutableMap<String, PrimesMappingTable> primesMappingTableMap) {

		checkNotNull(configuration);
		checkNotNull(voterInitialCodesPayloadMap);
		checkNotNull(primesMappingTableMap);

		// Prepare reused list
		final List<AuthorizationType> authorizationTypeList = configuration.getAuthorizations().getAuthorization();
		final List<ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteType> voteTypeList = configuration.getContest()
				.getVoteInformation().stream().parallel()
				.map(VoteInformationType::getVote)
				.toList();

		final List<ElectionInformationTypeExtended> electionInformationTypeExtendedList = configuration.getContest().getElectionGroupBallot().stream()
				.parallel()
				.flatMap(electionGroupBallotType -> electionGroupBallotType.getElectionInformation().stream().parallel()
						.map(electionInformationType -> new ElectionInformationTypeExtended(electionInformationType,
								electionGroupBallotType.getDomainOfInfluence())))
				.toList();

		final List<VotingCardType> votingCardTypesList = configuration.getRegister().getVoter().stream().parallel()
				.map(voterType -> {
					final List<String> domainOfInfluenceList = authorizationTypeList.stream().parallel()
							.filter(authorizationType -> authorizationType.getAuthorizationIdentification().equals(voterType.getAuthorization()))
							.map(AuthorizationType::getAuthorizationObject)
							.flatMap(Collection::stream)
							.map(AuthorizationObjectType::getDomainOfInfluence)
							.map(DomainOfInfluenceType::getDomainOfInfluenceIdentification)
							.toList();

					final String voterIdentification = voterType.getVoterIdentification();
					final VoterInitialCodesByVcs voterInitialCodesByVcs = voterInitialCodesPayloadMap.get(voterIdentification);
					final VoterInitialCodes voterInitialCodes = voterInitialCodesByVcs.voterInitialCodes();
					final VoterReturnCodes voterReturnCodes = voterReturnCodesMap.get(voterInitialCodes.verificationCardId());
					final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap = voterReturnCodes.choiceReturnCodesToEncodedVotingOptions()
							.stream()
							.collect(toImmutableMap(
									ChoiceReturnCodeToEncodedVotingOptionEntry::encodedVotingOption,
									ChoiceReturnCodeToEncodedVotingOptionEntry::choiceReturnCode));
					final PrimesMappingTable primesMappingTable = primesMappingTableMap.get(voterInitialCodesByVcs.verificationCardSetId());

					final VotingCardType votingCard = new VotingCardType();
					votingCard.setVoterIdentification(voterIdentification);
					votingCard.setVotingCardId(voterInitialCodes.votingCardId());
					votingCard.setStartVotingKey(voterInitialCodes.startVotingKey());
					votingCard.setBallotCastingKey(voterInitialCodes.ballotCastingKey());
					votingCard.setVoteCastReturnCode(voterReturnCodes.voteCastReturnCode());

					// Votes
					if (!voteTypeList.isEmpty()) {
						votingCard.setVote(toVotes(voteTypeList, domainOfInfluenceList, primesMappingTable, encodedVotingOptionToChoiceCodeMap));
					}

					// Elections
					if (!electionInformationTypeExtendedList.isEmpty()) {
						votingCard.setElection(toElections(electionInformationTypeExtendedList, domainOfInfluenceList, primesMappingTable,
								encodedVotingOptionToChoiceCodeMap));
					}

					return votingCard;
				})
				.toList();

		final ContestType contest = new ContestType();
		contest.setContestIdentification(configuration.getContest().getContestIdentification());
		contest.setVotingCard(votingCardTypesList);

		final VotingCardList votingCardList = new VotingCardList();
		votingCardList.setContest(contest);

		return votingCardList;
	}

	private static List<VoteType> toVotes(final List<ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteType> voteTypeList,
			final List<String> domainOfInfluenceList, final PrimesMappingTable primesMappingTable,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		return voteTypeList.stream().parallel()
				.filter(configurationVoteType -> domainOfInfluenceList.contains(configurationVoteType.getDomainOfInfluence()))
				.map(configurationVoteType -> {
					final String voteIdentification = configurationVoteType.getVoteIdentification();
					final VoteType voteType = new VoteType();
					voteType.setVoteIdentification(voteIdentification);

					// StandardBallot
					voteType.getQuestion()
							.addAll(toStandardBallotQuestions(configurationVoteType, encodedVotingOptionToChoiceCodeMap, primesMappingTable));

					// VariantBallot -> StandardQuestion && VariantBallot -> TieBreakQuestion
					final List<VariantBallotType> variantBallotTypeList = configurationVoteType.getBallot().stream().parallel()
							.map(BallotType::getVariantBallot)
							.filter(Objects::nonNull)
							.toList();

					voteType.getQuestion()
							.addAll(toVariantBallotStandardQuestions(variantBallotTypeList, primesMappingTable,
									encodedVotingOptionToChoiceCodeMap));
					voteType.getQuestion()
							.addAll(toVariantBallotTieBreakQuestions(variantBallotTypeList, primesMappingTable,
									encodedVotingOptionToChoiceCodeMap));

					return voteType;
				})
				.toList();
	}

	private static List<QuestionType> toStandardBallotQuestions(
			final ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteType configurationVoteType,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap, final PrimesMappingTable primesMappingTable) {
		return configurationVoteType.getBallot().stream().parallel()
				.map(BallotType::getStandardBallot)
				.filter(Objects::nonNull)
				.map(standardBallotType -> {
					final List<AnswerType> answers = standardBallotType.getAnswer().stream()
							.map(standardAnswerType -> {
								final String actualVotingOption = getAnswerActualVotingOption(standardBallotType.getQuestionIdentification(),
										standardAnswerType.getAnswerIdentification());
								final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable,
										encodedVotingOptionToChoiceCodeMap)
										.orElseThrow(() -> new IllegalStateException("Standard ballot answer choice code must not be null"));
								return new AnswerType()
										.withAnswerIdentification(standardAnswerType.getAnswerIdentification())
										.withChoiceReturnCode(choiceCode);
							})
							.toList();

					return new QuestionType()
							.withQuestionIdentification(standardBallotType.getQuestionIdentification())
							.withAnswer(answers);
				})
				.toList();
	}

	private static List<QuestionType> toVariantBallotStandardQuestions(final List<VariantBallotType> variantBallotTypeList,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		return variantBallotTypeList.stream()
				.map(VariantBallotType::getStandardQuestion)
				.flatMap(Collection::stream)
				.map(standardQuestionType -> {
					final List<AnswerType> answers = standardQuestionType.getAnswer().stream()
							.map(standardAnswerType -> {
								final String actualVotingOption = getAnswerActualVotingOption(standardQuestionType.getQuestionIdentification(),
										standardAnswerType.getAnswerIdentification());
								final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable,
										encodedVotingOptionToChoiceCodeMap)
										.orElseThrow(() -> new IllegalStateException("Variant ballot standard answer choice code must not be null"));

								return new AnswerType()
										.withAnswerIdentification(standardAnswerType.getAnswerIdentification())
										.withChoiceReturnCode(choiceCode);
							})
							.toList();

					return new QuestionType()
							.withQuestionIdentification(standardQuestionType.getQuestionIdentification())
							.withAnswer(answers);
				})
				.toList();
	}

	private static List<QuestionType> toVariantBallotTieBreakQuestions(final List<VariantBallotType> variantBallotTypeList,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		return variantBallotTypeList.stream()
				.map(VariantBallotType::getTieBreakQuestion)
				.flatMap(Collection::stream)
				.map(tieBreakQuestionType -> {
					final List<AnswerType> answers = tieBreakQuestionType.getAnswer().stream()
							.map(tiebreakAnswerType -> {
								final String actualVotingOption = getAnswerActualVotingOption(tieBreakQuestionType.getQuestionIdentification(),
										tiebreakAnswerType.getAnswerIdentification());
								final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable,
										encodedVotingOptionToChoiceCodeMap)
										.orElseThrow(() -> new IllegalStateException("Variant ballot tiebreak answer choice code must not be null"));

								return new AnswerType()
										.withAnswerIdentification(tiebreakAnswerType.getAnswerIdentification())
										.withChoiceReturnCode(choiceCode);
							})
							.toList();

					return new QuestionType()
							.withQuestionIdentification(tieBreakQuestionType.getQuestionIdentification())
							.withAnswer(answers);
				})
				.toList();
	}

	private static List<ElectionType> toElections(final List<ElectionInformationTypeExtended> electionInformationTypeExtendedList,
			final List<String> domainOfInfluenceList, final PrimesMappingTable primesMappingTable,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {

		return electionInformationTypeExtendedList.stream().parallel()
				.filter(configurationElectionInformationTypeExtended -> domainOfInfluenceList.contains(
						configurationElectionInformationTypeExtended.domainOfInfluence))
				.map(configurationElectionInformationTypeExtended -> {
					final ElectionInformationType configurationElectionInformationType = configurationElectionInformationTypeExtended.electionInformationType;
					final ElectionType electionType = new ElectionType();
					electionType.setElectionIdentification(configurationElectionInformationType.getElection().getElectionIdentification());

					// Candidates
					// Only for elections without defined lists
					if (configurationElectionInformationType.getList().isEmpty()) {
						electionType.getCandidate()
								.addAll(toCandidates(configurationElectionInformationType, primesMappingTable, encodedVotingOptionToChoiceCodeMap));
					}

					// Lists
					electionType.getList()
							.addAll(toLists(configurationElectionInformationType, primesMappingTable, encodedVotingOptionToChoiceCodeMap));

					// Empty list
					electionType.withEmptyList(
							toEmptyList(configurationElectionInformationType, primesMappingTable, encodedVotingOptionToChoiceCodeMap));

					// Write-ins
					if (configurationElectionInformationType.getElection().isWriteInsAllowed()) {
						electionType.getWriteInPosition()
								.addAll(toWriteInsChoiceCodes(configurationElectionInformationType.getElection().getElectionIdentification(),
										primesMappingTable, encodedVotingOptionToChoiceCodeMap, configurationElectionInformationType));
					}

					return electionType;
				})
				.toList();
	}

	private static List<CandidateType> toCandidates(final ElectionInformationType configurationElectionInformationType,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		final String electionIdentification = configurationElectionInformationType.getElection().getElectionIdentification();
		return configurationElectionInformationType.getCandidate().stream()
				.map(configurationCandidateType -> {
					final List<String> choiceCodes = IntStream.range(0,
									configurationElectionInformationType.getElection().getCandidateAccumulation().intValue())
							.mapToObj(acc -> getCandidateActualVotingOption(electionIdentification,
									configurationCandidateType.getCandidateIdentification(), acc))
							.map(actualVotingOption -> {
								final List<String> choiceCodeList = getChoiceCodeListFromActualVotingOption(
										actualVotingOption, primesMappingTable, encodedVotingOptionToChoiceCodeMap);

								checkState(!choiceCodeList.isEmpty(), "Candidate choice code list must not be empty");

								return choiceCodeList;
							})
							.flatMap(List::stream).toList();

					return new CandidateType()
							.withCandidateIdentification(configurationCandidateType.getCandidateIdentification())
							.withChoiceReturnCode(choiceCodes);
				})
				.filter(candidateType -> !candidateType.getChoiceReturnCode().isEmpty())
				.toList();
	}

	private static List<ListType> toLists(final ElectionInformationType configurationElectionInformationType,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		final String electionIdentification = configurationElectionInformationType.getElection().getElectionIdentification();
		return configurationElectionInformationType.getList().stream()
				.map(configListType -> {
					final String actualVotingOption = getListActualVotingOption(electionIdentification, configListType.getListIdentification());
					final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable,
							encodedVotingOptionToChoiceCodeMap)
							.orElse(null);

					final List<CandidateListType> candidateListTypes = toCandidateLists(configListType.getCandidatePosition(), primesMappingTable,
							encodedVotingOptionToChoiceCodeMap, electionIdentification, configurationElectionInformationType.getElection());
					return new ListType()
							.withListIdentification(configListType.getListIdentification())
							.withChoiceReturnCode(choiceCode)
							.withCandidate(candidateListTypes);
				})
				.toList();
	}

	private static EmptyListType toEmptyList(final ElectionInformationType configurationElectionInformationType,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {

		final ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.EmptyListType configEmptyList = configurationElectionInformationType.getEmptyList();
		final String electionIdentification = configurationElectionInformationType.getElection().getElectionIdentification();

		final List<ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.EmptyPositionType> emptyPositions = toEmptyPositions(
				configEmptyList.getEmptyPosition(), primesMappingTable, encodedVotingOptionToChoiceCodeMap, electionIdentification);

		final String actualVotingOption = getListActualVotingOption(electionIdentification, configEmptyList.getListIdentification());
		final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable, encodedVotingOptionToChoiceCodeMap)
				.orElse(null);

		return new EmptyListType()
				.withEmptyListIdentification(configEmptyList.getListIdentification())
				.withChoiceReturnCode(choiceCode)
				.withEmptyPosition(emptyPositions);
	}

	private static List<WriteInPositionType> toWriteInsChoiceCodes(final String electionIdentification,
			final PrimesMappingTable primesMappingTable,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap,
			final ElectionInformationType configurationElectionInformationType) {

		// find all write-in aliases of this election
		final ImmutableSet<String> allWriteInAliases = primesMappingTable.pTable().stream().parallel()
				.filter(primesMappingTableEntry -> primesMappingTableEntry.semanticInformation().startsWith(VotingOptionType.WRITE_IN.name()))
				.map(PrimesMappingTableEntry::actualVotingOption)
				.filter(actualVotingOption -> actualVotingOption.startsWith(electionIdentification))
				.collect(toImmutableSet());
		checkState(allWriteInAliases.size() == configurationElectionInformationType.getWriteInPosition().size(),
				"The number of write-in aliases must be equal to the number of write-in candidates for the election. [electionIdentification: %s]",
				electionIdentification);

		return configurationElectionInformationType.getWriteInPosition().stream().parallel()
				.sorted(Comparator.comparing(ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.WriteInPositionType::getPosition))
				.map(ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.WriteInPositionType::getWriteInPositionIdentification)
				.map(writeInPositionIdentification -> {
					final String writeInAlias = getWriteInPositionActualVotingOption(electionIdentification, writeInPositionIdentification);
					checkState(allWriteInAliases.contains(writeInAlias),
							"At least one write-in position does not have an alias. [writeInAliasNotFound: %s]", writeInAlias);

					final String choiceCode = getChoiceCodeFromActualVotingOption(writeInAlias, primesMappingTable,
							encodedVotingOptionToChoiceCodeMap)
							.orElseThrow(() -> new IllegalStateException(
									String.format("The write-in position does not have a corresponding a choice code. [writeInAlias: %s]",
											writeInAlias)));
					return new WriteInPositionType()
							.withWriteInPositionIdentification(writeInPositionIdentification)
							.withChoiceReturnCode(choiceCode);
				})
				.toList();
	}

	private static List<CandidateListType> toCandidateLists(final List<CandidatePositionType> candidatePositionTypeList,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap,
			final String electionIdentification, final ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionType electionType) {
		return candidatePositionTypeList.stream()
				.map(candidatePositionType ->
				{
					final List<String> choiceCodes = IntStream.range(0, electionType.getCandidateAccumulation().intValue())
							.mapToObj(acc ->
									getCandidateActualVotingOption(electionIdentification, candidatePositionType.getCandidateIdentification(), acc))
							.map(actualVotingOption -> {
								final List<String> choiceCodeList = getChoiceCodeListFromActualVotingOption(
										actualVotingOption, primesMappingTable, encodedVotingOptionToChoiceCodeMap);

								checkState(!choiceCodeList.isEmpty(), "CandidateList choice code list must not be empty");

								return choiceCodeList;
							})
							.flatMap(List::stream)
							.toList();
					return new CandidateListType()
							.withCandidateListIdentification(candidatePositionType.getCandidateListIdentification())
							.withChoiceReturnCode(choiceCodes);
				})
				.toList();
	}

	private static List<ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.EmptyPositionType> toEmptyPositions(
			final List<EmptyPositionType> emptyPositions, final PrimesMappingTable primesMappingTable,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap,
			final String electionIdentification) {

		return emptyPositions.stream()
				.map(EmptyPositionType::getEmptyPositionIdentification)
				.map(candidateListIdentification -> {
					final String actualVotingOption = getEmptyPositionActualVotingOption(electionIdentification, candidateListIdentification);
					final String choiceCode = getChoiceCodeFromActualVotingOption(actualVotingOption, primesMappingTable,
							encodedVotingOptionToChoiceCodeMap)
							.orElseThrow(() -> new IllegalStateException(String.format(
									"The empty list position does not have a corresponding a choice code. [candidateListIdentification: %s]",
									candidateListIdentification)));
					return new ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.EmptyPositionType()
							.withEmptyPositionIdentification(candidateListIdentification)
							.withChoiceReturnCode(choiceCode);
				})
				.toList();
	}

	private static Optional<String> getChoiceCodeFromActualVotingOption(final String actualVotingOption, final PrimesMappingTable primesMappingTable,
			final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {
		final List<String> choiceCodes = primesMappingTable.pTable()
				.stream()
				.filter(primesMappingTableEntry -> primesMappingTableEntry.actualVotingOption().equals(actualVotingOption))
				.map(PrimesMappingTableEntry::encodedVotingOption)
				.map(encodedVotingOptionToChoiceCodeMap::get)
				.toList();

		if (choiceCodes.size() > 1) {
			throw new IllegalStateException(
					String.format("Unexpected number of choice code. [expectedNumber: 1, actualNumber: %s, actualVotingOption: %s]",
							choiceCodes.size(), actualVotingOption));
		}

		if (choiceCodes.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(choiceCodes.getFirst());
	}

	private static List<String> getChoiceCodeListFromActualVotingOption(final String actualVotingOption,
			final PrimesMappingTable primesMappingTable, final ImmutableMap<PrimeGqElement, String> encodedVotingOptionToChoiceCodeMap) {

		return primesMappingTable.pTable()
				.stream()
				.filter(primesMappingTableEntry -> primesMappingTableEntry.actualVotingOption().equals(actualVotingOption))
				.map(PrimesMappingTableEntry::encodedVotingOption)
				.map(encodedVotingOptionToChoiceCodeMap::get)
				.toList();
	}

	private record ElectionInformationTypeExtended(ElectionInformationType electionInformationType, String domainOfInfluence) {
	}
}
