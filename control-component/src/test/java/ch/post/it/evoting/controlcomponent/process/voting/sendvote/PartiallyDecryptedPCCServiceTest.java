/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.ElectionEventEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartiallyDecryptedPCCService calling")
class PartiallyDecryptedPCCServiceTest extends TestGroupSetup {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	@Spy
	private final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

	@Mock
	private VerificationCardService verificationCardService;

	@Mock
	private PartiallyDecryptedPCCRepository partiallyDecryptedPCCRepository;

	@InjectMocks
	private PartiallyDecryptedPCCService partiallyDecryptedPCCService;

	@Nested
	@DisplayName("save with")
	class saveTest {

		private ContextIds contextIds;
		private PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC;

		@BeforeEach
		void setUp() {
			final String electionEventId = uuidGenerator.generate();
			final String verificationCardSetId = uuidGenerator.generate();
			final String verificationCardId = uuidGenerator.generate();
			contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

			final GroupVector<GqElement, GqGroup> exponentiatedGammas = gqGroupGenerator.genRandomGqElementVector(1);

			final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
					zqGroupGenerator.genRandomZqElementMember());

			partiallyDecryptedEncryptedPCC = new PartiallyDecryptedEncryptedPCC(contextIds, 1, exponentiatedGammas,
					GroupVector.of(exponentiationProof));
		}

		@Test
		@DisplayName("null partiallyDecryptedEncryptedPCC throws NullPointerException")
		void nullPCCThrows() {
			assertThrows(NullPointerException.class, () -> partiallyDecryptedPCCService.save(null));
		}

		@Test
		@DisplayName("serialization failing throws UncheckedIOException")
		void serializationFailingThrows() throws JsonProcessingException {
			final String verificationCardId = partiallyDecryptedEncryptedPCC.contextIds().verificationCardId();

			final VerificationCardEntity verificationCardEntity = new VerificationCardEntity();
			when(verificationCardService.getVerificationCardEntity(verificationCardId)).thenReturn(verificationCardEntity);

			when(objectMapper.writeValueAsBytes(partiallyDecryptedEncryptedPCC)).thenThrow(JsonProcessingException.class);

			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> partiallyDecryptedPCCService.save(partiallyDecryptedEncryptedPCC));
			final String errorMessage = String.format("Failed to serialize partially decrypted encrypted PCC. [contextId: %s]", contextIds);
			assertEquals(errorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("valid partiallyDecryptedEncryptedPCC does not throw")
		void validPartiallyDecryptedEncryptedPCC() {
			final String verificationCardId = partiallyDecryptedEncryptedPCC.contextIds().verificationCardId();

			final VerificationCardEntity verificationCardEntity = new VerificationCardEntity();
			when(verificationCardService.getVerificationCardEntity(verificationCardId)).thenReturn(verificationCardEntity);

			doReturn(null).when(partiallyDecryptedPCCRepository).save(any());

			assertDoesNotThrow(() -> partiallyDecryptedPCCService.save(partiallyDecryptedEncryptedPCC));
			verify(partiallyDecryptedPCCRepository).save(any());
		}

	}

	@Nested
	@DisplayName("get with")
	class getTest {

		private String verificationCardId;

		@BeforeEach
		void setUp() {
			verificationCardId = uuidGenerator.generate();
		}

		@Test
		@DisplayName("invalid verification card id throws")
		void invalidVerificationCardIdThrows() {
			assertThrows(FailedValidationException.class, () -> partiallyDecryptedPCCService.get("invalidId"));
			assertThrows(NullPointerException.class, () -> partiallyDecryptedPCCService.get(null));
		}

		@Test
		@DisplayName("partially decrypted pcc not found throws IllegalStateException")
		void partiallyDecryptedPCCNotFoundThrows() {
			when(partiallyDecryptedPCCRepository.findById(verificationCardId)).thenReturn(Optional.empty());

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> partiallyDecryptedPCCService.get(verificationCardId));
			final String errorMessage = String.format("Partially decrypted encrypted pcc not found. [verificationCardId: %s]", verificationCardId);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("deserialization failing throws UncheckIOException")
		void deserializationFailingThrows() {
			final String verificationCardSetId = uuidGenerator.generate();
			final String electionEventId = uuidGenerator.generate();
			final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, gqGroup);
			final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardSetAlias("alias-" + verificationCardSetId)
					.setVerificationCardSetDescription("Description " + verificationCardSetId)
					.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
					.setElectionEventEntity(electionEventEntity)
					.build();

			final VerificationCardEntity verificationCardEntity = new VerificationCardEntity(verificationCardId, verificationCardSetEntity,
					ImmutableByteArray.EMPTY);
			final PartiallyDecryptedPCCEntity partiallyDecryptedPCCEntity = new PartiallyDecryptedPCCEntity(verificationCardEntity,
					ImmutableByteArray.EMPTY);
			when(partiallyDecryptedPCCRepository.findById(verificationCardId)).thenReturn(Optional.of(partiallyDecryptedPCCEntity));

			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> partiallyDecryptedPCCService.get(verificationCardId));
			final String errorMessage = String.format("Failed to deserialize partially decrypted encrypted PCC. [verificationCardId: %s]",
					verificationCardId);
			assertEquals(errorMessage, exception.getMessage());
		}

	}

}
