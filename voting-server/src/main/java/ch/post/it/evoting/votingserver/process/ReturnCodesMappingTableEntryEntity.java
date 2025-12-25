/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "RETURN_CODES_MAPPING_TABLE_ENTRY")
public class ReturnCodesMappingTableEntryEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_SET_ID", referencedColumnName = "VERIFICATION_CARD_SET_ID")
	private VerificationCardSetEntity verificationCardSetEntity;

	@Id
	private String hashedLongReturnCode;

	@SuppressWarnings("java:S1068") // Used in queries directly.
	private String encryptedShortReturnCode;

	@Version
	private Integer changeControlId;

	public ReturnCodesMappingTableEntryEntity() {
	}

	public ReturnCodesMappingTableEntryEntity(final VerificationCardSetEntity verificationCardSetEntity, final String hashedLongReturnCode,
			final String encryptedShortReturnCode) {
		this.verificationCardSetEntity = checkNotNull(verificationCardSetEntity);
		this.hashedLongReturnCode = checkNotNull(hashedLongReturnCode);
		this.encryptedShortReturnCode = checkNotNull(encryptedShortReturnCode);
	}

}
