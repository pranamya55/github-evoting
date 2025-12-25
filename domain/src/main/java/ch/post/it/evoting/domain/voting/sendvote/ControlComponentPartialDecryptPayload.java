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

@JsonDeserialize(using = ControlComponentPartialDecryptPayloadDeserializer.class)
@JsonPropertyOrder({ "encryptionGroup", "partiallyDecryptedEncryptedPCC", "signature" })
public class ControlComponentPartialDecryptPayload implements SignedControlComponentPayload {

	private final GqGroup encryptionGroup;

	private final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC;

	private CryptoPrimitivesSignature signature;

	public ControlComponentPartialDecryptPayload(
			final GqGroup encryptionGroup,
			final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC,
			final CryptoPrimitivesSignature signature) {
		this(encryptionGroup, partiallyDecryptedEncryptedPCC);
		this.signature = checkNotNull(signature);
	}

	public ControlComponentPartialDecryptPayload(final GqGroup encryptionGroup, final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.partiallyDecryptedEncryptedPCC = checkNotNull(partiallyDecryptedEncryptedPCC);

		checkArgument(this.partiallyDecryptedEncryptedPCC.exponentiatedGammas().getGroup().equals(encryptionGroup),
				"The group of the exponentiated gammas of the partially decrypted encrypted PCC and of the control component partial decrypt payload must be the same.");
	}

	@Override
	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	@Override
	@JsonIgnore
	public int getNodeId() {
		return partiallyDecryptedEncryptedPCC.nodeId();
	}

	public PartiallyDecryptedEncryptedPCC getPartiallyDecryptedEncryptedPCC() {
		return partiallyDecryptedEncryptedPCC;
	}

	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				encryptionGroup,
				partiallyDecryptedEncryptedPCC);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ControlComponentPartialDecryptPayload that = (ControlComponentPartialDecryptPayload) o;
		return encryptionGroup.equals(that.encryptionGroup) &&
				partiallyDecryptedEncryptedPCC.equals(that.partiallyDecryptedEncryptedPCC) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, partiallyDecryptedEncryptedPCC, signature);
	}
}
