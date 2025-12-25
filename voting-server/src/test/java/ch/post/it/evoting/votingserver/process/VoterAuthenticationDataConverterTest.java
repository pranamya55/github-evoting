/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@DisplayName("VoterAuthenticationDataConverter calling")
class VoterAuthenticationDataConverterTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final VoterAuthenticationDataConverter converter = new VoterAuthenticationDataConverter(DomainObjectMapper.getNewInstance());

	private SetupComponentVoterAuthenticationData voterAuthenticationData;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		final String votingCardId = uuidGenerator.generate();
		final String credentialId = uuidGenerator.generate();

		final String baseAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		voterAuthenticationData = new SetupComponentVoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId,
				verificationCardId, votingCardId, credentialId, baseAuthenticationChallenge);
	}

	@Test
	@DisplayName("convert with null argument throws")
	void convertWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> converter.convertToDatabaseColumn(null));
		assertThrows(NullPointerException.class, () -> converter.convertToEntityAttribute(null));
	}

	@RepeatedTest(10)
	@DisplayName("convertToDatabaseColumn then convertToEntityAttribute gives original data")
	void cycle() {
		final byte[] databaseColumnBytes = converter.convertToDatabaseColumn(voterAuthenticationData);
		final SetupComponentVoterAuthenticationData cycledVoterAuthenticationData = converter.convertToEntityAttribute(databaseColumnBytes);
		assertEquals(voterAuthenticationData, cycledVoterAuthenticationData);
	}
}
