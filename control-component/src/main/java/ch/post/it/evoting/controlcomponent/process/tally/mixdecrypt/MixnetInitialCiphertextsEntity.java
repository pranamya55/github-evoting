/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
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
@Table(name = "MIXNET_INITIAL_CIPHERTEXTS")
public class MixnetInitialCiphertextsEntity {

	@Id
	private String ballotBoxId;

	@ManyToOne
	@JoinColumn(name = "ELECTION_EVENT_FK_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private String encryptedConfirmedVotesHash;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray mixnetInitialCiphertexts;

	@Version
	private Integer changeControlId;

	public MixnetInitialCiphertextsEntity() {
		// Intentionally left blank.
	}

	public MixnetInitialCiphertextsEntity(final String ballotBoxId, final ElectionEventEntity electionEventEntity,
			final String encryptedConfirmedVotesHash, final ImmutableByteArray mixnetInitialCiphertexts) {

		this.ballotBoxId = validateUUID(ballotBoxId);
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.encryptedConfirmedVotesHash = checkNotNull(encryptedConfirmedVotesHash);
		checkArgument(this.encryptedConfirmedVotesHash.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
				"The hash of the encrypted, confirmed votes must be of size %s.", BASE64_ENCODED_HASH_OUTPUT_LENGTH);
		validateBase64Encoded(this.encryptedConfirmedVotesHash);
		this.mixnetInitialCiphertexts = checkNotNull(mixnetInitialCiphertexts);
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public String getEncryptedConfirmedVotesHash() {
		return encryptedConfirmedVotesHash;
	}

	public ImmutableByteArray getMixnetInitialCiphertexts() {
		return mixnetInitialCiphertexts;
	}
}
