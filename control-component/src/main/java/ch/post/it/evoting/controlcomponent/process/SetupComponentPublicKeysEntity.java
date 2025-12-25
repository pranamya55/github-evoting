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
	private ImmutableByteArray combinedControlComponentPublicKeys;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electoralBoardPublicKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electoralBoardSchnorrProofs;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray electionPublicKey;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray choiceReturnCodesEncryptionPublicKey;

	@Version
	@Column(name = "CHANGE_CONTROL_ID")
	private Integer changeControlId;

	private SetupComponentPublicKeysEntity(final ElectionEventEntity electionEventEntity, final ImmutableByteArray combinedControlComponentPublicKeys,
			final ImmutableByteArray electoralBoardPublicKey, final ImmutableByteArray electoralBoardSchnorrProofs,
			final ImmutableByteArray electionPublicKey, final ImmutableByteArray choiceReturnCodesEncryptionPublicKey) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.combinedControlComponentPublicKeys = checkNotNull(combinedControlComponentPublicKeys);
		this.electoralBoardPublicKey = checkNotNull(electoralBoardPublicKey);
		this.electoralBoardSchnorrProofs = checkNotNull(electoralBoardSchnorrProofs);
		this.electionPublicKey = checkNotNull(electionPublicKey);
		this.choiceReturnCodesEncryptionPublicKey = checkNotNull(choiceReturnCodesEncryptionPublicKey);
	}

	public SetupComponentPublicKeysEntity() {

	}

	public ImmutableByteArray getCombinedControlComponentPublicKeys() {
		return combinedControlComponentPublicKeys;
	}

	public ImmutableByteArray getElectoralBoardPublicKey() {
		return electoralBoardPublicKey;
	}

	public ImmutableByteArray getElectoralBoardSchnorrProofs() {
		return electoralBoardSchnorrProofs;
	}

	public ImmutableByteArray getElectionPublicKey() {
		return electionPublicKey;
	}

	public ImmutableByteArray getChoiceReturnCodesEncryptionPublicKey() {
		return choiceReturnCodesEncryptionPublicKey;
	}

	public static class Builder {
		private ElectionEventEntity electionEventEntity;
		private ImmutableByteArray combinedControlComponentPublicKeys;
		private ImmutableByteArray electoralBoardPublicKey;
		private ImmutableByteArray electoralBoardSchnorrProofs;
		private ImmutableByteArray electionPublicKey;
		private ImmutableByteArray choiceReturnCodesEncryptionPublicKey;

		public Builder() {
			// Do nothing
		}

		public Builder setElectionEventEntity(final ElectionEventEntity electionEventEntity) {
			this.electionEventEntity = checkNotNull(electionEventEntity);
			return this;
		}

		public Builder setCombinedControlComponentPublicKey(final ImmutableByteArray combinedControlComponentPublicKeys) {
			checkNotNull(combinedControlComponentPublicKeys);
			this.combinedControlComponentPublicKeys = combinedControlComponentPublicKeys;
			return this;
		}

		public Builder setElectoralBoardPublicKey(final ImmutableByteArray electoralBoardPublicKey) {
			checkNotNull(electoralBoardPublicKey);
			this.electoralBoardPublicKey = electoralBoardPublicKey;
			return this;
		}

		public Builder setElectoralBoardSchnorrProofs(final ImmutableByteArray electoralBoardSchnorrProofs) {
			checkNotNull(electoralBoardSchnorrProofs);
			this.electoralBoardSchnorrProofs = electoralBoardSchnorrProofs;
			return this;
		}

		public Builder setElectionPublicKey(final ImmutableByteArray electionPublicKey) {
			checkNotNull(electionPublicKey);
			this.electionPublicKey = electionPublicKey;
			return this;
		}

		public Builder setChoiceReturnCodesEncryptionPublicKey(final ImmutableByteArray choiceReturnCodesEncryptionPublicKey) {
			checkNotNull(choiceReturnCodesEncryptionPublicKey);
			this.choiceReturnCodesEncryptionPublicKey = choiceReturnCodesEncryptionPublicKey;
			return this;
		}

		public SetupComponentPublicKeysEntity build() {
			return new SetupComponentPublicKeysEntity(electionEventEntity, combinedControlComponentPublicKeys, electoralBoardPublicKey,
					electoralBoardSchnorrProofs, electionPublicKey, choiceReturnCodesEncryptionPublicKey);
		}
	}
}
