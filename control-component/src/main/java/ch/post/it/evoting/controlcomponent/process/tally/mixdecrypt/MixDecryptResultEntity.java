/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.controlcomponent.process.ElectionEventEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@Table(name = "MIX_DECRYPT_RESULT")
public class MixDecryptResultEntity {

	@Id
	private String ballotBoxId;

	@ManyToOne
	@JoinColumn(name = "ELECTION_EVENT_FK_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray verifiableShuffle;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray verifiableDecryptions;

	@Version
	private Integer changeControlId;

	public MixDecryptResultEntity() {
		// Intentionally left blank.
	}

	public MixDecryptResultEntity(final String ballotBoxId, final ElectionEventEntity electionEventEntity,
			final ImmutableByteArray verifiableShuffle, final ImmutableByteArray verifiableDecryptions) {

		this.ballotBoxId = validateUUID(ballotBoxId);
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.verifiableShuffle = checkNotNull(verifiableShuffle);
		this.verifiableDecryptions = checkNotNull(verifiableDecryptions);
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public ImmutableByteArray getVerifiableShuffle() {
		return verifiableShuffle;
	}

	public ImmutableByteArray getVerifiableDecryptions() {
		return verifiableDecryptions;
	}
}
