/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

class AuthenticationChallengeTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	@Test
	void test() throws JsonProcessingException {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String derivedVoterIdentifier = uuidGenerator.generate();
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final BigInteger nonce = random.genRandomInteger(TWO_POW_256);
		final AuthenticationChallenge authenticationChallenge = new AuthenticationChallenge(derivedVoterIdentifier, derivedAuthenticationChallenge,
				nonce);
		final String serialized = objectMapper.writeValueAsString(authenticationChallenge);
		assertNotNull(serialized);
	}
}
