/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Predicate.not;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the information of all the ballot boxes. The total counts are computed with the getters.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "electionEventId", "ballotBoxesInformation", "totalICHTest", "totalICH", "totalACHTest", "totalACH", "totalForeignerTest",
		"totalForeigner" })
@JsonDeserialize(using = BallotBoxesReportDeserializer.class)
public record BallotBoxesReport(String electionEventId, ImmutableList<BallotBoxInformation> ballotBoxesInformation) {

	/**
	 * Constructs the Ballot Boxes report.
	 *
	 * @param electionEventId        the election event id. Must be non-null and a valid UUID.
	 * @param ballotBoxesInformation the information of all ballot boxes. Must be non-null, non-empty and not contain any null element.
	 * @throws NullPointerException      if any parameter is null or {@code ballotBoxesInformation} contains any null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalArgumentException  if {@code ballotBoxesInformation} is empty.
	 */
	public BallotBoxesReport {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxesInformation);
		checkArgument(!ballotBoxesInformation.isEmpty(), "The ballotBoxes must not be empty.");
	}

	@JsonSerialize
	public int totalICHTest() {
		return ballotBoxesInformation.stream().filter(BallotBoxInformation::isTest).mapToInt(BallotBoxInformation::countICH).sum();
	}

	@JsonProperty
	public int totalICH() {
		return ballotBoxesInformation.stream().filter(not(BallotBoxInformation::isTest)).mapToInt(
				BallotBoxInformation::countICH).sum();
	}

	@JsonProperty
	public int totalACHTest() {
		return ballotBoxesInformation.stream().filter(BallotBoxInformation::isTest).mapToInt(BallotBoxInformation::countACH).sum();
	}

	@JsonProperty
	public int totalACH() {
		return ballotBoxesInformation.stream().filter(not(BallotBoxInformation::isTest)).mapToInt(
				BallotBoxInformation::countACH).sum();
	}

	@JsonProperty
	public int totalForeignerTest() {
		return ballotBoxesInformation.stream().filter(BallotBoxInformation::isTest).mapToInt(BallotBoxInformation::countForeigner).sum();
	}

	@JsonProperty
	public int totalForeigner() {
		return ballotBoxesInformation.stream().filter(not(BallotBoxInformation::isTest)).mapToInt(
				BallotBoxInformation::countForeigner).sum();
	}

}
