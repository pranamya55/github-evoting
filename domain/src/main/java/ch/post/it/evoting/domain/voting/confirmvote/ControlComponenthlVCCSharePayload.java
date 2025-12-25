/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedControlComponentPayload;

@JsonPropertyOrder({ "encryptionGroup", "nodeId", "hashLongVoteCastReturnCodeShare", "confirmationKey", "confirmationAttemptId",
		"signature" })
@JsonDeserialize(using = ControlComponenthlVCCSharePayloadDeserializer.class)
public class ControlComponenthlVCCSharePayload implements SignedControlComponentPayload {

	private final GqGroup encryptionGroup;

	private final int nodeId;

	private final String hashLongVoteCastReturnCodeShare;

	private final ConfirmationKey confirmationKey;

	private final int confirmationAttemptId;

	private CryptoPrimitivesSignature signature;

	public ControlComponenthlVCCSharePayload(
			final GqGroup encryptionGroup,
			final int nodeId,
			final String hashLongVoteCastReturnCodeShare,
			final ConfirmationKey confirmationKey,
			final int confirmationAttemptId,
			final CryptoPrimitivesSignature signature) {
		this(encryptionGroup, nodeId, hashLongVoteCastReturnCodeShare, confirmationKey, confirmationAttemptId);
		this.signature = checkNotNull(signature);
	}

	public ControlComponenthlVCCSharePayload(final GqGroup encryptionGroup, final int nodeId, final String hashLongVoteCastReturnCodeShare,
			final ConfirmationKey confirmationKey, final int confirmationAttemptId) {
		this.encryptionGroup = checkNotNull(encryptionGroup);

		checkArgument(confirmationAttemptId >= 0 && confirmationAttemptId < MAX_CONFIRMATION_ATTEMPTS,
				"The confirmation attempt id must be in range [0,%s).", MAX_CONFIRMATION_ATTEMPTS);
		this.confirmationAttemptId = confirmationAttemptId;

		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		this.nodeId = nodeId;
		this.hashLongVoteCastReturnCodeShare = validateBase64Encoded(hashLongVoteCastReturnCodeShare);
		this.confirmationKey = checkNotNull(confirmationKey);

		checkArgument(this.encryptionGroup.equals(confirmationKey.element().getGroup()),
				"The group of the confirmation key and the control component hlVCC payload must be equal.");
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public int getNodeId() {
		return nodeId;
	}

	public String getHashLongVoteCastReturnCodeShare() {
		return hashLongVoteCastReturnCodeShare;
	}

	public ConfirmationKey getConfirmationKey() {
		return confirmationKey;
	}

	public int getConfirmationAttemptId() {
		return confirmationAttemptId;
	}

	@Override
	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				encryptionGroup,
				HashableBigInteger.from(BigInteger.valueOf(nodeId)),
				HashableString.from(hashLongVoteCastReturnCodeShare),
				confirmationKey,
				HashableBigInteger.from(confirmationAttemptId));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponenthlVCCSharePayload that = (ControlComponenthlVCCSharePayload) o;
		return nodeId == that.nodeId &&
				encryptionGroup.equals(that.encryptionGroup) &&
				hashLongVoteCastReturnCodeShare.equals(that.hashLongVoteCastReturnCodeShare) &&
				confirmationKey.equals(that.confirmationKey) &&
				confirmationAttemptId == that.confirmationAttemptId &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, nodeId, hashLongVoteCastReturnCodeShare, confirmationKey, confirmationAttemptId,
				signature);
	}
}
