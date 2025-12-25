/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;

/**
 * Regroups the output values needed by the GenVerDat algorithm.
 *
 * <ul>
 *     <li>vc, the vector of verification card ids. Non-null.</li>
 *     <li>SVK, the vector of Start Voting Keys. Non-null.</li>
 *     <li>K, the vector of verification card public keys. Non-null.</li>
 *     <li>k, the vector of verification card secret keys. Non-null.</li>
 *     <li>LpCC, the Partial Choice Return Codes allow list. Non-null.</li>
 *     <li>BCK, the vector of ballot casting keys. Non-null.</li>
 *     <li>cpCC, the vector of encrypted, hashed partial Choice Return Codes. Non-null.</li>
 *     <li>cck, the vector of encrypted, hashed Confirmation Keys. Non-null.</li>
 * </ul>
 */
public class GenVerDatOutput {

	private final int size;
	private final GqGroup gqGroup;

	private final ImmutableList<String> verificationCardIds;
	private final ImmutableList<String> startVotingKeys;
	private final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs;
	private final ImmutableList<String> partialChoiceReturnCodesAllowList;
	private final ImmutableList<String> ballotCastingKeys;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

	private GenVerDatOutput(
			final ImmutableList<String> verificationCardIds,
			final ImmutableList<String> startVotingKeys,
			final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs,
			final ImmutableList<String> partialChoiceReturnCodesAllowList,
			final ImmutableList<String> ballotCastingKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys) {

		checkNotNull(verificationCardIds);
		checkNotNull(startVotingKeys);
		checkNotNull(verificationCardKeyPairs);
		checkNotNull(partialChoiceReturnCodesAllowList);
		checkNotNull(ballotCastingKeys);
		checkNotNull(encryptedHashedConfirmationKeys);

		checkArgument(!verificationCardIds.isEmpty(), "The output must not be empty.");
		checkArgument(!startVotingKeys.isEmpty(), "The start voting keys must not be empty.");

		this.size = verificationCardIds.size();
		checkArgument(this.size == verificationCardKeyPairs.size() && this.size == startVotingKeys.size()
				&& this.size == ballotCastingKeys.size() && this.size == encryptedHashedPartialChoiceReturnCodes.size()
				&& this.size == encryptedHashedConfirmationKeys.size(), "All vectors must have the same size.");

		final int n = encryptedHashedPartialChoiceReturnCodes.getElementSize();
		checkArgument(partialChoiceReturnCodesAllowList.size() == n * this.size,
				String.format("There must be %d elements in the allow list.", n * this.size));

		partialChoiceReturnCodesAllowList.forEach(
				element -> checkArgument(validateBase64Encoded(element).length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
						String.format("Elements in allowList must be of length %s.", BASE64_ENCODED_HASH_OUTPUT_LENGTH)));

		final GqGroup group = verificationCardKeyPairs.get(0).getGroup();
		checkArgument(group.equals(encryptedHashedPartialChoiceReturnCodes.get(0).getGroup()) && group
				.equals(encryptedHashedConfirmationKeys.get(0).getGroup()), "All vectors must belong to the same group.");

		checkArgument(hasNoDuplicates(verificationCardIds), "The vector of verification card ids must not contain any duplicated element.");

		this.gqGroup = group;
		this.verificationCardIds = verificationCardIds;
		this.startVotingKeys = startVotingKeys;
		this.verificationCardKeyPairs = verificationCardKeyPairs;
		this.partialChoiceReturnCodesAllowList = partialChoiceReturnCodesAllowList;
		this.ballotCastingKeys = ballotCastingKeys;
		this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
		this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public ImmutableList<String> getStartVotingKeys() {
		return startVotingKeys;
	}

	public ImmutableList<ElGamalMultiRecipientKeyPair> getVerificationCardKeyPairs() {
		return verificationCardKeyPairs;
	}

	public ImmutableList<String> getPartialChoiceReturnCodesAllowList() {
		return partialChoiceReturnCodesAllowList;
	}

	public ImmutableList<String> getBallotCastingKeys() {
		return ballotCastingKeys;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedPartialChoiceReturnCodes() {
		return encryptedHashedPartialChoiceReturnCodes;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedConfirmationKeys() {
		return encryptedHashedConfirmationKeys;
	}

	public int size() {
		return this.size;
	}

	public GqGroup getGroup() {
		return this.gqGroup;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final GenVerDatOutput that = (GenVerDatOutput) o;
		return size == that.size && gqGroup.equals(that.gqGroup) && verificationCardIds.equals(that.verificationCardIds) && startVotingKeys.equals(
				that.startVotingKeys) && verificationCardKeyPairs.equals(that.verificationCardKeyPairs) && partialChoiceReturnCodesAllowList.equals(
				that.partialChoiceReturnCodesAllowList) && ballotCastingKeys.equals(that.ballotCastingKeys)
				&& encryptedHashedPartialChoiceReturnCodes.equals(that.encryptedHashedPartialChoiceReturnCodes)
				&& encryptedHashedConfirmationKeys.equals(
				that.encryptedHashedConfirmationKeys);
	}

	@Override
	public int hashCode() {
		return Objects.hash(size, gqGroup, verificationCardIds, startVotingKeys, verificationCardKeyPairs, partialChoiceReturnCodesAllowList,
				ballotCastingKeys, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys);
	}

	public static class Builder {
		private ImmutableList<String> verificationCardIds;
		private ImmutableList<String> startVotingKeys;
		private ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs;
		private ImmutableList<String> partialChoiceReturnCodesAllowList;
		private ImmutableList<String> ballotCastingKeys;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

		public Builder setVerificationCardIds(final ImmutableList<String> verificationCardIds) {
			this.verificationCardIds = verificationCardIds;
			return this;
		}

		public Builder setStartVotingKeys(final ImmutableList<String> startVotingKeys) {
			this.startVotingKeys = startVotingKeys;
			return this;
		}

		public Builder setVerificationCardKeyPairs(final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs) {
			this.verificationCardKeyPairs = verificationCardKeyPairs;
			return this;
		}

		public Builder setPartialChoiceReturnCodesAllowList(final ImmutableList<String> partialChoiceReturnCodesAllowList) {
			this.partialChoiceReturnCodesAllowList = partialChoiceReturnCodesAllowList;
			return this;
		}

		public Builder setBallotCastingKeys(final ImmutableList<String> ballotCastingKeys) {
			this.ballotCastingKeys = ballotCastingKeys;
			return this;
		}

		public Builder setEncryptedHashedPartialChoiceReturnCodes(
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes) {
			this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
			return this;
		}

		public Builder setEncryptedHashedConfirmationKeys(
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys) {
			this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
			return this;
		}

		public GenVerDatOutput build() {
			return new GenVerDatOutput(verificationCardIds, startVotingKeys, verificationCardKeyPairs, partialChoiceReturnCodesAllowList,
					ballotCastingKeys, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys);
		}
	}

}
