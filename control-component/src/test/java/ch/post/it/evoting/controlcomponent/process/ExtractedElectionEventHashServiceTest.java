/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.ExtractElectionEventService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@DisplayName("ExtractedElectionEventHashService calling")
class ExtractedElectionEventHashServiceTest {

	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static ExtractedElectionEventHashRepository extractedElectionEventHashRepository;
	private static ExtractedElectionEventHashService extractedElectionEventHashService;
	private static String electionEventId;
	private static String extractedElectionEventHash;

	@BeforeAll
	static void setUpAll() {
		final ExtractedElectionEventGenerator extractedElectionEventGenerator = new ExtractedElectionEventGenerator();
		final ExtractedElectionEvent extractedElectionEvent = extractedElectionEventGenerator.generate();
		electionEventId = extractedElectionEvent.electionEventId();

		final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm = new GetHashExtractedElectionEventAlgorithm(
				createBase64(), createHash());
		extractedElectionEventHash = getHashExtractedElectionEventAlgorithm.getHashExtractedElectionEvent(extractedElectionEvent);

		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		final GqGroup encryptionGroup = extractedElectionEvent.encryptionGroup();
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);
		when(electionEventService.getElectionEventEntity(electionEventId)).thenReturn(electionEventEntity);
		when(electionEventService.getEncryptionGroup(any())).thenReturn(encryptionGroup);

		extractedElectionEventHashRepository = mock(ExtractedElectionEventHashRepository.class);
		final ExtractedElectionEventHashEntity extractedElectionEventHashEntity = new ExtractedElectionEventHashEntity(electionEventEntity,
				extractedElectionEventHash);
		when(extractedElectionEventHashRepository.save(any())).thenReturn(extractedElectionEventHashEntity);
		when(extractedElectionEventHashRepository.findById(electionEventId)).thenReturn(Optional.of(extractedElectionEventHashEntity));

		final ExtractElectionEventService extractElectionEventService = mock(ExtractElectionEventService.class);
		when(extractElectionEventService.extractElectionEvent(electionEventId)).thenReturn(extractedElectionEvent);

		extractedElectionEventHashService = new ExtractedElectionEventHashService(electionEventService, extractElectionEventService,
				getHashExtractedElectionEventAlgorithm, extractedElectionEventHashRepository);
	}

	@Nested
	@DisplayName("computeAndSave with")
	class ComputeAndSaveTest {

		@Test
		@DisplayName("null electionEventId throws NullPointerException")
		void computeAndSaveWithNullElectionEventIdThrows() {
			assertThrows(NullPointerException.class, () -> extractedElectionEventHashService.computeAndSave(null));
		}

		@Test
		@DisplayName("invalid UUID electionEventId throws IllegalArgumentException")
		void computeAndSaveWithInvalidUUIDElectionEventIdThrows() {
			final String invalidUUID = UUID_GENERATOR.generate().concat("!");
			final FailedValidationException exception = assertThrows(FailedValidationException.class,
					() -> extractedElectionEventHashService.computeAndSave(invalidUUID));

			final String expected = String.format("The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
					invalidUUID);
			assertEquals(expected, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("valid electionEventId does not throw")
		void computeAndSaveWithValidExtractedElectionEventHashDoesNotThrow() {
			assertDoesNotThrow(() -> extractedElectionEventHashService.computeAndSave(electionEventId));

			verify(extractedElectionEventHashRepository, times(1)).save(any());
		}
	}

	@Nested
	@DisplayName("getHashExtractedElectionEvent with")
	class GetHashExtractedElectionEventTest {

		@Test
		@DisplayName("null electionEventId throws NullPointerException")
		void getHashExtractedElectionEventWithNullElectionEventIdThrows() {
			assertThrows(NullPointerException.class, () -> extractedElectionEventHashService.getHashExtractedElectionEvent(null));
		}

		@Test
		@DisplayName("invalid UUID electionEventId throws IllegalArgumentException")
		void getHashExtractedElectionEventWithInvalidUUIDElectionEventIdThrows() {
			final String invalidUUID = UUID_GENERATOR.generate().concat("!");
			final FailedValidationException exception = assertThrows(FailedValidationException.class,
					() -> extractedElectionEventHashService.getHashExtractedElectionEvent(invalidUUID));

			final String expected = String.format("The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
					invalidUUID);
			assertEquals(expected, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("non existing electionEventId throws IllegalStateException")
		void getHashExtractedElectionEventWithNonExistingElectionEventIdThrows() {
			final String nonExistingElectionId = UUID_GENERATOR.generate();
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> extractedElectionEventHashService.getHashExtractedElectionEvent(nonExistingElectionId));

			final String expected = String.format("Extracted election event hash entity not found. [electionEventId: %s]", nonExistingElectionId);
			assertEquals(expected, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("valid electionEventId returns the hash of the extracted election event")
		void getHashExtractedElectionEventWithValidElectionEventIdReturnsTheHash() {
			final String actualExtractedElectionEventHash = extractedElectionEventHashService.getHashExtractedElectionEvent(electionEventId);
			assertEquals(extractedElectionEventHash, actualExtractedElectionEventHash);
		}
	}
}