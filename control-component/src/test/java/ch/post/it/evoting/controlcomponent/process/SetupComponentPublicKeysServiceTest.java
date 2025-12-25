/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;

@DisplayName("SetupComponentPublicKeysServiceTest")
class SetupComponentPublicKeysServiceTest {

	private static final ObjectMapper objectMapper = mock(ObjectMapper.class);
	private static final SetupComponentPublicKeysRepository SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY = mock(SetupComponentPublicKeysRepository.class);
	private static final ElectionEventService electionEventService = mock(ElectionEventService.class);
	private static final BallotBoxService ballotBoxService = mock(BallotBoxService.class);

	private static SetupComponentPublicKeysService setupComponentPublicKeysService;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static String electionEventId;

	@BeforeAll
	static void setUpAll() throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate();
		setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();
		final GqGroup encryptionGroup = setupComponentPublicKeysPayload.getEncryptionGroup();
		electionEventId = setupComponentPublicKeysPayload.getElectionEventId();
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);

		final SetupComponentPublicKeysEntity electionContextEntity = new SetupComponentPublicKeysEntity.Builder()
				.setElectionEventEntity(electionEventEntity)
				.setCombinedControlComponentPublicKey(
						new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.combinedControlComponentPublicKeys())))
				.setElectoralBoardPublicKey(new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.electoralBoardPublicKey())))
				.setElectoralBoardSchnorrProofs(
						new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.electoralBoardSchnorrProofs())))
				.setElectionPublicKey(new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.electionPublicKey())))
				.setChoiceReturnCodesEncryptionPublicKey(
						new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey())))
				.build();

		when(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY.save(any())).thenReturn(new SetupComponentPublicKeysEntity());
		when(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY.findById(electionEventId)).thenReturn(
				Optional.ofNullable(electionContextEntity));
		when(electionEventService.getElectionEventEntity(electionEventId)).thenReturn(electionEventEntity);
		doNothing().when(ballotBoxService).saveAllFromContexts(any());
		when(electionEventService.getEncryptionGroup(any())).thenReturn(encryptionGroup);

		setupComponentPublicKeysService = new SetupComponentPublicKeysService(mapper, electionEventService, SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY);
	}

	@Test
	@DisplayName("saving with failed serialization of SetupComponentPublicKeys throws UncheckedIOException")
	void failedToSerializeSetupComponentPublicKeysThrows() throws JsonProcessingException {
		when(objectMapper.writeValueAsBytes(any())).thenThrow(JsonProcessingException.class);
		setupComponentPublicKeysService = new SetupComponentPublicKeysService(objectMapper, electionEventService,
				SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY);
		final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
				() -> setupComponentPublicKeysService.save(electionEventId, setupComponentPublicKeys));
		assertEquals("Failed to serialize setup component public keys.", exception.getMessage());
	}

	@Test
	@DisplayName("saving with valid SetupComponentPublicKeys does not throw")
	void savingValidSetupComponentPublicKeysDoesNotThrow() {
		setupComponentPublicKeysService.save(electionEventId, setupComponentPublicKeys);
		verify(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY, times(1)).save(any());
	}

	@Test
	@DisplayName("loading non existing SetupComponentPublicKeys throws IllegalStateException")
	void loadNonExistingThrows() {
		final String nonExistingElectionId = "E77DBE3C70874EA584C490A0C6AC0CA4";
		final IllegalStateException exceptionCcm = assertThrows(IllegalStateException.class,
				() -> setupComponentPublicKeysService.getSetupComponentPublicKeysEntity(nonExistingElectionId));
		assertEquals(String.format("Setup component public keys entity not found. [electionEventId: %s]", nonExistingElectionId),
				Throwables.getRootCause(exceptionCcm).getMessage());
	}
}
