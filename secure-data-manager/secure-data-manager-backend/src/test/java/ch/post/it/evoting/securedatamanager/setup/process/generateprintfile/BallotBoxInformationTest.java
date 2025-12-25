/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.math.LatinAlphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A BallotBoxInformation")
class BallotBoxInformationTest {

	private static final int NAME_LENGTH = 5;
	private static final int COUNT_UPPER_BOUND = 10;
	private static final Random RANDOM = RandomFactory.createRandom();
	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static final ObjectMapper OBJECT_MAPPER = DomainObjectMapper.getNewInstance();

	private ObjectNode rootNode;
	private BallotBoxInformation ballotBoxInformation;

	@BeforeEach
	void setUp() {
		final String name = RANDOM.genRandomString(NAME_LENGTH, LatinAlphabet.getInstance());
		final String verificationCardSetId = UUID_GENERATOR.generate();
		final boolean isTest = RANDOM.genRandomInteger(1) < 0.5;
		final int countICH = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);
		final int countACH = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);
		final int countForeigner = RANDOM.genRandomInteger(COUNT_UPPER_BOUND);

		ballotBoxInformation = new BallotBoxInformation(name, verificationCardSetId, isTest, countICH, countACH, countForeigner);

		// Expected json.
		rootNode = OBJECT_MAPPER.createObjectNode();
		rootNode.put("name", name);
		rootNode.put("verificationCardSetId", verificationCardSetId);
		rootNode.put("isTest", isTest);
		rootNode.put("countICH", countICH);
		rootNode.put("countACH", countACH);
		rootNode.put("countForeigner", countForeigner);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serialize() throws JsonProcessingException {
		final String serializedBallotBoxInformation = OBJECT_MAPPER.writeValueAsString(ballotBoxInformation);

		assertEquals(rootNode.toString(), serializedBallotBoxInformation);
	}

	@Test
	@DisplayName("deserialized gives expected BallotBoxInformation")
	void deserialize() throws JsonProcessingException {
		final BallotBoxInformation deserializedBallotBoxInformation = OBJECT_MAPPER.readValue(rootNode.toString(), BallotBoxInformation.class);

		assertEquals(ballotBoxInformation, deserializedBallotBoxInformation);
	}

	@Test
	@DisplayName("serialized and deserialized gives original BallotBoxInformation")
	void cycle() throws JsonProcessingException {
		final String serializedBallotBoxInformation = OBJECT_MAPPER.writeValueAsString(ballotBoxInformation);
		final BallotBoxInformation deserializedBallotBoxInformation = OBJECT_MAPPER.readValue(serializedBallotBoxInformation,
				BallotBoxInformation.class);

		assertEquals(ballotBoxInformation, deserializedBallotBoxInformation);
	}

	@Nested
	@DisplayName("constructed with")
	class ConstructorTest {

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void nullParameterThrows() {
			final String name = ballotBoxInformation.name();
			final String verificationCardSetId = ballotBoxInformation.verificationCardSetId();
			final boolean isTest = ballotBoxInformation.isTest();
			final int countICH = ballotBoxInformation.countICH();
			final int countACH = ballotBoxInformation.countACH();
			final int countForeigner = ballotBoxInformation.countForeigner();

			assertThrows(NullPointerException.class,
					() -> new BallotBoxInformation(null, verificationCardSetId, isTest, countICH, countACH, countForeigner));
			assertThrows(NullPointerException.class, () -> new BallotBoxInformation(name, null, isTest, countICH, countACH, countForeigner));
		}

		@Test
		@DisplayName("invalid verificationCardSetId throws FailedValidationException")
		void invalidVerificationCardSetIdThrows() {
			final String name = ballotBoxInformation.name();
			final boolean isTest = ballotBoxInformation.isTest();
			final int countICH = ballotBoxInformation.countICH();
			final int countACH = ballotBoxInformation.countACH();
			final int countForeigner = ballotBoxInformation.countForeigner();

			assertThrows(FailedValidationException.class,
					() -> new BallotBoxInformation(name, "invalid", isTest, countICH, countACH, countForeigner));
		}

		@Test
		@DisplayName("negative any negative count throws IllegalArgumentException")
		void negativeCountThrows() {
			final String name = ballotBoxInformation.name();
			final String verificationCardSetId = ballotBoxInformation.verificationCardSetId();
			final boolean isTest = ballotBoxInformation.isTest();
			final int countICH = ballotBoxInformation.countICH();
			final int countACH = ballotBoxInformation.countACH();
			final int countForeigner = ballotBoxInformation.countForeigner();

			assertAll(
					() -> assertThrows(IllegalArgumentException.class,
							() -> new BallotBoxInformation(name, verificationCardSetId, isTest, -1, countACH, countForeigner)),
					() -> assertThrows(IllegalArgumentException.class,
							() -> new BallotBoxInformation(name, verificationCardSetId, isTest, countICH, -1, countForeigner)),
					() -> assertThrows(IllegalArgumentException.class,
							() -> new BallotBoxInformation(name, verificationCardSetId, isTest, countICH, countACH, -1))
			);
		}

	}

}