/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "PCC_ALLOW_LIST_ENTRY")
public class PCCAllowListEntryEntity {

	@Id
	private String partialChoiceReturnCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_FK_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	private int chunkId;

	@Version
	private Integer changeControlId;

	public PCCAllowListEntryEntity() {

	}

	public PCCAllowListEntryEntity(final VerificationCardSetEntity verificationCardSetEntity, final String partialChoiceReturnCode,
			final int chunkId) {
		this.verificationCardSetEntity = verificationCardSetEntity;
		this.partialChoiceReturnCode = partialChoiceReturnCode;
		this.chunkId = chunkId;
	}

	public VerificationCardSetEntity getVerificationCardSetEntity() {
		return verificationCardSetEntity;
	}

	public String getPartialChoiceReturnCode() {
		return partialChoiceReturnCode;
	}

	public int getChunkId() {
		return chunkId;
	}
}
