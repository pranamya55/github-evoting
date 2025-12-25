/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToByteArray;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Streams;

import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

@Service
public class GenEncLongCodeSharesAlgorithm {

	private final KeyDerivation keyDerivation;
	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final VerificationCardService verificationCardService;

	public GenEncLongCodeSharesAlgorithm(
			final KeyDerivation keyDerivation,
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final VerificationCardService verificationCardService) {
		this.keyDerivation = keyDerivation;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.verificationCardService = verificationCardService;
	}

	/**
	 * Generates the encrypted CCR_j long return code shares.
	 *
	 * @param context the {@link GenEncLongCodeSharesContext} containing necessary ids, keys and group. Non-null.
	 * @param input   the {@link GenEncLongCodeSharesInput} input, contains the verification card id, the encrypted hashed partial choice return codes
	 *                and the encrypted hashed confirmation key of a specific voter. Non-null.
	 * @return output the {@link GenEncLongCodeSharesOutput}, contains the output for each verification card.
	 * @throws NullPointerException     if any of the context or input are null.
	 * @throws IllegalArgumentException if any of the voting cards has already been generated.
	 */
	@SuppressWarnings("java:S117")
	@Transactional
	public GenEncLongCodeSharesOutput genEncLongCodeShares(final GenEncLongCodeSharesContext context, final GenEncLongCodeSharesInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.getEncryptionGroup().equals(input.getVerificationCardPublicKeys().getGroup()),
				"The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final GqElement g = p_q_g.getGenerator();
		final int j = context.getNodeId();
		final String ee = context.getElectionEventId();
		final String vcs = context.getVerificationCardSetId();
		final ImmutableList<String> vc = context.getVerificationCardIds();
		final int n = context.getNumberOfVotingOptions();

		// Input.
		final ZqElement k_j_prime = input.getReturnCodesGenerationSecretKey();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_pCC = input.getEncryptedHashedPartialChoiceReturnCodes();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_ck = input.getEncryptedHashedConfirmationKeys();

		// Cross-checks.
		final int N_E = vc.size();
		checkArgument(c_pCC.size() == N_E,
				"There must be as many encrypted hashed partial choice return codes as number of eligible voters. [expected: %s, actual: %s]",
				N_E, c_pCC.size());
		checkArgument(n == c_pCC.getElementSize(),
				"The encrypted hashed partial choice return codes must have n elements. [expected: %s, actual: %s]", n, c_pCC.getElementSize());

		record EncLongCodeShare(GqElement K_j_id, GqElement Kc_j_id, ElGamalMultiRecipientCiphertext c_expPCC_j_id,
								ExponentiationProof pi_expPCC_j_id,
								ElGamalMultiRecipientCiphertext c_expCK_j_id,
								ExponentiationProof pi_expCK_j_id,
								VerificationCard l_genVC_j_id) {
		}

		// Require.
		checkArgument(verificationCardService.existsNone(vc), "At least one of the voting cards has already been generated.");

		// Operation.
		final ImmutableByteArray PRK = integerToByteArray(k_j_prime.getValue());

		final ImmutableList<EncLongCodeShare> encLongCodeShares = IntStream.range(0, N_E)
				.parallel()
				.mapToObj(id -> {
					final String vc_id = vc.get(id);

					final ImmutableList<String> info = ImmutableList.of("VoterChoiceReturnCodeGeneration", ee, vcs, vc_id);

					final ZqElement k_j_id = keyDerivation.KDFToZq(PRK, info, q);

					final GqElement K_j_id = g.exponentiate(k_j_id);

					final ImmutableList<String> info_CK = ImmutableList.of("VoterVoteCastReturnCodeGeneration", ee, vcs, vc_id);

					final ZqElement kc_j_id = keyDerivation.KDFToZq(PRK, info_CK, q);

					final GqElement Kc_j_id = g.exponentiate(kc_j_id);

					final ElGamalMultiRecipientCiphertext c_pCC_id = c_pCC.get(id);
					final ElGamalMultiRecipientCiphertext c_expPCC_j_id = c_pCC_id.getCiphertextExponentiation(k_j_id);

					final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ee, vc_id, "GenEncLongCodeShares", integerToString(j));

					final GroupVector<GqElement, GqGroup> g_c_pCC_id = Streams.concat(Stream.of(g), c_pCC_id.stream()).collect(toGroupVector());
					final GroupVector<GqElement, GqGroup> k_j_id_c_expPCC_j_id = Streams.concat(Stream.of(K_j_id), c_expPCC_j_id.stream())
							.collect(toGroupVector());
					final ExponentiationProof pi_expPCC_j_id = zeroKnowledgeProof.genExponentiationProof(g_c_pCC_id, k_j_id, k_j_id_c_expPCC_j_id,
							i_aux);

					final ElGamalMultiRecipientCiphertext c_ck_id = c_ck.get(id);
					final ElGamalMultiRecipientCiphertext c_expCK_j_id = c_ck_id.getCiphertextExponentiation(kc_j_id);

					final GroupVector<GqElement, GqGroup> g_c_ck_id = Streams.concat(Stream.of(g), c_ck_id.stream()).collect(toGroupVector());
					final GroupVector<GqElement, GqGroup> Kc_j_id_c_expCK_j_id = Streams.concat(Stream.of(Kc_j_id), c_expCK_j_id.stream())
							.collect(toGroupVector());
					final ExponentiationProof pi_expCK_j_id = zeroKnowledgeProof.genExponentiationProof(g_c_ck_id, kc_j_id, Kc_j_id_c_expCK_j_id,
							i_aux);

					// Create verification card for vc_id.
					final ElGamalMultiRecipientPublicKey verificationCardPublicKey = input.getVerificationCardPublicKeys().get(id);
					final VerificationCard verificationCard = new VerificationCard(vc_id, vcs, verificationCardPublicKey);

					return new EncLongCodeShare(K_j_id, Kc_j_id, c_expPCC_j_id, pi_expPCC_j_id, c_expCK_j_id, pi_expCK_j_id, verificationCard);
				})
				.collect(toImmutableList());

		// Save all verification cards. Equivalent to performing L_genVC,j ← L_genVC,j || vc_id for all id.
		verificationCardService.saveAll(encLongCodeShares.stream()
				.map(EncLongCodeShare::l_genVC_j_id)
				.collect(toImmutableList()));

		return new GenEncLongCodeSharesOutput.Builder()
				.setVoterChoiceReturnCodeGenerationPublicKeys(encLongCodeShares.stream()
						.map(EncLongCodeShare::K_j_id)
						.collect(toGroupVector()))
				.setVoterVoteCastReturnCodeGenerationPublicKeys(encLongCodeShares.stream()
						.map(EncLongCodeShare::Kc_j_id)
						.collect(toGroupVector()))
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(encLongCodeShares.stream()
						.map(EncLongCodeShare::c_expPCC_j_id)
						.collect(toGroupVector()))
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(encLongCodeShares.stream()
						.map(EncLongCodeShare::pi_expPCC_j_id)
						.collect(toGroupVector()))
				.setExponentiatedEncryptedHashedConfirmationKeys(encLongCodeShares.stream()
						.map(EncLongCodeShare::c_expCK_j_id)
						.collect(toGroupVector()))
				.setProofsCorrectExponentiationConfirmationKeys(encLongCodeShares.stream()
						.map(EncLongCodeShare::pi_expCK_j_id)
						.collect(toGroupVector()))
				.build();
	}

}
