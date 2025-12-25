/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "VERIFICATION_CARD")
public class VerificationCardEntity {

	@Id
	private String verificationCardId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray verificationCardPublicKey;

	@OneToOne(mappedBy = "verificationCardEntity", cascade = CascadeType.ALL, optional = false)
	@PrimaryKeyJoinColumn
	private VerificationCardStateEntity verificationCardStateEntity;

	@Version
	private Integer changeControlId;

	public VerificationCardEntity() {
	}

	public VerificationCardEntity(
			final String verificationCardId,
			final VerificationCardSetEntity verificationCardSetEntity,
			final ImmutableByteArray verificationCardPublicKey) {
		this.verificationCardId = validateUUID(verificationCardId);
		this.verificationCardSetEntity = verificationCardSetEntity;
		this.verificationCardPublicKey = checkNotNull(verificationCardPublicKey);
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public ImmutableByteArray getVerificationCardPublicKey() {
		return verificationCardPublicKey;
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return verificationCardSetEntity;
	}

	public VerificationCardStateEntity getVerificationCardStateEntity() {
		return verificationCardStateEntity;
	}

	public void setVerificationCardStateEntity(final VerificationCardStateEntity verificationCardStateEntity) {
		this.verificationCardStateEntity = verificationCardStateEntity;
	}
}
