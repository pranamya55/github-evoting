/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
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

import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "LONG_VOTE_CAST_RETURN_CODE_SHARE")
@IdClass(LVCCShareEntityKey.class)
public class LVCCShareEntity {

	@Id
	@Column(name = "VERIFICATION_CARD_ID")
	private String verificationCardId;

	@MapsId("verificationCardId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	private String confirmationKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray longVoteCastReturnCodeShare;

	private String hashedLongVoteCastReturnCodeShare;

	@Id
	private int confirmationAttemptId = 0;

	@Version
	private Integer changeControlId;

	public LVCCShareEntity(final VerificationCardEntity verificationCardEntity,
			final ImmutableByteArray longVoteCastReturnCodeShare,
			final String hashedLongVoteCastReturnCodeShare,
			final String confirmationKey,
			final int confirmationAttemptId) {
		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.longVoteCastReturnCodeShare = checkNotNull(longVoteCastReturnCodeShare);
		this.hashedLongVoteCastReturnCodeShare = checkNotNull(hashedLongVoteCastReturnCodeShare);
		this.confirmationKey = checkNotNull(confirmationKey);

		checkArgument(confirmationAttemptId >= 0 && confirmationAttemptId < MAX_CONFIRMATION_ATTEMPTS,
				"The confirmation attempt id must be in range [0,%s).", MAX_CONFIRMATION_ATTEMPTS);
		this.confirmationAttemptId = confirmationAttemptId;
	}

	public LVCCShareEntity() {
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public ImmutableByteArray getLongVoteCastReturnCodeShare() {
		return longVoteCastReturnCodeShare;
	}

	public String getHashedLongVoteCastReturnCodeShare() {
		return hashedLongVoteCastReturnCodeShare;
	}

	public String getConfirmationKey() {
		return confirmationKey;
	}

	public int getConfirmationAttemptId() {
		return confirmationAttemptId;
	}
}
