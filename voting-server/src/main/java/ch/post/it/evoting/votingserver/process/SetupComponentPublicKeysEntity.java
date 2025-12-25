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
@Table(name = "SETUP_COMPONENT_PUBLIC_KEYS")
public class SetupComponentPublicKeysEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray choiceReturnCodesEncryptionPublicKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray combinedControlComponentPublicKeys;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electionPublicKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electoralBoardPublicKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electoralBoardSchnorrProofs;

	@Version
	private Integer changeControlId;

	public SetupComponentPublicKeysEntity() {
	}

	public SetupComponentPublicKeysEntity(final ElectionEventEntity electionEventEntity, final ImmutableByteArray combinedControlComponentPublicKeys,
			final ImmutableByteArray electoralBoardPublicKey, final ImmutableByteArray electoralBoardSchnorrProofs,
			final ImmutableByteArray electionPublicKey, final ImmutableByteArray choiceReturnCodesEncryptionPublicKey) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.combinedControlComponentPublicKeys = checkNotNull(combinedControlComponentPublicKeys);
		this.electoralBoardPublicKey = checkNotNull(electoralBoardPublicKey);
		this.electoralBoardSchnorrProofs = checkNotNull(electoralBoardSchnorrProofs);
		this.electionPublicKey = checkNotNull(electionPublicKey);
		this.choiceReturnCodesEncryptionPublicKey = checkNotNull(choiceReturnCodesEncryptionPublicKey);
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public ImmutableByteArray getChoiceReturnCodesEncryptionPublicKey() {
		return choiceReturnCodesEncryptionPublicKey;
	}

	public ImmutableByteArray getCombinedControlComponentPublicKeys() {
		return combinedControlComponentPublicKeys;
	}

	public ImmutableByteArray getElectionPublicKey() {
		return electionPublicKey;
	}

	public ImmutableByteArray getElectoralBoardPublicKey() {
		return electoralBoardPublicKey;
	}

	public ImmutableByteArray getElectoralBoardSchnorrProofs() {
		return electoralBoardSchnorrProofs;
	}

}
