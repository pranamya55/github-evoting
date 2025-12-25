/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class ControlComponenthlVCCSharePayloadTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();
	private final String verificationCardId = uuidGenerator.generate();
	private final String hashLongVoteCastCodeShare = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);

	@Test
	void serialisationDeserialisationCycleWorks() throws IOException {
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final int nodeId = 1;
		final int confirmationAttemptId = 0;
		final GqGroup encryptionGroup = GroupTestData.getGqGroup();
		final GqElement gqElement = new GqGroupGenerator(encryptionGroup).genMember();
		final ConfirmationKey confirmationKey = new ConfirmationKey(contextIds, gqElement);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray("randomSignatureContents".getBytes(StandardCharsets.UTF_8)));
		final ControlComponenthlVCCSharePayload responsePayload = new ControlComponenthlVCCSharePayload(encryptionGroup,
				nodeId, hashLongVoteCastCodeShare, confirmationKey, confirmationAttemptId, signature);
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final ControlComponenthlVCCSharePayload responsePayloadCycled = objectMapper.readValue(
				objectMapper.writeValueAsBytes(responsePayload), ControlComponenthlVCCSharePayload.class);
		assertEquals(responsePayload, responsePayloadCycled);

	}

	@Nested
	@DisplayName("Check constructor validation")
	class CheckConstructor {

		private final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		private final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray("signature".getBytes(StandardCharsets.UTF_8)));
		private final Integer nodeId = 1;
		private final int confirmationAttemptId = 0;

		private final GqGroup encryptionGroup = GroupTestData.getGqGroup();

		private final ConfirmationKey confirmationKey = new ConfirmationKey(contextIds, new GqGroupGenerator(encryptionGroup).genMember());

		@Test
		@DisplayName("Check null arguments")
		void nullArgs() {

			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> new ControlComponenthlVCCSharePayload(null, nodeId, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertThrows(NullPointerException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, null, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertThrows(NullPointerException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, hashLongVoteCastCodeShare, null,
									confirmationAttemptId, signature)),
					() -> assertThrows(NullPointerException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, null))
			);
		}

		@Test
		@DisplayName("Check hashLongVoteCastCodeShare in format Base64")
		void formatBase64() {
			final String validBase64Value = "Ax/6A+2w";
			final String invalidBase64Value = "$$";

			assertAll(
					() -> assertDoesNotThrow(
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, validBase64Value, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertThrows(FailedValidationException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, invalidBase64Value, confirmationKey,
									confirmationAttemptId, signature))
			);
		}

		@Test
		@DisplayName("Check nodeIds")
		void nodeIdArgs() {

			assertAll(
					() -> assertThrows(IllegalArgumentException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 0, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertDoesNotThrow(
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 1, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertDoesNotThrow(
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 2, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertDoesNotThrow(
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 3, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertDoesNotThrow(
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 4, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature)),
					() -> assertThrows(IllegalArgumentException.class,
							() -> new ControlComponenthlVCCSharePayload(encryptionGroup, 5, hashLongVoteCastCodeShare, confirmationKey,
									confirmationAttemptId, signature))
			);
		}
	}

}
