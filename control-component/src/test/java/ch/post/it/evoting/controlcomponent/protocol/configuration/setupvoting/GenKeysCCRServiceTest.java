/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("genKeysCCR called with")
class GenKeysCCRServiceTest {

	private static GenKeysCCRService genKeysCCRService;
	private static GqGroup encryptionGroup;
	private static String electionEventId;
	private static ElectionEventContext electionEventContext;

	@BeforeAll
	static void setUpAll() {
		final Random random = RandomFactory.createRandom();
		final GenKeysCCRAlgorithm genKeysCCRAlgorithm = new GenKeysCCRAlgorithm(random, createZeroKnowledgeProof());
		genKeysCCRService = new GenKeysCCRService(genKeysCCRAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		encryptionGroup = electionEventContext.encryptionGroup();
		electionEventId = electionEventContext.electionEventId();
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, electionEventId, electionEventContext),
				Arguments.of(encryptionGroup, null, electionEventContext),
				Arguments.of(encryptionGroup, electionEventId, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genKeysCCRWithNullParametersThrows(final GqGroup encryptionGroup, final String electionEventId,
			final ElectionEventContext electionEventContext) {
		assertThrows(NullPointerException.class,
				() -> genKeysCCRService.genKeysCCR(encryptionGroup, electionEventId, electionEventContext));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void genKeysCCRWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> genKeysCCRService.genKeysCCR(encryptionGroup, "InvalidElectionEventId", electionEventContext));
	}

	@Test
	@DisplayName("different election event id throws IllegalArgumentException")
	void genKeysCCRWithDifferentElectionEventIdThrows() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String differrentElectionEventId = uuidGenerator.generate();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genKeysCCRService.genKeysCCR(encryptionGroup, differrentElectionEventId, electionEventContext));

		final String expected = "The election event context does not correspond to the given election event id.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}
}
