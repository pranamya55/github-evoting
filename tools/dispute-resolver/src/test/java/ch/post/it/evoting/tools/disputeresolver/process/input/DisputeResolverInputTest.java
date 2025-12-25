/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;

@DisplayName("DisputeResolverInput with")
class DisputeResolverInputTest {

	private final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator = new ControlComponentExtractedElectionEventPayloadGenerator();
	private final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();

	private ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads;
	private ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;

	@BeforeEach
	void setUp() {
		controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadGenerator.generate();
		controlComponentExtractedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloadGenerator.generate(
				controlComponentExtractedElectionEventPayloads.getFirst());
	}

	@Test
	@DisplayName("valid inputs instantiates successfully.")
	void instantiateHappyPath() {
		final DisputeResolverInput disputeResolverInput = assertDoesNotThrow(() -> new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		));

		assertEquals(controlComponentExtractedElectionEventPayloads, disputeResolverInput.controlComponentExtractedElectionEventPayloads());
		assertEquals(controlComponentExtractedVerificationCardsPayloads, disputeResolverInput.controlComponentExtractedVerificationCardsPayloads());
	}

	@Test
	@DisplayName("null inputs throw a NullPointerException.")
	void instantiateWithNullInputThrows() {
		assertThrows(NullPointerException.class, () -> new DisputeResolverInput(null, controlComponentExtractedVerificationCardsPayloads));
		assertThrows(NullPointerException.class, () -> new DisputeResolverInput(controlComponentExtractedElectionEventPayloads, null));
	}

	@Test
	@DisplayName("unexpected number of control component extracted election event payloads throws an IllegalArgumentException.")
	void instantiateFailsWhenUnexpectedNumberOfElectionEventPayloads() {
		final ImmutableList<ControlComponentExtractedElectionEventPayload> unexpectedElectionEventPayloads = controlComponentExtractedElectionEventPayloads.stream()
				.limit(ControlComponentNode.ids().size() - 1)
				.collect(toImmutableList());

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				unexpectedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		));

		assertEquals("The number of control component extracted election event payloads must match the number of control component nodes.",
				illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("unexpected number of control component extracted verification cards payloads throws an IllegalArgumentException.")
	void instantiateFailsWhenUnexpectedNumberOfVerificationCardsPayloads() {
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> unexpectedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloads.stream()
				.limit(ControlComponentNode.ids().size() - 1)
				.collect(toImmutableList());

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				unexpectedVerificationCardsPayloads
		));

		assertEquals("The number of control component extracted verification cards payloads must match the number of control component nodes.",
				illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("control component extracted election event payloads with duplicate node ids throws an IllegalArgumentException.")
	void instantiateFailsWhenElectionEventPayloadsContainDuplicateNodeIds() {
		final ImmutableList<ControlComponentExtractedElectionEventPayload> duplicatedPayloads =
				Stream.concat(
						Stream.of(controlComponentExtractedElectionEventPayloads.getFirst()),
						controlComponentExtractedElectionEventPayloads.stream().limit(ControlComponentNode.ids().size() - 1)
				).collect(toImmutableList());

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				duplicatedPayloads,
				controlComponentExtractedVerificationCardsPayloads
		));

		assertEquals("The control component extracted election event payloads must not contain duplicate node ids.",
				illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("control component extracted verification cards payloads with duplicate node ids throws an IllegalArgumentException.")
	void instantiateFailsWhenVerificationCardsPayloadsContainDuplicateNodeIds() {
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> duplicatedPayloads =
				Stream.concat(
						Stream.of(controlComponentExtractedVerificationCardsPayloads.getFirst()),
						controlComponentExtractedVerificationCardsPayloads.stream().limit(ControlComponentNode.ids().size() - 1)
				).collect(toImmutableList());

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				duplicatedPayloads
		));

		assertEquals("The control component extracted verification cards payloads must not contain duplicate node ids.",
				illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("control component extracted election event payloads with different election event ids throws an IllegalArgumentException.")
	void instantiateFailsWhenElectionEventPayloadsHaveDifferentElectionEventIds() {
		final ImmutableList<ControlComponentExtractedElectionEventPayload> anotherControlComponentExtractedElectionEventPayloads =
				Stream.concat(
						Stream.of(controlComponentExtractedElectionEventPayloads.getFirst()),
						controlComponentExtractedElectionEventPayloadGenerator.generate().stream().skip(1)
				).collect(toImmutableList());

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				anotherControlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		));

		assertEquals("All control component extracted payloads must have the same election event id.", illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("payloads with different encryption groups throws an IllegalArgumentException.")
	void instantiateFailsWhenPayloadsHaveDifferentEncryptionGroups() {

		final ControlComponentExtractedElectionEventPayload firstControlComponentExtractedElectionEventPayload = controlComponentExtractedElectionEventPayloads.getFirst();
		final ExtractedElectionEvent extractedElectionEvent = firstControlComponentExtractedElectionEventPayload.getExtractedElectionEvent();
		final GqGroup encryptionGroup = extractedElectionEvent.encryptionGroup();

		final ControlComponentExtractedVerificationCardsPayloadGenerator anotherControlComponentExtractedVerificationCardsPayloadGenerator =
				new ControlComponentExtractedVerificationCardsPayloadGenerator(GroupTestData.getDifferentGqGroup(encryptionGroup));

		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> anotherControlComponentExtractedVerificationCardsPayloads =
				anotherControlComponentExtractedVerificationCardsPayloadGenerator.generate(firstControlComponentExtractedElectionEventPayload);

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				anotherControlComponentExtractedVerificationCardsPayloads
		));

		assertEquals("All control component extracted payloads must have the same encryption group.", illegalArgumentException.getMessage());
	}
}
