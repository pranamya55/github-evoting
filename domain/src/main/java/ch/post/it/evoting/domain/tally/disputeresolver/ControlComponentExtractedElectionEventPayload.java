/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonDeserialize(using = ControlComponentExtractedElectionEventPayloadDeserializer.class)
public class ControlComponentExtractedElectionEventPayload implements SignedPayload {

	private final int nodeId;
	private final ExtractedElectionEvent extractedElectionEvent;

	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public ControlComponentExtractedElectionEventPayload(
			@JsonProperty("nodeId")
			final int nodeId,
			@JsonProperty("extractedElectionEvent")
			final ExtractedElectionEvent extractedElectionEvent,
			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(nodeId, extractedElectionEvent);
		this.signature = checkNotNull(signature);
	}

	public ControlComponentExtractedElectionEventPayload(final int nodeId, final ExtractedElectionEvent extractedElectionEvent) {
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		this.nodeId = nodeId;
		this.extractedElectionEvent = checkNotNull(extractedElectionEvent);
	}

	public int getNodeId() {
		return nodeId;
	}

	public ExtractedElectionEvent getExtractedElectionEvent() {
		return extractedElectionEvent;
	}

	@Override
	public CryptoPrimitivesSignature getSignature() {
		return this.signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableBigInteger.from(nodeId), extractedElectionEvent);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponentExtractedElectionEventPayload that = (ControlComponentExtractedElectionEventPayload) o;
		return nodeId == that.nodeId && Objects.equals(extractedElectionEvent, that.extractedElectionEvent) && Objects.equals(
				signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, extractedElectionEvent, signature);
	}
}
