/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.BooleanConverter;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "HASHED_LONG_VOTE_CAST_RETURN_CODE_SHARES")
public class HashedLVCCSharesEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray hashedLongVoteCastReturnCodeShares;

	@Convert(converter = BooleanConverter.class)
	private boolean isVerified = false;

	@Version
	private Integer changeControlId;

	public HashedLVCCSharesEntity() {
	}

	public HashedLVCCSharesEntity(final VerificationCardEntity verificationCardEntity, final ImmutableByteArray hashedLongVoteCastReturnCodeShares,
			final boolean isVerified) {
		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.hashedLongVoteCastReturnCodeShares = checkNotNull(hashedLongVoteCastReturnCodeShares);
		this.isVerified = isVerified;
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public void setVerificationCardEntity(final VerificationCardEntity verificationCardEntity) {
		checkNotNull(verificationCardEntity);
		this.verificationCardEntity = verificationCardEntity;
	}

	public ImmutableByteArray getHashedLongVoteCastReturnCodeShares() {
		return hashedLongVoteCastReturnCodeShares;
	}

	public void setHashedLongVoteCastReturnCodeShares(final ImmutableByteArray hashedLongVoteCastReturnCodeShares) {
		checkNotNull(hashedLongVoteCastReturnCodeShares);
		this.hashedLongVoteCastReturnCodeShares = hashedLongVoteCastReturnCodeShares;
	}

	public boolean isVerified() {
		return isVerified;
	}

	public void setIsVerified(final boolean isVerified) {
		this.isVerified = isVerified;
	}
}
