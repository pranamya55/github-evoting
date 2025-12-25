/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.generators;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentExtractedElectionEventPayloadGenerator {

	private static final Hash hash = createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	final GqGroup encryptionGroup;
	final ElGamalGenerator elGamalGenerator;
	final ExtractedElectionEventGenerator extractedElectionEventGenerator;

	public ControlComponentExtractedElectionEventPayloadGenerator(final GqGroup encryptionGroup) {
		this.encryptionGroup = encryptionGroup;
		this.elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		this.extractedElectionEventGenerator = new ExtractedElectionEventGenerator(encryptionGroup);
	}

	public ControlComponentExtractedElectionEventPayloadGenerator() {
		this(GroupTestData.getLargeGqGroup());
	}

	public ImmutableList<ControlComponentExtractedElectionEventPayload> generate() {
		final String electionEventId = uuidGenerator.generate();
		final ExtractedElectionEvent extractedElectionEvent = extractedElectionEventGenerator.generate(electionEventId);

		return ControlComponentNode.ids().stream()
				.map(nodeId -> generate(extractedElectionEvent, nodeId))
				.collect(toImmutableList());
	}

	public ControlComponentExtractedElectionEventPayload generate(final String electionEventId, final int nodeId) {

		final ExtractedElectionEvent extractedElectionEvent = extractedElectionEventGenerator.generate(electionEventId);

		return generate(extractedElectionEvent, nodeId);
	}

	public ControlComponentExtractedElectionEventPayload generate(final ExtractedElectionEvent extractedElectionEvent, final int nodeId) {

		final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload = new ControlComponentExtractedElectionEventPayload(
				nodeId, extractedElectionEvent);

		final ImmutableByteArray payloadHash = hash.recursiveHash(controlComponentExtractedElectionEventPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		controlComponentExtractedElectionEventPayload.setSignature(signature);

		return controlComponentExtractedElectionEventPayload;
	}
}
