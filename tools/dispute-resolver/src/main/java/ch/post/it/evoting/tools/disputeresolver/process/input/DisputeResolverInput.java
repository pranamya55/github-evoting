/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;

/**
 * Validated input payloads for the dispute resolution process. The validations are the following:
 * <ul>
 *     <li>The number of control component extracted election event payloads must match the number of control component nodes.</li>
 *     <li>The number of control component extracted verification cards payloads must match the number of control component nodes.</li>
 *     <li>The control component extracted election event payloads must not contain duplicate node ids.</li>
 *     <li>The control component extracted verification cards payloads must not contain duplicate node ids.</li>
 *     <li>All payloads must have the same election event id.</li>
 *     <li>All payloads must have the same encryption group.</li>
 * </ul>
 *
 * @param controlComponentExtractedElectionEventPayloads     the control component extracted election event payloads. Must be non-null.
 * @param controlComponentExtractedVerificationCardsPayloads the control component extracted verification cards payloads. Must be non-null.
 */
public record DisputeResolverInput(
		ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads,
		ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads) {

	public DisputeResolverInput {
		checkNotNull(controlComponentExtractedElectionEventPayloads);
		checkNotNull(controlComponentExtractedVerificationCardsPayloads);

		// Check that the number of payloads matches the number of control component nodes.
		checkArgument(controlComponentExtractedElectionEventPayloads.size() == ControlComponentNode.ids().size(),
				"The number of control component extracted election event payloads must match the number of control component nodes.");

		checkArgument(controlComponentExtractedVerificationCardsPayloads.size() == ControlComponentNode.ids().size(),
				"The number of control component extracted verification cards payloads must match the number of control component nodes.");

		// Check that the payloads do not contain duplicate node ids.
		checkArgument(hasNoDuplicates(controlComponentExtractedElectionEventPayloads.stream()
				.map(ControlComponentExtractedElectionEventPayload::getNodeId)
				.collect(toImmutableList())), "The control component extracted election event payloads must not contain duplicate node ids.");

		checkArgument(hasNoDuplicates(controlComponentExtractedVerificationCardsPayloads.stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getNodeId)
				.collect(toImmutableList())), "The control component extracted verification cards payloads must not contain duplicate node ids.");

		final ImmutableList<ExtractedElectionEvent> extractedElectionEvents = controlComponentExtractedElectionEventPayloads.stream()
				.map(ControlComponentExtractedElectionEventPayload::getExtractedElectionEvent)
				.collect(toImmutableList());

		// Check that all payloads have the same election event id.
		checkArgument(allEqual(Stream.concat(
				extractedElectionEvents.stream().map(ExtractedElectionEvent::electionEventId),
				controlComponentExtractedVerificationCardsPayloads.stream().map(ControlComponentExtractedVerificationCardsPayload::getElectionEventId)
		), Function.identity()), "All control component extracted payloads must have the same election event id.");

		// Check that all payloads have the same encryption group.
		checkArgument(allEqual(Stream.concat(
				extractedElectionEvents.stream().map(ExtractedElectionEvent::encryptionGroup),
				controlComponentExtractedVerificationCardsPayloads.stream().map(ControlComponentExtractedVerificationCardsPayload::getEncryptionGroup)
		), Function.identity()), "All control component extracted payloads must have the same encryption group.");
	}

	public String electionEventId() {
		return controlComponentExtractedElectionEventPayloads.getFirst().getExtractedElectionEvent().electionEventId();
	}
}
