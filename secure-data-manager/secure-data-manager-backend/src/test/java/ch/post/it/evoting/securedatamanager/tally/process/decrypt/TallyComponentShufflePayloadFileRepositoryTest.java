/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.evotinglibraries.domain.SerializationTestData;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.TallyComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.VerifiablePlaintextDecryption;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.tally.process.TallyPathResolver;

@DisplayName("Use TallyComponentShufflePayloadPersistenceService to ")
class TallyComponentShufflePayloadFileRepositoryTest {

	private static final String FILE_NAME = "tallyComponentShufflePayload_4";
	private static final String ELECTION_EVENT_ID = "F28493B098604663B6A6969F53F51B56";
	private static final String BALLOT_BOX_ID = "672B1B49ED0341A3BAA953D611B90B74";

	private static TallyComponentShufflePayload expectedTallyComponentShufflePayload;
	private static TallyComponentShufflePayloadFileRepository tallyComponentShufflePayloadFileRepository;
	private static TallyPathResolver payloadResolver;

	@TempDir
	Path tempDir;

	@BeforeAll
	static void setUpAll() {
		// Create ciphertexts list.
		final GqGroup gqGroup = SerializationTestData.getGqGroup();
		final int nbrMessage = 4;

		final VerifiableShuffle verifiableShuffle = SerializationTestData.getVerifiableShuffle(nbrMessage);
		final VerifiablePlaintextDecryption verifiablePlaintextDecryption = SerializationTestData.getVerifiablePlaintextDecryption(nbrMessage);

		// Generate random bytes for signature content and create payload signature.
		final byte[] randomBytes = new byte[10];
		new SecureRandom().nextBytes(randomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(randomBytes));

		expectedTallyComponentShufflePayload = new TallyComponentShufflePayload(gqGroup, ELECTION_EVENT_ID, BALLOT_BOX_ID, verifiableShuffle,
				verifiablePlaintextDecryption, signature);

		payloadResolver = Mockito.mock(TallyPathResolver.class);
		tallyComponentShufflePayloadFileRepository = new TallyComponentShufflePayloadFileRepository(FILE_NAME, DomainObjectMapper.getNewInstance(),
				payloadResolver);
	}

	@Test
	@DisplayName("read TallyComponentShufflePayload file")
	void readMixnetPayload() {
		// Mock payloadResolver path and write payload
		when(payloadResolver.resolveBallotBoxPath(ELECTION_EVENT_ID, BALLOT_BOX_ID)).thenReturn(tempDir);
		tallyComponentShufflePayloadFileRepository.savePayload(ELECTION_EVENT_ID, BALLOT_BOX_ID, expectedTallyComponentShufflePayload);

		// Read payload and check
		final TallyComponentShufflePayload actualMixnetPayload = tallyComponentShufflePayloadFileRepository.getPayload(ELECTION_EVENT_ID,
				BALLOT_BOX_ID);

		assertEquals(expectedTallyComponentShufflePayload, actualMixnetPayload);
	}

	@Test
	@DisplayName("save TallyComponentShufflePayload file")
	void saveMixnetPayload() {
		// Mock payloadResolver path
		when(payloadResolver.resolveBallotBoxPath(ELECTION_EVENT_ID, BALLOT_BOX_ID)).thenReturn(tempDir);
		final Path payloadPath = tempDir.resolve(FILE_NAME + Constants.JSON);

		assertFalse(Files.exists(payloadPath), "The tally component shuffle payload file should not exist at this point");

		// Write payload
		assertNotNull(tallyComponentShufflePayloadFileRepository.savePayload(ELECTION_EVENT_ID, BALLOT_BOX_ID, expectedTallyComponentShufflePayload));

		assertTrue(Files.exists(payloadPath), "The tally component shuffle payload file should exist at this point");
	}

}
