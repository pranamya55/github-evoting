/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonDeserialize(using = ControlComponentExtractedVerificationCardsPayloadDeserializer.class)
public class ControlComponentExtractedVerificationCardsPayload implements SignedPayload {

	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final int nodeId;
	private final ImmutableList<ExtractedVerificationCard> extractedVerificationCards;

	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public ControlComponentExtractedVerificationCardsPayload(
			@JsonProperty("encryptionGroup")
			final GqGroup encryptionGroup,
			@JsonProperty("electionEventId")
			final String electionEventId,
			@JsonProperty("nodeId")
			final int nodeId,
			@JsonProperty("extractedVerificationCards")
			final ImmutableList<ExtractedVerificationCard> extractedVerificationCards,
			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(encryptionGroup, electionEventId, nodeId, extractedVerificationCards);
		this.signature = checkNotNull(signature);
	}

	public ControlComponentExtractedVerificationCardsPayload(final GqGroup encryptionGroup, final String electionEventId, final int nodeId,
			final ImmutableList<ExtractedVerificationCard> extractedVerificationCards) {
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.electionEventId = validateUUID(electionEventId);
		this.nodeId = nodeId;
		this.extractedVerificationCards = checkNotNull(extractedVerificationCards).stream()
				.sorted(Comparator.comparing(ExtractedVerificationCard::verificationCardId))
				.collect(toImmutableList());

		checkArgument(
				hasNoDuplicates(extractedVerificationCards.stream()
						.map(ExtractedVerificationCard::verificationCardId)
						.collect(toImmutableList())),
				"The verification card ids must be unique.");
		checkArgument(extractedVerificationCards.stream()
						.map(ExtractedVerificationCard::encryptedVote)
						.map(ElGamalMultiRecipientCiphertext::getGroup)
						.allMatch(encryptionGroup::equals),
				"The encrypted vote's group must be equal to the encryption group.");
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public ImmutableList<ExtractedVerificationCard> getExtractedVerificationCards() {
		return extractedVerificationCards;
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
		return ImmutableList.of(
				encryptionGroup,
				HashableString.from(electionEventId),
				HashableBigInteger.from(nodeId),
				HashableList.from(extractedVerificationCards)
		);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponentExtractedVerificationCardsPayload that = (ControlComponentExtractedVerificationCardsPayload) o;
		return nodeId == that.nodeId && Objects.equals(encryptionGroup, that.encryptionGroup) && Objects.equals(electionEventId,
				that.electionEventId) && Objects.equals(extractedVerificationCards, that.extractedVerificationCards)
				&& Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, nodeId, extractedVerificationCards, signature);
	}
}
