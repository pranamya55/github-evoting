/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;

class VoteSummaryTest {

	private String voteId;
	private int votePosition;
	private ImmutableMap<String, String> voteDescription;
	private String domainOfInfluence;
	private ImmutableList<AuthorizationSummary> authorizations;
	private ImmutableList<BallotSummary> ballots;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final VoteSummary voteSummary = summaryGenerator.generateVoteSummary();
		voteId = voteSummary.voteId();
		votePosition = voteSummary.votePosition();
		voteDescription = voteSummary.voteDescription();
		domainOfInfluence = voteSummary.domainOfInfluence();
		authorizations = voteSummary.authorizations();
		ballots = voteSummary.ballots();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new VoteSummary(voteId, votePosition, voteDescription, domainOfInfluence, authorizations, ballots));
	}

	@Test
	@DisplayName("should throw exception when voteId is null")
	void shouldThrowExceptionWhenVoteIdIsNull() {
		assertThrows(NullPointerException.class,
				() -> new VoteSummary(null, votePosition, voteDescription, domainOfInfluence, authorizations, ballots));
	}

	@Test
	@DisplayName("should throw exception when votePosition is negative")
	void shouldThrowExceptionWhenVotePositionIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VoteSummary(voteId, -2, voteDescription, domainOfInfluence, authorizations, ballots));

		final String expected = "The vote position must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when voteDescription is null")
	void shouldThrowExceptionWhenVoteDescriptionIsNull() {
		assertThrows(NullPointerException.class,
				() -> new VoteSummary(voteId, votePosition, null, domainOfInfluence, authorizations, ballots));
	}

	@Test
	@DisplayName("should throw exception when domainOfInfluence is null")
	void shouldThrowExceptionWhenDomainOfInfluenceIsNull() {
		assertThrows(NullPointerException.class,
				() -> new VoteSummary(voteId, votePosition, voteDescription, null, authorizations, ballots));
	}

	@Test
	@DisplayName("should throw exception when authorizations is null")
	void shouldThrowExceptionWhenAuthorizationsIsNull() {
		assertThrows(NullPointerException.class,
				() -> new VoteSummary(voteId, votePosition, voteDescription, domainOfInfluence, null, ballots));
	}

	@Test
	@DisplayName("should throw exception when ballots is null")
	void shouldThrowExceptionWhenBallotsIsNull() {
		assertThrows(NullPointerException.class,
				() -> new VoteSummary(voteId, votePosition, voteDescription, domainOfInfluence, authorizations, null));
	}
}