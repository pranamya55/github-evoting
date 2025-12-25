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
@Table(name = "CCM_ELECTION_KEY")
public class CcmjElectionKeysEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	@Version
	private Integer changeControlId;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray ccmjElectionKeyPair;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray ccmjSchnorrProofs;

	public CcmjElectionKeysEntity() {
		// Needed by the repository.
	}

	public CcmjElectionKeysEntity(final ElectionEventEntity electionEventEntity, final ImmutableByteArray ccmjElectionKeyPair,
			final ImmutableByteArray ccmjSchnorrProofs) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.ccmjElectionKeyPair = checkNotNull(ccmjElectionKeyPair);
		this.ccmjSchnorrProofs = checkNotNull(ccmjSchnorrProofs);
	}

	public ImmutableByteArray getCcmjElectionKeyPair() {
		return ccmjElectionKeyPair;
	}

	public ImmutableByteArray getCcmjSchnorrProofs() {
		return ccmjSchnorrProofs;
	}
}
