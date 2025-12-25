/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class TallyComponentVotesServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String BALLOT_BOX_ID = uuidGenerator.generate();

	private static TallyComponentVotesService tallyComponentVotesService;
	private static GroupVector<GroupVector<PrimeGqElement, GqGroup>, GqGroup> votes;
	private static ImmutableList<ImmutableList<String>> decodedVotes;
	private static ImmutableList<ImmutableList<String>> decodedWriteIns;

	@BeforeAll
	static void setUp(
			@TempDir
			final Path tempDir) {

		final GqGroup gqGroup = GroupTestData.getGroupP59();

		final int desiredNumberOfPrimes = 3;
		final GroupVector<PrimeGqElement, GqGroup> primeGroupMembers = PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers(gqGroup,
				desiredNumberOfPrimes);

		votes = GroupVector.of(GroupVector.from(primeGroupMembers), GroupVector.from(primeGroupMembers));
		decodedVotes = votes.stream()
				.map(actualSelectedVotingOption -> actualSelectedVotingOption.stream()
						.map(v -> "124124aa|154153")
						.collect(toImmutableList()))
				.collect(toImmutableList());
		decodedWriteIns = votes.stream()
				.map(actualSelectedVotingOption -> actualSelectedVotingOption.stream()
						.map(v -> "James Bond")
						.collect(toImmutableList()))
				.collect(toImmutableList());

		final TallyPathResolver pathResolverMock = Mockito.mock(TallyPathResolver.class);
		when(pathResolverMock.resolveBallotBoxPath(anyString(), anyString())).thenReturn(tempDir);

		final TallyComponentVotesFileRepository tallyComponentVotesFileRepository = new TallyComponentVotesFileRepository(pathResolverMock,
				DomainObjectMapper.getNewInstance());
		tallyComponentVotesService = new TallyComponentVotesService(tallyComponentVotesFileRepository);
	}

	@Test
	@DisplayName("load with null parameters throws a NullPointerException")
	void verifyLoadTallyComponentVotesWithNullParametersThrows() {
		assertThrows(NullPointerException.class, () -> tallyComponentVotesService.load(null, BALLOT_BOX_ID));
		assertThrows(NullPointerException.class, () -> tallyComponentVotesService.load(ELECTION_EVENT_ID, null));
	}

	@Test
	@DisplayName("save with null parameters throws a NullPointerException")
	void verifySaveTallyComponentVotesWithNullParametersThrows() {
		assertThrows(NullPointerException.class, () -> tallyComponentVotesService.save(null));
	}

	@Test
	@DisplayName("persist tally component votes payload")
	void testSaveAndLoad() {
		final TallyComponentVotesPayload payload = new TallyComponentVotesPayload(votes.getGroup(), ELECTION_EVENT_ID, BALLOT_BOX_ID, votes,
				decodedVotes, decodedWriteIns);
		payload.setSignature(new CryptoPrimitivesSignature(new ImmutableByteArray("".getBytes(StandardCharsets.UTF_8))));

		final IllegalStateException ise1 = assertThrows(IllegalStateException.class,
				() -> tallyComponentVotesService.load(ELECTION_EVENT_ID, BALLOT_BOX_ID));
		assertTrue(ise1.getMessage().startsWith("Requested tally component votes payload is not present."));

		//persist
		assertDoesNotThrow(() -> tallyComponentVotesService.save(payload));

		//load
		assertDoesNotThrow(() -> tallyComponentVotesService.load(payload.getElectionEventId(), payload.getBallotBoxId()));

		//try to persist again
		final IllegalStateException ise2 = assertThrows(IllegalStateException.class, () -> tallyComponentVotesService.save(payload));
		assertTrue(ise2.getMessage().startsWith("Requested tally component votes payload already exists."));
	}
}
