/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;

/**
 * Regroups the inputs of the GenCMTable algorithm.
 *
 * <ul>
 *    <li>sk<sub>setup</sub>, the setup secret key. Non-null.</li>
 *    <li>c<sub>pC</sub>, the vector of encrypted pre-Choice Return Codes. Non-null.</li>
 *    <li>p<sub>VCC</sub>, the Pre-Vote Cast Return Codes. Non-null.</li>
 * </ul>
 */
public record GenCMTableInput(ElGamalMultiRecipientPrivateKey setupSecretKey,
							  GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes,
							  GroupVector<GqElement, GqGroup> preVoteCastReturnCodes) {

	/**
	 * @throws NullPointerException     if any of the fields is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>Any of the {@code encryptedPreChoiceReturnCodes} and {@code encryptedPreVoteCastReturnCodes} is empty.</li>
	 *                                      <li>The sizes of {@code encryptedPreChoiceReturnCodes} and {@code encryptedPreVoteCastReturnCodes} are not equal.</li>
	 *                                      <li>The GqGroup of {@code encryptedPreChoiceReturnCodes} and {@code encryptedPreVoteCastReturnCodes} are not equal.</li>
	 *                                  </ul>
	 */
	public GenCMTableInput {
		checkNotNull(setupSecretKey);
		encryptedPreChoiceReturnCodes = GroupVector.from(checkNotNull(encryptedPreChoiceReturnCodes));
		preVoteCastReturnCodes = GroupVector.from(checkNotNull(preVoteCastReturnCodes));

		// Input size checks.
		final ImmutableList<Integer> inputsSize = ImmutableList.of(encryptedPreChoiceReturnCodes.size(), preVoteCastReturnCodes.size());
		checkArgument(inputsSize.stream().allMatch(size -> size > 0), "All inputs must not be empty.");
		checkArgument(allEqual(inputsSize.stream(), Function.identity()), "All inputs sizes must be the same.");
		checkArgument(setupSecretKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The setup secret key must have at most n_sup elements. [n_sup: %s]", MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		checkArgument(encryptedPreChoiceReturnCodes.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The size of the encrypted pre-Choice Return Codes must be at most n_sup. [n_sup: %s]", MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

		// Cross-group checks.
		checkArgument(encryptedPreChoiceReturnCodes.getGroup().equals(preVoteCastReturnCodes.getGroup()),
				"All inputs must have the same Gq group.");
	}

	@Override
	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes() {
		return GroupVector.from(encryptedPreChoiceReturnCodes);
	}

	@Override
	public GroupVector<GqElement, GqGroup> preVoteCastReturnCodes() {
		return GroupVector.from(preVoteCastReturnCodes);
	}

	public GqGroup getGroup() {
		return this.encryptedPreChoiceReturnCodes.getGroup();
	}

}
