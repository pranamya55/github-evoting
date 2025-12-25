/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.validations.PasswordValidation;

/**
 * Contains the inputs of the MixDecOffline algorithm.
 *
 * <ul>
 *     <li>c<sub>dec,4</sub>, the partially decrypted votes. Non-null.</li>
 *     <li>(PW<sub>0</sub>, ..., PW<sub>k-1</sub>), the passwords of the k electoral board members. Non-null.</li>
 * </ul>
 */
public record MixDecOfflineInput(GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes,
								 ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {

	public MixDecOfflineInput {
		checkNotNull(partiallyDecryptedVotes);
		checkNotNull(electoralBoardMembersPasswords);

		electoralBoardMembersPasswords.forEach(pwd -> PasswordValidation.validate(pwd.get(), "electoral board member"));
	}
}
