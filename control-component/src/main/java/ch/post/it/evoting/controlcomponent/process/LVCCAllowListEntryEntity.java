/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "LVCC_ALLOW_LIST_ENTRY")
public class LVCCAllowListEntryEntity {

	@Id
	private String longVoteCastReturnCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	@Version
	private Integer changeControlId;

	public LVCCAllowListEntryEntity() {

	}

	public LVCCAllowListEntryEntity(final VerificationCardSetEntity verificationCardSetEntity, final String longVoteCastReturnCode) {
		this.verificationCardSetEntity = checkNotNull(verificationCardSetEntity);
		this.longVoteCastReturnCode = checkNotNull(longVoteCastReturnCode);
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return this.verificationCardSetEntity;
	}

	public String getLongVoteCastReturnCode() {
		return this.longVoteCastReturnCode;
	}
}
