/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

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

import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "PARTIALLY_DECRYPTED_PCC")
public class PartiallyDecryptedPCCEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Version
	private Integer changeControlId;

	@Column(name = "PARTIALLY_DECRYPTED_ENCRYPTED_PCC")
	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray partiallyDecryptedEncryptedPCC;

	public PartiallyDecryptedPCCEntity() {
	}

	public PartiallyDecryptedPCCEntity(final VerificationCardEntity verificationCardEntity, final ImmutableByteArray partiallyDecryptedEncryptedPCC) {
		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.partiallyDecryptedEncryptedPCC = checkNotNull(partiallyDecryptedEncryptedPCC);
	}

	public ImmutableByteArray getPartiallyDecryptedEncryptedPCC() {
		return partiallyDecryptedEncryptedPCC;
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

}
