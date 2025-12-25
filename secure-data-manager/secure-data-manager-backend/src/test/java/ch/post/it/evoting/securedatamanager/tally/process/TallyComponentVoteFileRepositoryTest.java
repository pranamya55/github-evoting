/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class TallyComponentVoteFileRepositoryTest {

	private static final TallyPathResolver pathResolverMock = mock(TallyPathResolver.class);
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static TallyComponentVotesFileRepository tallyComponentVotesFileRepository;

	private final GqGroup gqGroup = GroupTestData.getGroupP59();
	private final String ballotBoxId = uuidGenerator.generate();
	private final String electionEventId = uuidGenerator.generate();
	private final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray("".getBytes(StandardCharsets.UTF_8)));

	private GroupVector<GroupVector<PrimeGqElement, GqGroup>, GqGroup> decryptedVotes;
	private ImmutableList<ImmutableList<String>> decodedVotes;

	private TallyComponentVotesPayload tallyComponentVotesPayload;

	@BeforeAll
	static void setUpAll() {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

		tallyComponentVotesFileRepository = new TallyComponentVotesFileRepository(pathResolverMock, objectMapper);
	}

	@BeforeEach
	void setUp() {
		final int desiredNumberOfPrimes = 3;
		final GroupVector<PrimeGqElement, GqGroup> primeGroupMembers =
				PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers(gqGroup, desiredNumberOfPrimes);

		decryptedVotes = GroupVector.of(GroupVector.from(primeGroupMembers), GroupVector.from(primeGroupMembers));
		decodedVotes = decryptedVotes.stream()
				.map(decryptedVote -> decryptedVote.stream()
						.map(v -> "124124aa|154153")
						.collect(toImmutableList()))
				.collect(toImmutableList());

		final ImmutableList<ImmutableList<String>> decodedWriteIns = decryptedVotes.stream()
				.map(actualSelectedVotingOption -> actualSelectedVotingOption.stream()
						.map(v -> "James Bond")
						.collect(toImmutableList()))
				.collect(toImmutableList());

		tallyComponentVotesPayload = new TallyComponentVotesPayload(decryptedVotes.getGroup(), electionEventId, ballotBoxId, decryptedVotes,
				decodedVotes, decodedWriteIns, signature);
	}

	@Test
	void persistTallyComponentVotesTest(
			@TempDir
			final Path tempDir) {
		when(pathResolverMock.resolveBallotBoxPath(anyString(), anyString())).thenReturn(tempDir);

		assertEquals(Optional.empty(), tallyComponentVotesFileRepository.load(electionEventId, ballotBoxId));

		assertDoesNotThrow(() -> tallyComponentVotesFileRepository.save(tallyComponentVotesPayload));

		final Optional<TallyComponentVotesPayload> optionalPayload = tallyComponentVotesFileRepository.load(electionEventId, ballotBoxId);
		assertTrue(optionalPayload.isPresent());

		final TallyComponentVotesPayload payload = optionalPayload.get();
		assertNotNull(payload);
		assertAll(
				() -> assertEquals(electionEventId, payload.getElectionEventId()),
				() -> assertEquals(ballotBoxId, payload.getBallotBoxId()),
				() -> assertEquals(decryptedVotes.getGroup(), payload.getEncryptionGroup()),
				() -> assertEquals(decryptedVotes.size(), payload.getDecryptedVotes().size()),
				() -> assertEquals(decodedVotes, payload.getDecodedVotes()),
				() -> assertEquals(signature, payload.getSignature())
		);
	}

	@Test
	void persistTallyComponentVotesNullTest() {
		assertThrows(NullPointerException.class, () -> tallyComponentVotesFileRepository.save(null));
	}

	@Test
	void loadTallyComponentVotesNullTest() {
		assertAll(
				() -> assertThrows(NullPointerException.class, () -> tallyComponentVotesFileRepository.load(null, ballotBoxId)),
				() -> assertThrows(NullPointerException.class, () -> tallyComponentVotesFileRepository.load(electionEventId, null))
		);
	}

	@Test
	void loadTallyComponentVotesInvalidUUIDTest() {
		assertAll(
				() -> assertThrows(FailedValidationException.class, () -> tallyComponentVotesFileRepository.load("123", ballotBoxId)),
				() -> assertThrows(FailedValidationException.class, () -> tallyComponentVotesFileRepository.load(electionEventId, "123"))
		);

	}
}
