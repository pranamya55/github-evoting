/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@JsonDeserialize(using = ControlComponentlVCCSharePayloadDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({ "electionEventId", "verificationCardSetId", "verificationCardId", "nodeId", "encryptionGroup", "longVoteCastReturnCodeShare",
		"confirmationKey", "isVerified", "signature" })
public class ControlComponentlVCCSharePayload implements SignedControlComponentPayload {

	private final String electionEventId;

	private final String verificationCardSetId;

	private final String verificationCardId;

	private final int nodeId;

	private final GqGroup encryptionGroup;

	private final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare;

	private final ConfirmationKey confirmationKey;

	private final boolean isVerified;

	private CryptoPrimitivesSignature signature;

	public ControlComponentlVCCSharePayload(
			final String electionEventId,
			final String verificationCardSetId,
			final String verificationCardId,
			final int nodeId,
			final GqGroup encryptionGroup,
			final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare,
			final ConfirmationKey confirmationKey,
			final boolean isVerified,
			final CryptoPrimitivesSignature signature) {
		this(electionEventId, verificationCardSetId, verificationCardId, nodeId, encryptionGroup, longVoteCastReturnCodeShare, confirmationKey,
				isVerified);
		this.signature = checkNotNull(signature);
	}

	public ControlComponentlVCCSharePayload(final String electionEventId, final String verificationCardSetId, final String verificationCardId,
			final int nodeId, final GqGroup encryptionGroup, final ConfirmationKey confirmationKey, final boolean isVerified) {
		this(electionEventId, verificationCardSetId, verificationCardId, nodeId, encryptionGroup, null, confirmationKey, isVerified);
	}

	public ControlComponentlVCCSharePayload(final String electionEventId, final String verificationCardSetId, final String verificationCardId,
			final int nodeId, final GqGroup encryptionGroup, final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare,
			final ConfirmationKey confirmationKey, final boolean isVerified) {
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.verificationCardId = validateUUID(verificationCardId);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		this.nodeId = nodeId;
		this.encryptionGroup = checkNotNull(encryptionGroup);
		checkArgument((longVoteCastReturnCodeShare != null) == isVerified);
		this.longVoteCastReturnCodeShare = longVoteCastReturnCodeShare;
		this.confirmationKey = checkNotNull(confirmationKey);
		this.isVerified = isVerified;

		checkArgument(Objects.isNull(longVoteCastReturnCodeShare) || electionEventId.equals(longVoteCastReturnCodeShare.electionEventId()),
				"The election event id of the control component lVCC share payload and the long vote cast return code share must be equal.");
		checkArgument(
				Objects.isNull(longVoteCastReturnCodeShare) || verificationCardSetId.equals(longVoteCastReturnCodeShare.verificationCardSetId()),
				"The verification card set id of the control component lVCC share payload and the long vote cast return code share must be equal.");
		checkArgument(Objects.isNull(longVoteCastReturnCodeShare) || verificationCardId.equals(longVoteCastReturnCodeShare.verificationCardId()),
				"The verification card id of the control component lVCC share payload and the long vote cast return code share must be equal.");

		checkArgument(Objects.isNull(longVoteCastReturnCodeShare) ||
						encryptionGroup.equals(longVoteCastReturnCodeShare.longVoteCastReturnCodeShare().getGroup()),
				"The groups of the long vote cast return codes share and the control component lVCC share payload must be equal.");
		checkArgument(encryptionGroup.equals(confirmationKey.element().getGroup()),
				"The groups of the confirmation key element and the control component lVCC share payload must be equal.");
		checkArgument(confirmationKey.contextIds().electionEventId().equals(electionEventId),
				"The election event id of the control component lVCC share payload and the confirmation key must be equal.");
		checkArgument(confirmationKey.contextIds().verificationCardSetId().equals(verificationCardSetId),
				"The verification card set id of the control component lVCC share payload and the confirmation key must be equal.");
		checkArgument(confirmationKey.contextIds().verificationCardId().equals(verificationCardId),
				"The verification card id of the control component lVCC share payload and the confirmation key must be equal.");
	}

	public Optional<LongVoteCastReturnCodeShare> getLongVoteCastReturnCodeShare() {
		return Optional.ofNullable(longVoteCastReturnCodeShare);
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public ConfirmationKey getConfirmationKey() {
		return confirmationKey;
	}

	@JsonProperty("isVerified")
	public boolean isVerified() {
		return isVerified;
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
		if (isVerified) {
			return ImmutableList.of(
					HashableString.from(electionEventId),
					HashableString.from(verificationCardSetId),
					HashableString.from(verificationCardId),
					HashableBigInteger.from(BigInteger.valueOf(nodeId)),
					encryptionGroup,
					longVoteCastReturnCodeShare,
					confirmationKey,
					HashableString.from(Boolean.toString(isVerified)));
		} else {
			return ImmutableList.of(
					HashableString.from(electionEventId),
					HashableString.from(verificationCardSetId),
					HashableString.from(verificationCardId),
					HashableBigInteger.from(BigInteger.valueOf(nodeId)),
					encryptionGroup,
					confirmationKey,
					HashableString.from(Boolean.toString(isVerified)));
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponentlVCCSharePayload that = (ControlComponentlVCCSharePayload) o;
		return nodeId == that.nodeId &&
				isVerified == that.isVerified &&
				Objects.equals(encryptionGroup, that.encryptionGroup) &&
				Objects.equals(electionEventId, that.electionEventId) &&
				Objects.equals(verificationCardSetId, that.verificationCardSetId) &&
				Objects.equals(verificationCardId, that.verificationCardId) &&
				Objects.equals(longVoteCastReturnCodeShare, that.longVoteCastReturnCodeShare) &&
				Objects.equals(confirmationKey, that.confirmationKey) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, verificationCardSetId, verificationCardId, nodeId, longVoteCastReturnCodeShare,
				isVerified, confirmationKey, signature);
	}

	@Override
	public String toString() {
		return "ControlComponentlVCCSharePayload{" +
				"encryptionGroup=" + encryptionGroup +
				", electionEventId='" + electionEventId + '\'' +
				", verificationCardSetId='" + verificationCardSetId + '\'' +
				", verificationCardId='" + verificationCardId + '\'' +
				", nodeId=" + nodeId +
				", longVoteCastReturnCodeShare=" + longVoteCastReturnCodeShare +
				", confirmationKey=" + confirmationKey +
				", isVerified=" + isVerified +
				", signature=" + signature +
				'}';
	}
}
