/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Regroups the context values needed by the VerifyBallotCCR algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the {@code GqGroup} with modulus p, cardinality q and generator g. Not null.</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vcs, the verification card set id. Not null and a valid UUID.</li>
 *     <li>vc<sub>id</sub>, the verification card id. Not null and a valid UUID.</li>
 *     <li>pTable, the primes mapping table. Not null.</li>
 *     <li>K<sub>id</sub>, the verification card public key. Not null.</li>
 *     <li>EL<sub>pk</sub>, the election public key. Not null.</li>
 *     <li>pk<sub>CCR</sub>, the Choice Return Codes encryption public key. Not null.</li>
 * </ul>
 */
public class VerifyBallotCCRContext {

	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final String verificationCardId;
	private final PrimesMappingTable primesMappingTable;
	private final GqElement verificationCardPublicKey;
	private final ElGamalMultiRecipientPublicKey electionPublicKey;
	private final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;

	private VerifyBallotCCRContext(final GqGroup encryptionGroup, final String electionEventId, final String verificationCardSetId,
			final String verificationCardId,
			final PrimesMappingTable primesMappingTable, final GqElement verificationCardPublicKey,
			final ElGamalMultiRecipientPublicKey electionPublicKey,
			final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey) {
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardId = verificationCardId;
		this.primesMappingTable = primesMappingTable;
		this.verificationCardPublicKey = verificationCardPublicKey;
		this.electionPublicKey = electionPublicKey;
		this.choiceReturnCodesEncryptionPublicKey = choiceReturnCodesEncryptionPublicKey;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public PrimesMappingTable getPrimesMappingTable() {
		return primesMappingTable;
	}

	public GqElement getVerificationCardPublicKey() {
		return verificationCardPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getElectionPublicKey() {
		return electionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getChoiceReturnCodesEncryptionPublicKey() {
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
		final VerifyBallotCCRContext that = (VerifyBallotCCRContext) o;
		return encryptionGroup.equals(that.encryptionGroup) && electionEventId.equals(that.electionEventId) && verificationCardSetId.equals(
				that.verificationCardSetId) && verificationCardId.equals(that.verificationCardId) && primesMappingTable.equals(
				that.primesMappingTable)
				&& verificationCardPublicKey.equals(that.verificationCardPublicKey) && electionPublicKey.equals(that.electionPublicKey)
				&& choiceReturnCodesEncryptionPublicKey.equals(that.choiceReturnCodesEncryptionPublicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, verificationCardSetId, verificationCardId, primesMappingTable,
				verificationCardPublicKey,
				electionPublicKey, choiceReturnCodesEncryptionPublicKey);
	}

	public static final class Builder {
		private GqGroup encryptionGroup;
		private String electionEventId;
		private String verificationCardSetId;
		private String verificationCardId;
		private PrimesMappingTable primesMappingTable;
		private GqElement verificationCardPublicKey;
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

		public Builder setVerificationCardId(final String verificationCardId) {
			this.verificationCardId = verificationCardId;
			return this;
		}

		public Builder setPrimesMappingTable(final PrimesMappingTable primesMappingTable) {
			this.primesMappingTable = primesMappingTable;
			return this;
		}

		public Builder setVerificationCardPublicKey(final GqElement verificationCardPublicKey) {
			this.verificationCardPublicKey = verificationCardPublicKey;
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

		public VerifyBallotCCRContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			validateUUID(verificationCardSetId);
			validateUUID(verificationCardId);
			checkNotNull(primesMappingTable);
			checkNotNull(verificationCardPublicKey);
			checkNotNull(electionPublicKey);
			checkNotNull(choiceReturnCodesEncryptionPublicKey);

			final Stream<GqGroup> gqGroups = Stream.of(encryptionGroup, primesMappingTable.getEncryptionGroup(),
					verificationCardPublicKey.getGroup(), electionPublicKey.getGroup(), choiceReturnCodesEncryptionPublicKey.getGroup());
			checkArgument(allEqual(gqGroups, Function.identity()), "All input GqGroups must be the same.");

			return new VerifyBallotCCRContext(encryptionGroup, electionEventId, verificationCardSetId, verificationCardId, primesMappingTable,
					verificationCardPublicKey, electionPublicKey, choiceReturnCodesEncryptionPublicKey);
		}
	}
}
