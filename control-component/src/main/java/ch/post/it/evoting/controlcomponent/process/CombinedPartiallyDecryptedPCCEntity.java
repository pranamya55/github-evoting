/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Column;
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
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

/**
 * Contains the exponentiated gammas and corresponding exponentiation proofs of all control components.
 */
@Entity
@Table(name = "COMBINED_PARTIALLY_DECRYPTED_PCC")
public class CombinedPartiallyDecryptedPCCEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Column(name = "COMBINED_PARTIALLY_DECRYPTED_PCC")
	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray combinedPartiallyDecryptedPCC;

	@Version
	private Integer changeControlId;

	public CombinedPartiallyDecryptedPCCEntity() {
	}

	public CombinedPartiallyDecryptedPCCEntity(final VerificationCardEntity verificationCardEntity,
			final ImmutableByteArray combinedPartiallyDecryptedPCC) {

		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.combinedPartiallyDecryptedPCC = checkNotNull(combinedPartiallyDecryptedPCC);
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public ImmutableByteArray getCombinedPartiallyDecryptedPCC() {
		return combinedPartiallyDecryptedPCC;
	}

}
