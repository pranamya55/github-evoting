/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

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

import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "LONG_CHOICE_RETURN_CODE_SHARE")
public class LCCShareEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray longChoiceReturnCodeShare;

	@Version
	private Integer changeControlId;

	public LCCShareEntity(final VerificationCardEntity verificationCardEntity, final ImmutableByteArray longChoiceReturnCodeShare) {
		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.longChoiceReturnCodeShare = checkNotNull(longChoiceReturnCodeShare);
	}

	public LCCShareEntity() {
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public ImmutableByteArray getLongChoiceReturnCodeShare() {
		return longChoiceReturnCodeShare;
	}
}
