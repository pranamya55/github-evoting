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
@Table(name = "ENCRYPTED_VERIFIABLE_VOTE")
public class EncryptedVerifiableVoteEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray contextIds;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray encryptedVote;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray exponentiatedEncryptedVote;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray encryptedPartialChoiceReturnCodes;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray exponentiationProof;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray plaintextEqualityProof;

	@Version
	private Integer changeControlId;

	public EncryptedVerifiableVoteEntity() {
	}

	private EncryptedVerifiableVoteEntity(final ImmutableByteArray contextIds, final ImmutableByteArray encryptedVote,
			final ImmutableByteArray exponentiatedEncryptedVote, final ImmutableByteArray encryptedPartialChoiceReturnCodes,
			final ImmutableByteArray exponentiationProof, final ImmutableByteArray plaintextEqualityProof,
			final VerificationCardEntity verificationCardEntity) {
		this.contextIds = contextIds;
		this.encryptedVote = encryptedVote;
		this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
		this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
		this.exponentiationProof = exponentiationProof;
		this.plaintextEqualityProof = plaintextEqualityProof;
		this.verificationCardEntity = verificationCardEntity;
	}

	public ImmutableByteArray getEncryptedVote() {
		return encryptedVote;
	}

	public ImmutableByteArray getExponentiatedEncryptedVote() {
		return exponentiatedEncryptedVote;
	}

	public ImmutableByteArray getEncryptedPartialChoiceReturnCodes() {
		return encryptedPartialChoiceReturnCodes;
	}

	public ImmutableByteArray getExponentiationProof() {
		return exponentiationProof;
	}

	public ImmutableByteArray getPlaintextEqualityProof() {
		return plaintextEqualityProof;
	}

	public ImmutableByteArray getContextIds() {
		return contextIds;
	}

	public Integer getChangeControlId() {
		return changeControlId;
	}

	public VerificationCardEntity getVerificationCardEntity() {
		return verificationCardEntity;
	}

	public static class Builder {

		private ImmutableByteArray contextIds;
		private ImmutableByteArray encryptedVote;
		private ImmutableByteArray exponentiatedEncryptedVote;
		private ImmutableByteArray encryptedPartialChoiceReturnCodes;
		private ImmutableByteArray exponentiationProof;
		private ImmutableByteArray plaintextEqualityProof;
		private VerificationCardEntity verificationCardEntity;

		public Builder setContextIds(final ImmutableByteArray contextIds) {
			this.contextIds = checkNotNull(contextIds);
			return this;
		}

		public Builder setEncryptedVote(final ImmutableByteArray encryptedVote) {
			this.encryptedVote = checkNotNull(encryptedVote);
			return this;
		}

		public Builder setExponentiatedEncryptedVote(final ImmutableByteArray exponentiatedEncryptedVote) {
			this.exponentiatedEncryptedVote = checkNotNull(exponentiatedEncryptedVote);
			return this;
		}

		public Builder setEncryptedPartialChoiceReturnCodes(final ImmutableByteArray encryptedPartialChoiceReturnCodes) {
			this.encryptedPartialChoiceReturnCodes = checkNotNull(encryptedPartialChoiceReturnCodes);
			return this;
		}

		public Builder setExponentiationProof(final ImmutableByteArray exponentiationProof) {
			this.exponentiationProof = checkNotNull(exponentiationProof);
			return this;
		}

		public Builder setPlaintextEqualityProof(final ImmutableByteArray plaintextEqualityProof) {
			this.plaintextEqualityProof = checkNotNull(plaintextEqualityProof);
			return this;
		}

		public Builder setVerificationCardEntity(final VerificationCardEntity verificationCardEntity) {
			this.verificationCardEntity = verificationCardEntity;
			return this;
		}

		public EncryptedVerifiableVoteEntity build() {
			checkNotNull(contextIds);
			checkNotNull(encryptedVote);
			checkNotNull(exponentiatedEncryptedVote);
			checkNotNull(encryptedPartialChoiceReturnCodes);
			checkNotNull(exponentiationProof);
			checkNotNull(plaintextEqualityProof);
			checkNotNull(verificationCardEntity);

			return new EncryptedVerifiableVoteEntity(contextIds, encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes,
					exponentiationProof, plaintextEqualityProof, verificationCardEntity);
		}
	}
}
