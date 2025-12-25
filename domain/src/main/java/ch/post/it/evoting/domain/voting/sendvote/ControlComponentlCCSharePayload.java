/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedControlComponentPayload;

@JsonDeserialize(using = ControlComponentlCCSharePayloadDeserializer.class)
@JsonPropertyOrder({ "encryptionGroup", "longChoiceReturnCodeShare", "signature" })
public class ControlComponentlCCSharePayload implements SignedControlComponentPayload {

	private final GqGroup encryptionGroup;

	private final LongChoiceReturnCodeShare longChoiceReturnCodeShare;

	private CryptoPrimitivesSignature signature;

	public ControlComponentlCCSharePayload(
			final GqGroup encryptionGroup,
			final LongChoiceReturnCodeShare longChoiceReturnCodeShare,
			final CryptoPrimitivesSignature signature) {
		this(encryptionGroup, longChoiceReturnCodeShare);
		this.signature = checkNotNull(signature);
	}

	public ControlComponentlCCSharePayload(final GqGroup encryptionGroup, final LongChoiceReturnCodeShare longChoiceReturnCodeShare) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.longChoiceReturnCodeShare = checkNotNull(longChoiceReturnCodeShare);

		checkArgument(encryptionGroup.equals(longChoiceReturnCodeShare.longChoiceReturnCodeShare().getGroup()),
				"The groups of the longChoiceReturnCodeShare and the control component LCC share must be equal.");
	}

	@Override
	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	@Override
	@JsonIgnore
	public int getNodeId() {
		return longChoiceReturnCodeShare.nodeId();
	}

	public LongChoiceReturnCodeShare getLongChoiceReturnCodeShare() {
		return longChoiceReturnCodeShare;
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
				longChoiceReturnCodeShare);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponentlCCSharePayload that = (ControlComponentlCCSharePayload) o;
		return encryptionGroup.equals(that.encryptionGroup) &&
				longChoiceReturnCodeShare.equals(that.longChoiceReturnCodeShare) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, longChoiceReturnCodeShare, signature);
	}
}
