/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;

class MixDecOfflineInputTest {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final GqGroup GQ_GROUP = GroupTestData.getLargeGqGroup();

	private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes;
	private ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords;

	@BeforeEach
	void setupEach() {
		final int n = RANDOM.nextInt(4) + 2;
		final int numberOfAllowedWriteInsPlusOne = RANDOM.nextInt(5) + 1;
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(GQ_GROUP);
		partiallyDecryptedVotes = elGamalGenerator.genRandomCiphertextVector(n, numberOfAllowedWriteInsPlusOne);
		electoralBoardMembersPasswords = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1_2".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2_2".toCharArray()));
	}

	@Test
	void constructMixDecOfflineInputWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new MixDecOfflineInput(null, electoralBoardMembersPasswords));
		assertThrows(NullPointerException.class, () -> new MixDecOfflineInput(partiallyDecryptedVotes, null));
	}

	@Test
	void constructWithInvalidPasswordsThrows() {
		final ImmutableList<SafePasswordHolder> invalidElectoralBoardMembersPasswords = ImmutableList.of(
				new SafePasswordHolder("invalid".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2_2".toCharArray()));
		assertThrows(IllegalArgumentException.class, () -> new MixDecOfflineInput(partiallyDecryptedVotes, invalidElectoralBoardMembersPasswords));
	}
}
