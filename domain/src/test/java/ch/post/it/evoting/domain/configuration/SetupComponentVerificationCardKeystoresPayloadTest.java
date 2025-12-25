/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("A SetupComponentVerificationCardKeystoresPayload")
class SetupComponentVerificationCardKeystoresPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();

		final Random random = RandomFactory.createRandom();
		final Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final ImmutableList<VerificationCardKeystore> verificationCardKeystores = ImmutableList.of(
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet)))
		);

		setupComponentVerificationCardKeystoresPayload = new SetupComponentVerificationCardKeystoresPayload(electionEventId, verificationCardSetId,
				verificationCardKeystores);

		final Hash hash = HashFactory.createHash();
		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentVerificationCardKeystoresPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentVerificationCardKeystoresPayload.setSignature(signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(electionEventId)));
		rootNode.set("verificationCardSetId", mapper.readTree(mapper.writeValueAsString(verificationCardSetId)));
		rootNode.set("verificationCardKeystores", mapper.readTree(mapper.writeValueAsString(verificationCardKeystores)));
		rootNode.set("signature", mapper.readTree(mapper.writeValueAsString(signature)));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(setupComponentVerificationCardKeystoresPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final SetupComponentVerificationCardKeystoresPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				SetupComponentVerificationCardKeystoresPayload.class);
		assertEquals(setupComponentVerificationCardKeystoresPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final SetupComponentVerificationCardKeystoresPayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(setupComponentVerificationCardKeystoresPayload),
						SetupComponentVerificationCardKeystoresPayload.class);

		assertEquals(setupComponentVerificationCardKeystoresPayload, deserializedPayload);
	}
}
