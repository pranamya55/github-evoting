/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;

/**
 * Implements the MixDecOffline algorithm.
 */
@Service
@ConditionalOnProperty("role.isTally")
public class MixDecOfflineAlgorithm {

	private final Hash hash;
	private final Mixnet mixnet;
	private final BallotBoxService ballotBoxService;
	private final ZeroKnowledgeProof zeroKnowledgeProof;

	public MixDecOfflineAlgorithm(
			final Hash hash,
			final Mixnet mixnet,
			final BallotBoxService ballotBoxService,
			final ZeroKnowledgeProof zeroKnowledgeProof) {
		this.hash = hash;
		this.mixnet = mixnet;
		this.ballotBoxService = ballotBoxService;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
	}

	/**
	 * Shuffles (and re-encrypts) the votes and performs the final decryption.
	 *
	 * @param context the context data
	 * @param input   the input data
	 * @throws NullPointerException     if the context or the input is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>there are less than 2 votes.</li>
	 *                                      <li>the partially encrypted votes do not have exactly number of allowed write-ins + 1 elements.</li>
	 *                                      <li>the partially encrypted votes have more elements than the electoral board keys.</li>
	 *                                      <li>there is less than 2 electoral board members.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings({ "java:S117", "java:S1488" }) // This is intended in order to have a better alignment with the specification.
	public MixDecOfflineOutput mixDecOffline(final MixDecOfflineContext context, final MixDecOfflineInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.getEncryptionGroup().equals(input.partiallyDecryptedVotes().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final String ee = context.getElectionEventId();
		final String bb = context.getBallotBoxId();
		final int delta = context.getNumberOfAllowedWriteInsPlusOne();

		// Input.
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_dec_4 = input.partiallyDecryptedVotes();
		final ImmutableList<ImmutableByteArray> PW = input.electoralBoardMembersPasswords().stream()
				.map(SafePasswordHolder::get)
				.map(CharBuffer::wrap)
				.map(StandardCharsets.UTF_8::encode)
				.map(ByteBuffer::array)
				.map(ImmutableByteArray::new)
				.collect(toImmutableList());

		// Cross-checks.
		checkArgument(c_dec_4.getElementSize() == delta,
				"The number of elements in the partially decrypted votes must correspond to the number of allowed write-ins + 1. [l: %s, delta: %s]",
				c_dec_4.getElementSize(), delta);

		// Require.
		final int N_c_hat = c_dec_4.size();
		final int k = PW.size();
		checkArgument(N_c_hat >= 2, "There must be at least 2 partially decrypted votes. [N_c_hat: %s]", N_c_hat);
		checkArgument(k >= 2, "There must be at least 2 electoral board members. [k: %s]", k);
		// Corresponds to bb ∉ L_bb,Tally.
		checkArgument(!ballotBoxService.isDecrypted(bb), "The ballot box has already been decrypted. [ballotBoxId: %s]", bb);

		// Operation.
		final GroupVector<ZqElement, ZqGroup> EB_sk_elements = IntStream.range(0, delta).parallel()
				.mapToObj(i -> {
							final ZqElement EB_sk_i = hash.recursiveHashToZq(q,
									Streams.concat(Stream.of(
															HashableString.from("ElectoralBoardSecretKey"),
															HashableString.from(ee),
															HashableBigInteger.from(BigInteger.valueOf(i))),
													PW.stream())
											.collect(HashableList.toHashableList()));
							// EB_pk is computed during the generation of the key pair (EB_pk, EB_sk).
							return EB_sk_i;
						}
				)
				.collect(toGroupVector());
		final ElGamalMultiRecipientPrivateKey EB_sk = new ElGamalMultiRecipientPrivateKey(EB_sk_elements);
		final ElGamalMultiRecipientKeyPair EB_pk_EB_sk = ElGamalMultiRecipientKeyPair.from(EB_sk, p_q_g.getGenerator());
		final ElGamalMultiRecipientPublicKey EB_pk = EB_pk_EB_sk.getPublicKey();

		final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ee, bb, "MixDecOffline");
		final VerifiableShuffle c_mix_5_pi_mix_5 = mixnet.genVerifiableShuffle(c_dec_4, EB_pk);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_mix_5 = c_mix_5_pi_mix_5.shuffledCiphertexts();
		final VerifiableDecryptions c_dec_5_pi_dec_5 = zeroKnowledgeProof.genVerifiableDecryptions(c_mix_5, EB_pk_EB_sk, i_aux);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_dec_5 = c_dec_5_pi_dec_5.getCiphertexts();
		final GroupVector<DecryptionProof, ZqGroup> pi_dec_5 = c_dec_5_pi_dec_5.getDecryptionProofs();

		final GroupVector<ElGamalMultiRecipientMessage, GqGroup> m = IntStream.range(0, N_c_hat)
				.mapToObj(i -> {
					final ElGamalMultiRecipientCiphertext c_dec_5_i = c_dec_5.get(i);
					return new ElGamalMultiRecipientMessage(c_dec_5_i.getPhis());
				})
				.collect(toGroupVector());

		// Corresponds to L_bb,Tally = L_bb,Tally || bb.
		ballotBoxService.setDecrypted(bb);

		// Wipe the passwords after usage.
		input.electoralBoardMembersPasswords().forEach(SafePasswordHolder::clear);

		return new MixDecOfflineOutput(c_mix_5_pi_mix_5, m, pi_dec_5);
	}
}
