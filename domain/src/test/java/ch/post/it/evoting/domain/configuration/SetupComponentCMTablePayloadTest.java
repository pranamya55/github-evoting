/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class SetupComponentCMTablePayloadTest {

	private static final Random RANDOM = RandomFactory.createRandom();
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	CryptoPrimitivesSignature signature;
	private String electionEventId;
	private String verificationCardSetId;
	private int chunkId;
	private ImmutableMap<String, String> returnCodesMappingTable;

	@BeforeEach
	void setup() {
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();

		chunkId = SECURE_RANDOM.nextInt(100);
		returnCodesMappingTable = ImmutableMap.emptyMap();
		signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray(RANDOM.genRandomString(100, base64Alphabet).getBytes(StandardCharsets.UTF_8)));
	}

	private static Stream<Arguments> nullArgumentProvider() {
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final int chunkId = SECURE_RANDOM.nextInt(100);
		final ImmutableMap<String, String> returnCodesMappingTable = ImmutableMap.emptyMap();
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray(RANDOM.genRandomString(100, base64Alphabet).getBytes(StandardCharsets.UTF_8)));

		return Stream.of(
				Arguments.of(null, verificationCardSetId, chunkId, returnCodesMappingTable, signature),
				Arguments.of(electionEventId, null, chunkId, returnCodesMappingTable, signature),
				Arguments.of(electionEventId, verificationCardSetId, chunkId, null, signature)
		);
	}

	@ParameterizedTest
	@MethodSource("nullArgumentProvider")
	void buildWithNullArgumentsThrows(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final ImmutableMap<String, String> returnCodesMappingTable, final CryptoPrimitivesSignature signature) {
		final SetupComponentCMTablePayload.Builder builder = new SetupComponentCMTablePayload.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setReturnCodesMappingTable(returnCodesMappingTable)
				.setSignature(signature);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void buildWithNonUuidArgumentsThrows() {
		final SetupComponentCMTablePayload.Builder builder1 = new SetupComponentCMTablePayload.Builder()
				.setElectionEventId("nonUUID")
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setReturnCodesMappingTable(returnCodesMappingTable)
				.setSignature(signature);
		assertThrows(FailedValidationException.class, builder1::build);

		final SetupComponentCMTablePayload.Builder builder2 = new SetupComponentCMTablePayload.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId("nonUUID")
				.setChunkId(chunkId)
				.setReturnCodesMappingTable(returnCodesMappingTable)
				.setSignature(signature);
		assertThrows(FailedValidationException.class, builder2::build);
	}

	@Test
	void buildWithNegativeChunkIdThrows() {
		final SetupComponentCMTablePayload.Builder builder = new SetupComponentCMTablePayload.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(-1)
				.setReturnCodesMappingTable(returnCodesMappingTable)
				.setSignature(signature);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("The chunkId must be positive.", Throwables.getRootCause(exception).getMessage());
	}
}
