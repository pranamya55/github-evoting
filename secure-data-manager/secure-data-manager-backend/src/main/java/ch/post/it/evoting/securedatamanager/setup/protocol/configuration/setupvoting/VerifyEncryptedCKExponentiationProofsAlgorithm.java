/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

@Service
@ConditionalOnProperty("role.isSetup")
public class VerifyEncryptedCKExponentiationProofsAlgorithm {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyEncryptedCKExponentiationProofsAlgorithm.class);

	private final ZeroKnowledgeProof zeroKnowledgeProof;

	public VerifyEncryptedCKExponentiationProofsAlgorithm(final ZeroKnowledgeProof zeroKnowledgeProof) {
		this.zeroKnowledgeProof = zeroKnowledgeProof;
	}

	/**
	 * Verifies the Confirmation Key's exponentiation proofs of a list of verification card IDs.
	 *
	 * @param context the {@link VerifyEncryptedExponentiationProofsContext}. Must be non-null.
	 * @param input   the {@link VerifyEncryptedCKExponentiationProofsInput}. Must be non-null.
	 * @return true if the proofs are valid for all {@code id}, false otherwise.
	 */
	@SuppressWarnings("java:S117")
	public boolean verifyEncryptedCKExponentiationProofs(final VerifyEncryptedExponentiationProofsContext context,
			final VerifyEncryptedCKExponentiationProofsInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.encryptedHashedConfirmationKey().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final int j = context.nodeId();
		final String ee = context.electionEventId();
		final ImmutableList<String> vc = context.verificationCardIds();
		final int N_E = vc.size();

		// Input.
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_ck = input.encryptedHashedConfirmationKey();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> Kc_j = input.voterVoteCastReturnCodeGenerationPublicKeys();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expCK_j = input.exponentiatedEncryptedHashedConfirmationKey();
		final GroupVector<ExponentiationProof, ZqGroup> pi_expCK_j = input.proofsOfCorrectExponentiation();

		// Cross-size validations.
		checkArgument(c_ck.size() == N_E, "The size of each input must be equal to the number of eligible voters.");

		// Operation.
		return IntStream.range(0, N_E)
				.parallel()
				.mapToObj(id -> {
					final GroupVector<GqElement, GqGroup> g = Stream.concat(Stream.of(p_q_g.getGenerator()), c_ck.get(id).stream())
							.collect(GroupVector.toGroupVector());

					final GroupVector<GqElement, GqGroup> y = Stream.concat(Kc_j.get(id).getKeyElements().stream(), c_expCK_j.get(id).stream())
							.collect(GroupVector.toGroupVector());

					final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ee, vc.get(id), "GenEncLongCodeShares", integerToString(j));

					final boolean exponentiationVerif_id = zeroKnowledgeProof.verifyExponentiation(g, y, pi_expCK_j.get(id), i_aux);

					if (exponentiationVerif_id) {
						LOGGER.debug("The Confirmation Key's exponentiation proof is valid. [ee: {}, j: {}, vc_id: {}]", ee, j, vc.get(id));
					} else {
						LOGGER.error("The Confirmation Key's exponentiation proof is invalid. [ee: {}, j: {}, vc_id: {}]", ee, j, vc.get(id));
					}
					return exponentiationVerif_id;
				})
				.reduce(Boolean::logicalAnd)
				.orElse(Boolean.FALSE);
	}
}
