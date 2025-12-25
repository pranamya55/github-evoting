/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure.PreconfigureSummary;

class ConfigurationSummaryTest {

	private String contestId;
	private ImmutableMap<String, String> contestDescription;
	private LocalDate contestDate;
	private String electionEventSeed;
	private LocalDateTime evotingFromDate;
	private LocalDateTime evotingToDate;
	private int gracePeriod;
	private int voterTotal;
	private ImmutableList<String> extendedAuthenticationType;
	private ElectoralBoardSummary electoralBoard;
	private ImmutableList<ElectionGroupSummary> electionGroups;
	private ImmutableList<VoteSummary> votes;
	private ImmutableList<AuthorizationSummary> authorizations;
	private PreconfigureSummary preconfigureSummary;
	private String configurationSignature;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ConfigurationSummary configurationSummary = summaryGenerator.generateConfigurationSummary();
		contestId = configurationSummary.getContestId();
		contestDescription = configurationSummary.getContestDescription();
		contestDate = configurationSummary.getContestDate();
		electionEventSeed = configurationSummary.getElectionEventSeed();
		evotingFromDate = configurationSummary.getEvotingFromDate();
		evotingToDate = configurationSummary.getEvotingToDate();
		gracePeriod = configurationSummary.getGracePeriod();
		voterTotal = configurationSummary.getVoterTotal();
		extendedAuthenticationType = configurationSummary.getExtendedAuthenticationType();
		electoralBoard = configurationSummary.getElectoralBoard();
		electionGroups = configurationSummary.getElectionGroups();
		votes = configurationSummary.getVotes();
		authorizations = configurationSummary.getAuthorizations();
		preconfigureSummary = configurationSummary.getPreconfigureSummary();
		configurationSignature = configurationSummary.getConfigurationSignature();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ConfigurationSummary.Builder()
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
				.build());
	}

	@Test
	@DisplayName("should create instance with null preconfigure summary")
	void shouldCreateInstanceWithNullPreconfigureSummary() {
		assertDoesNotThrow(() -> new ConfigurationSummary.Builder()
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
				.withConfigurationSignature(configurationSignature)
				.build());
	}

	@Test
	@DisplayName("should throw exception when contestId is null")
	void shouldThrowExceptionWhenContestIdIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(null)
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
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when contestDescription is null")
	void shouldThrowExceptionWhenContestDescriptionIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(null)
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
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when contestDate is null")
	void shouldThrowExceptionWhenContestDateIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(null)
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
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electionEventSeed is null")
	void shouldThrowExceptionWhenElectionEventSeedIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(null)
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
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when evotingFromDate is null")
	void shouldThrowExceptionWhenEvotingFromDateIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(null)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when evotingToDate is null")
	void shouldThrowExceptionWhenEvotingToDateIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(null)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when evotingFromDate is after evotingToDate")
	void shouldThrowExceptionWhenEvotingFromDateIsAfterEvotingToDate() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingToDate.plusSeconds(1))
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The evotingFromDate must not be after the evotingToDate.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when gracePeriod is negative")
	void shouldThrowExceptionWhenGracePeriodIsNegative() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(-40)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The grace period must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when voterTotal is zero")
	void shouldThrowExceptionWhenVoterTotalIsZero() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(0)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "There must be at least one voter.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when voterTotal is negative")
	void shouldThrowExceptionWhenVoterTotalIsNegative() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(-5)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "There must be at least one voter.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when extendedAuthenticationType is null")
	void shouldThrowExceptionWhenExtendedAuthenticationTypeIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(null)
				.withElectoralBoard(electoralBoard)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electoralBoard is null")
	void shouldThrowExceptionWhenElectoralBoardIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
				.withContestId(contestId)
				.withContestDescription(contestDescription)
				.withContestDate(contestDate)
				.withElectionEventSeed(electionEventSeed)
				.withEvotingFromDate(evotingFromDate)
				.withEvotingToDate(evotingToDate)
				.withGracePeriod(gracePeriod)
				.withVoterTotal(voterTotal)
				.withExtendedAuthenticationType(extendedAuthenticationType)
				.withElectoralBoard(null)
				.withElectionGroups(electionGroups)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electionGroups is null")
	void shouldThrowExceptionWhenElectionGroupsIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
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
				.withElectionGroups(null)
				.withVotes(votes)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when votes is null")
	void shouldThrowExceptionWhenVotesIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
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
				.withVotes(null)
				.withAuthorizations(authorizations)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when authorizations is null")
	void shouldThrowExceptionWhenAuthorizationsIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
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
				.withAuthorizations(null)
				.withPreconfigureSummary(preconfigureSummary)
				.withConfigurationSignature(configurationSignature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when configurationSignature is null")
	void shouldThrowExceptionWhenConfigurationSignatureIsNull() {
		final ConfigurationSummary.Builder builder = new ConfigurationSummary.Builder()
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
				.withConfigurationSignature(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}
