/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

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
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORE")
public class SetupComponentVerificationCardKeystoreEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray verificationCardKeystore;

	@Version
	private Integer changeControlId;

	public SetupComponentVerificationCardKeystoreEntity() {
	}

	public SetupComponentVerificationCardKeystoreEntity(final VerificationCardEntity verificationCardEntity,
			final ImmutableByteArray verificationCardKeystore) {
		this.verificationCardEntity = checkNotNull(verificationCardEntity);
		this.verificationCardKeystore = checkNotNull(verificationCardKeystore);
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public ImmutableByteArray getVerificationCardKeystore() {
		return verificationCardKeystore;
	}

}
