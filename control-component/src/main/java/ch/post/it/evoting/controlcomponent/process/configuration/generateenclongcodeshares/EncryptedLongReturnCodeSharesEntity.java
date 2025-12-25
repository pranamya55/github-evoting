/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "ENCRYPTED_LONG_RETURN_CODE_SHARES")
@IdClass(EncryptedLongReturnCodeSharesEntityKey.class)
public class EncryptedLongReturnCodeSharesEntity {

	@Id
	private int chunkId;

	@Id
	@Column(name = "VERIFICATION_CARD_SET_FK_ID")
	private String verificationCardSetId;

	@MapsId("verificationCardSetId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray controlComponentCodeShares;

	@Version
	private Integer changeControlId;

	public EncryptedLongReturnCodeSharesEntity(final int chunkId, final VerificationCardSetEntity verificationCardSetEntity,
			final ImmutableByteArray controlComponentCodeShares) {
		this.chunkId = chunkId;
		checkArgument(chunkId >= 0, "The chunkId must be positive.");
		this.verificationCardSetEntity = checkNotNull(verificationCardSetEntity);
		this.controlComponentCodeShares = checkNotNull(controlComponentCodeShares);
	}

	public EncryptedLongReturnCodeSharesEntity() {
	}

	public int getChunkId() {
		return chunkId;
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return verificationCardSetEntity;
	}

	public ImmutableByteArray getControlComponentCodeShares() {
		return controlComponentCodeShares;
	}
}
