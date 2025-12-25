/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.LatinAlphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A BallotBoxesReport")
class BallotBoxesReportTest {

	private static final int NAME_LENGTH = 5;
	private static final int COUNT_UPPER_BOUND = 10;
	private static final int BALLOT_BOXES_UPPER_BOUND = 10;
	private static final Random RANDOM = RandomFactory.createRandom();
	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static final ObjectMapper OBJECT_MAPPER = DomainObjectMapper.getNewInstance();

	private ObjectNode rootNode;
	private String electionEventId;
	private BallotBoxesReport ballotBoxesReport;

	@BeforeEach
	void setUp() {
		electionEventId = UUID_GENERATOR.generate();
		final int nbrBallotBoxes = RANDOM.genRandomInteger(BALLOT_BOXES_UPPER_BOUND) + 1;

		final ImmutableList<BallotBoxInformation> ballotBoxesInformation = IntStream.range(0, nbrBallotBoxes)
				.mapToObj(i -> {
					final String name = RANDOM.genRandomString(NAME_LENGTH, LatinAlphabet.getInstance());
					final String verificationCardSetId = UUID_GENERATOR.generate();
					final boolean isTest = RANDOM.genRandomInteger(1) < 0.5;
					final int countICH = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);
					final int countACH = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);
					final int countForeigner = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);

					return new BallotBoxInformation(name, verificationCardSetId, isTest, countICH, countACH, countForeigner);
				})
				.collect(toImmutableList());

		ballotBoxesReport = new BallotBoxesReport(electionEventId, ballotBoxesInformation);

		// Expected json.
		rootNode = OBJECT_MAPPER.createObjectNode();
		rootNode.put("electionEventId", electionEventId);
		rootNode.set("ballotBoxesInformation", OBJECT_MAPPER.valueToTree(ballotBoxesInformation));
		rootNode.put("totalICHTest", ballotBoxesReport.totalICHTest());
		rootNode.put("totalICH", ballotBoxesReport.totalICH());
		rootNode.put("totalACHTest", ballotBoxesReport.totalACHTest());
		rootNode.put("totalACH", ballotBoxesReport.totalACH());
		rootNode.put("totalForeignerTest", ballotBoxesReport.totalForeignerTest());
		rootNode.put("totalForeigner", ballotBoxesReport.totalForeigner());
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serialize() throws JsonProcessingException {
		final String serializedBallotBoxesReport = OBJECT_MAPPER.writeValueAsString(ballotBoxesReport);

		assertEquals(rootNode.toString(), serializedBallotBoxesReport);
	}

	@Test
	@DisplayName("deserialized gives expected BallotBoxesReport")
	void deserialize() throws JsonProcessingException {
		final BallotBoxesReport deserializedBallotBoxesReport = OBJECT_MAPPER.readValue(rootNode.toString(), BallotBoxesReport.class);

		assertEquals(ballotBoxesReport, deserializedBallotBoxesReport);
	}

	@Test
	@DisplayName("serialized then deserialized gives original BallotBoxesReport")
	void cycle() throws JsonProcessingException {
		final String serializedBallotBoxesReport = OBJECT_MAPPER.writeValueAsString(ballotBoxesReport);
		final BallotBoxesReport deserializedBallotBoxesReport = OBJECT_MAPPER.readValue(serializedBallotBoxesReport, BallotBoxesReport.class);

		assertEquals(ballotBoxesReport, deserializedBallotBoxesReport);
	}

	@Nested
	@DisplayName("constructed with")
	class ConstructorTest {

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void nullParameterThrows() {
			final ImmutableList<BallotBoxInformation> ballotBoxesInformation = ballotBoxesReport.ballotBoxesInformation();

			assertThrows(NullPointerException.class, () -> new BallotBoxesReport(null, ballotBoxesInformation));
			assertThrows(NullPointerException.class, () -> new BallotBoxesReport(electionEventId, null));
		}

		@Test
		@DisplayName("invalid electionEventId throws FailedValidationException")
		void invalidElectionEventIdThrows() {
			final String invalidElectionEventId = "invalid";
			final ImmutableList<BallotBoxInformation> ballotBoxesInformation = ballotBoxesReport.ballotBoxesInformation();

			assertThrows(FailedValidationException.class, () -> new BallotBoxesReport(invalidElectionEventId, ballotBoxesInformation));
		}

		@Test
		@DisplayName("empty ballotBoxesInformation throws IllegalArgumentException")
		void emptyBallotBoxesInformationThrows() {
			final ImmutableList<BallotBoxInformation> emptyBallotBoxesInformation = ImmutableList.emptyList();

			assertThrows(IllegalArgumentException.class, () -> new BallotBoxesReport(electionEventId, emptyBallotBoxesInformation));
		}

	}

}