/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@DisplayName("ElectionEventContextServiceTest")
class SetupComponentPublicKeysServiceTest {

	private static final SetupComponentPublicKeysRepository SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY = mock(SetupComponentPublicKeysRepository.class);
	private static final ElectionEventService ELECTION_EVENT_SERVICE = mock(ElectionEventService.class);
	private static final MessageHandler MESSAGE_HANDLER = mock(MessageHandler.class);
	private static final Serializer SERIALIZER = mock(Serializer.class);
	private static final SignatureKeystore<Alias> SIGNATURE_KEYSTORE = mock(SignatureKeystore.class);
	private static final ResponseCompletionService responseCompletionService = mock(ResponseCompletionService.class);
	private static SetupComponentPublicKeysService setupComponentPublicKeysService;
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static String electionEventId;

	@BeforeAll
	static void setUpAll() throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate();
		setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();
		electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		setupComponentPublicKeysService = new SetupComponentPublicKeysService(SERIALIZER, mapper, MESSAGE_HANDLER, ELECTION_EVENT_SERVICE,
				SIGNATURE_KEYSTORE, responseCompletionService, SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY);

		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId,
				setupComponentPublicKeysPayload.getEncryptionGroup());
		final ImmutableByteArray electionPublicKey = new ImmutableByteArray(mapper.writeValueAsBytes(setupComponentPublicKeys.electionPublicKey()));
		final ImmutableByteArray ccrEncryptionPublicKey = new ImmutableByteArray(
				mapper.writeValueAsBytes(setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey()));
		final SetupComponentPublicKeysEntity setupComponentPublicKeysEntity = new SetupComponentPublicKeysEntity(electionEventEntity,
				ImmutableByteArray.EMPTY, ImmutableByteArray.EMPTY, ImmutableByteArray.EMPTY, electionPublicKey, ccrEncryptionPublicKey);
		when(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY.findById(electionEventId)).thenReturn(Optional.of(setupComponentPublicKeysEntity));

		when(ELECTION_EVENT_SERVICE.retrieveElectionEventEntity(electionEventId)).thenReturn(electionEventEntity);
	}

	@Test
	@DisplayName("Retrieving the voting client public keys with invalid IDs throws")
	void retrievingInvalidIdsThrows() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> setupComponentPublicKeysService.getVotingClientPublicKeys(null)),
				() -> assertThrows(FailedValidationException.class,
						() -> setupComponentPublicKeysService.getVotingClientPublicKeys("invalid electionEventId"))
		);
	}

	@Test
	@DisplayName("Retrieving not saved election event context throws")
	void retrievingNotSavedElectionEventContextThrows() {
		when(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY.findById(electionEventId)).thenReturn(Optional.empty());
		assertThrows(IllegalStateException.class, () -> setupComponentPublicKeysService.getVotingClientPublicKeys(electionEventId));
	}

	@Test
	@DisplayName("Retrieving the voting client public keys for a saved election event context does not throw")
	void retrievingSavedElectionEventContextDoesNotThrow() {
		final VotingClientPublicKeys expected = new VotingClientPublicKeys(setupComponentPublicKeysPayload.getEncryptionGroup(),
				setupComponentPublicKeys.electionPublicKey(), setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey());

		final VotingClientPublicKeys result;
		result = assertDoesNotThrow(() -> setupComponentPublicKeysService.getVotingClientPublicKeys(electionEventId));
		verify(SETUP_COMPONENT_PUBLIC_KEYS_REPOSITORY, times(1)).findById(electionEventId);
		assertEquals(expected.encryptionParameters(), result.encryptionParameters());
		assertEquals(expected.electionPublicKey().getKeyElements(), result.electionPublicKey().getKeyElements());
		assertEquals(expected.choiceReturnCodesEncryptionPublicKey().getKeyElements(),
				result.choiceReturnCodesEncryptionPublicKey().getKeyElements());
	}

	@Test
	@DisplayName("error verifying payload signature while saving throws IllegalStateException")
	void savingErrorVerifyingSignatureThrows() throws SignatureException {
		when(SIGNATURE_KEYSTORE.verifySignature(any(), eq(setupComponentPublicKeysPayload), any(), any())).thenThrow(SignatureException.class);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> setupComponentPublicKeysService.save(setupComponentPublicKeysPayload));

		assertEquals(String.format("Could not verify the signature of the setup component public keys. [electionEventId: %s]", electionEventId),
				exception.getMessage());
	}

	@Test
	@DisplayName("save payload with invalid signature throws InvalidPayloadSignatureException")
	void savingInvalidSignatureThrows() throws SignatureException {
		when(SIGNATURE_KEYSTORE.verifySignature(any(), eq(setupComponentPublicKeysPayload), any(), any())).thenReturn(false);

		final InvalidPayloadSignatureException exception = assertThrows(InvalidPayloadSignatureException.class,
				() -> setupComponentPublicKeysService.save(setupComponentPublicKeysPayload));

		assertEquals(String.format("Signature of payload %s is invalid. [electionEventId: %s]", SetupComponentPublicKeysPayload.class.getSimpleName(),
				electionEventId), exception.getMessage());
	}

}
