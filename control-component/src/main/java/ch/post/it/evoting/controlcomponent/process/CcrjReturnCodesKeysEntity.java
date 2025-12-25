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
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "CCR_RETURN_CODES_KEYS")
public class CcrjReturnCodesKeysEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	@Version
	private Integer changeControlId;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray ccrjChoiceReturnCodesEncryptionKeyPair;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray ccrjReturnCodesGenerationSecretKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray ccrjSchnorrProofs;

	public CcrjReturnCodesKeysEntity() {
		// Needed by the repository.
	}

	public CcrjReturnCodesKeysEntity(
			final ElectionEventEntity electionEventEntity,
			final ImmutableByteArray ccrjChoiceReturnCodesEncryptionKeyPair,
			final ImmutableByteArray ccrjReturnCodesGenerationSecretKey,
			final ImmutableByteArray ccrjSchnorrProofs) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.ccrjChoiceReturnCodesEncryptionKeyPair = checkNotNull(ccrjChoiceReturnCodesEncryptionKeyPair);
		this.ccrjReturnCodesGenerationSecretKey = checkNotNull(ccrjReturnCodesGenerationSecretKey);
		this.ccrjSchnorrProofs = checkNotNull(ccrjSchnorrProofs);
	}

	public ImmutableByteArray getCcrjChoiceReturnCodesEncryptionKeyPair() {
		return ccrjChoiceReturnCodesEncryptionKeyPair;
	}

	public ImmutableByteArray getCcrjReturnCodesGenerationSecretKey() {
		return ccrjReturnCodesGenerationSecretKey;
	}

	public ImmutableByteArray getCcrjSchnorrProofs() {
		return ccrjSchnorrProofs;
	}
}
