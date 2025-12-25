/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.VerifiableShuffleGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;

@DisplayName("Construct MixDecOfflineOutput with")
class MixDecOfflineOutputTest {

	private static final SecureRandom RANDOM = new SecureRandom();

	private GqGroup gqGroup;
	@SuppressWarnings("java:S116") // Aligns with our algorithms coding conventions.
	private int N;
	private int l;
	private VerifiableShuffle verifiableShuffle;
	private GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes;
	private GroupVector<DecryptionProof, ZqGroup> decryptionProofs;

	@BeforeEach
	void setup() {
		gqGroup = GroupTestData.getGqGroup();
		N = RANDOM.nextInt(5) + 2;
		l = RANDOM.nextInt(3) + 1;
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		verifiableShuffle = createVerifiableShuffle(gqGroup, N, l);
		decryptedVotes = elGamalGenerator.genRandomMessageVector(N, l);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));
		decryptionProofs = Stream.generate(() -> zqGroupGenerator.genRandomZqElementVector(l))
				.limit(N)
				.map(z -> new DecryptionProof(zqGroupGenerator.genRandomZqElementMember(), z))
				.collect(GroupVector.toGroupVector());
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new MixDecOfflineOutput(null, decryptedVotes, decryptionProofs));
		assertThrows(NullPointerException.class, () -> new MixDecOfflineOutput(verifiableShuffle, null, decryptionProofs));
		assertThrows(NullPointerException.class, () -> new MixDecOfflineOutput(verifiableShuffle, decryptedVotes, null));
	}

	@Test
	@DisplayName("the verifiable shuffle having a different group than the decrypted votes throws an IllegalArgumentException")
	void constructWithVerifiableShuffleDifferentGroupThrows() {
		final GqGroup otherGqGroup = GroupTestData.getDifferentGqGroup(gqGroup);
		final VerifiableShuffle otherVerifiableShuffle = createVerifiableShuffle(otherGqGroup, N, l);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new MixDecOfflineOutput(otherVerifiableShuffle, decryptedVotes, decryptionProofs));
		assertEquals("The shuffled votes must have the same group as the decrypted votes.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the decrypted votes having a different group order than the decryption proofs throws an IllegalArgumentException")
	void constructWithDecryptedVotesDifferentGroupOrderThanDecryptionProofsThrows() {
		final GqGroup otherGqGroup = GroupTestData.getDifferentGqGroup(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(otherGqGroup));
		final GroupVector<DecryptionProof, ZqGroup> otherDecryptionProofs = Stream.generate(() -> zqGroupGenerator.genRandomZqElementVector(l))
				.limit(N)
				.map(z -> new DecryptionProof(zqGroupGenerator.genRandomZqElementMember(), z))
				.collect(GroupVector.toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new MixDecOfflineOutput(verifiableShuffle, decryptedVotes, otherDecryptionProofs));
		assertEquals("The decrypted votes and the decryption proofs must have the same group order.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the shuffled votes and the decrypted votes having a different vector size throws an IllegalArgumentException")
	void constructWithShuffledVotesDifferentVectorSizeDecryptedVotesThrows() {
		final VerifiableShuffle bigVerifiableShuffle = createVerifiableShuffle(gqGroup, N + 1, l);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new MixDecOfflineOutput(bigVerifiableShuffle, decryptedVotes, decryptionProofs));
		assertEquals("The shuffled votes and the decrypted votes must have the same vector size.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the shuffled votes and the decrypted votes having a different element size throws an IllegalArgumentException")
	void constructWithShuffledVotesDifferentElementSizeDecryptedotesThrows() {
		final VerifiableShuffle longVerifiableShuffle = createVerifiableShuffle(gqGroup, N, l + 1);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new MixDecOfflineOutput(longVerifiableShuffle, decryptedVotes, decryptionProofs));
		assertEquals("The shuffled votes and the decrypted votes must have the same element size.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the decrypted votes and the decryption proofs having a different vector size throws an IllegalArgumentException")
	void constructWithDecryptedVotesDifferentVectorSizeDecryptionProofsThrows() {
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));
		final GroupVector<DecryptionProof, ZqGroup> bigDecryptionProofs = Stream.generate(() -> zqGroupGenerator.genRandomZqElementVector(l))
				.limit(N + 1)
				.map(z -> new DecryptionProof(zqGroupGenerator.genRandomZqElementMember(), z))
				.collect(GroupVector.toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new MixDecOfflineOutput(verifiableShuffle, decryptedVotes, bigDecryptionProofs));
		assertEquals("The decrypted votes and the decryption proofs must have the same vector size.",
				Throwables.getRootCause(exception).getMessage());
	}

	private VerifiableShuffle createVerifiableShuffle(final GqGroup gqGroup, final int N, final int l) {
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledVotes = elGamalGenerator.genRandomCiphertextVector(N, l);
		final ShuffleArgument shuffleArgument = createShuffleArgument(gqGroup, N, l);
		return new VerifiableShuffle(shuffledVotes, shuffleArgument);
	}

	private ShuffleArgument createShuffleArgument(final GqGroup gqGroup, final int N, final int l) {
		return new VerifiableShuffleGenerator(gqGroup).genVerifiableShuffle(N, l).shuffleArgument();
	}
}
