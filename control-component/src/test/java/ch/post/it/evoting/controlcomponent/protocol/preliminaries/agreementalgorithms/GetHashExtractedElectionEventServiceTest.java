/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@DisplayName("getHashExtractedElectionEvent with")
class GetHashExtractedElectionEventServiceTest {

	private static ExtractedElectionEventHashService extractedElectionEventHashService;
	private static GetHashExtractedElectionEventService getHashExtractedElectionEventService;

	private String electionEventId;

	@BeforeAll
	static void setupAll() {
		extractedElectionEventHashService = mock(ExtractedElectionEventHashService.class);
		getHashExtractedElectionEventService = new GetHashExtractedElectionEventService(extractedElectionEventHashService);
	}

	@BeforeEach
	void setup() {
		final ExtractedElectionEventGenerator generator = new ExtractedElectionEventGenerator();
		final ExtractedElectionEvent extractedElectionEvent = generator.generate();
		electionEventId = extractedElectionEvent.electionEventId();

		final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm = new GetHashExtractedElectionEventAlgorithm(
				createBase64(), createHash());
		final String extractedElectionEventHash = getHashExtractedElectionEventAlgorithm.getHashExtractedElectionEvent(extractedElectionEvent);
		when(extractedElectionEventHashService.getHashExtractedElectionEvent(electionEventId)).thenReturn(extractedElectionEventHash);
	}

	@Test
	@DisplayName("null argument throws a NullPointerException")
	void getHashExtractedElectionEventWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> getHashExtractedElectionEventService.getHashExtractedElectionEvent(null));
	}

	@Test
	@DisplayName("valid arguments returns a Base64 encoded string")
	void getHashExtractedElectionEventWithValidArgumentsReturns() {
		final String result = assertDoesNotThrow(
				() -> getHashExtractedElectionEventService.getHashExtractedElectionEvent(electionEventId));
		final Base64 base64 = createBase64();
		assertEquals(BASE64_ENCODED_HASH_OUTPUT_LENGTH, result.length());
		assertDoesNotThrow(() -> base64.base64Decode(result));
	}
}