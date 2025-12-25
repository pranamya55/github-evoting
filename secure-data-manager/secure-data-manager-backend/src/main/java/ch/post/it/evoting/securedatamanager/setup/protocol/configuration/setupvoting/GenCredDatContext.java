/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the context needed by the GenCredDat algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vcs, the verification card set id. Not null and a valid UUID.</li>
 *     <li>vc, the vector of verification card ids. Non-null and a list of valid UUIDs.</li>
 *     <li>pTable, the primes mapping table of size n. Not null.</li>
 *     <li>EL<sub>pk</sub>, the election public key. Not null.</li>
 *     <li>pk<sub>CCR</sub>, the Choice Return Codes encryption public keys. Not null.</li>
 * </ul>
 */
public final class GenCredDatContext {
	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final ImmutableList<String> verificationCardIds;
	private final PrimesMappingTable primesMappingTable;
	private final ElGamalMultiRecipientPublicKey electionPublicKey;
	private final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;

	private GenCredDatContext(final GqGroup encryptionGroup, final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> verificationCardIds, final PrimesMappingTable primesMappingTable,
			final ElGamalMultiRecipientPublicKey electionPublicKey, final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey) {
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardIds = verificationCardIds;
		this.primesMappingTable = primesMappingTable;
		this.electionPublicKey = electionPublicKey;
		this.choiceReturnCodesEncryptionPublicKey = choiceReturnCodesEncryptionPublicKey;
	}

	public GqGroup encryptionGroup() {
		return encryptionGroup;
	}

	public String electionEventId() {
		return electionEventId;
	}

	public String verificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public PrimesMappingTable primesMappingTable() {
		return primesMappingTable;
	}

	public ElGamalMultiRecipientPublicKey electionPublicKey() {
		return electionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey() {
		return choiceReturnCodesEncryptionPublicKey;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final GenCredDatContext that = (GenCredDatContext) o;
		return encryptionGroup.equals(that.encryptionGroup) && electionEventId.equals(that.electionEventId) && verificationCardSetId.equals(
				that.verificationCardSetId) && verificationCardIds.equals(that.verificationCardIds) && primesMappingTable.equals(
				that.primesMappingTable)
				&& electionPublicKey.equals(that.electionPublicKey) && choiceReturnCodesEncryptionPublicKey.equals(
				that.choiceReturnCodesEncryptionPublicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, verificationCardSetId, verificationCardIds, primesMappingTable, electionPublicKey,
				choiceReturnCodesEncryptionPublicKey);
	}

	public static class Builder {

		private GqGroup encryptionGroup;
		private String electionEventId;
		private String verificationCardSetId;
		private ImmutableList<String> verificationCardIds;
		private PrimesMappingTable primesMappingTable;
		private ElGamalMultiRecipientPublicKey electionPublicKey;
		private ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setVerificationCardSetId(final String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}

		public Builder setVerificationCardIds(final ImmutableList<String> verificationCardIds) {
			this.verificationCardIds = verificationCardIds;
			return this;
		}

		public Builder setPrimesMappingTable(final PrimesMappingTable primesMappingTable) {
			this.primesMappingTable = primesMappingTable;
			return this;
		}

		public Builder setElectionPublicKey(final ElGamalMultiRecipientPublicKey electionPublicKey) {
			this.electionPublicKey = electionPublicKey;
			return this;
		}

		public Builder setChoiceReturnCodesEncryptionPublicKey(final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey) {
			this.choiceReturnCodesEncryptionPublicKey = choiceReturnCodesEncryptionPublicKey;
			return this;
		}

		public GenCredDatContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			validateUUID(verificationCardSetId);
			checkNotNull(primesMappingTable);
			checkNotNull(electionPublicKey);
			checkNotNull(choiceReturnCodesEncryptionPublicKey);
			checkNotNull(verificationCardIds).forEach(Validations::validateUUID);
			checkArgument(hasNoDuplicates(verificationCardIds), "All verificationCardIds must be unique.");
			checkArgument(!verificationCardIds.isEmpty(), "The vector of verification card Ids must have at least one element.");

			checkArgument(allEqual(Stream.of(encryptionGroup, electionPublicKey.getGroup(), choiceReturnCodesEncryptionPublicKey.getGroup(),
					primesMappingTable.getEncryptionGroup()), Function.identity()), "All inputs must have the same group.");

			checkArgument(electionPublicKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The size of the election public key must be smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
					electionPublicKey.size(), MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);
			checkArgument(choiceReturnCodesEncryptionPublicKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
					"The size of the CCR encryption public key must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
					choiceReturnCodesEncryptionPublicKey.size(), MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

			// The constructor of the PrimesMappingTable ensures the pTable is not empty, and the list of actualVotingOptions, encodedVotingOptions
			// semanticInformation and correctness information have the same size.

			// The constructor of the PrimesMappingTableEntries ensures the actualVotingOptions, encodedVotingOptions, semanticInformation and
			// correctness information does not contain null elements.

			return new GenCredDatContext(encryptionGroup, electionEventId, verificationCardSetId, verificationCardIds, primesMappingTable,
					electionPublicKey, choiceReturnCodesEncryptionPublicKey);
		}
	}

}
