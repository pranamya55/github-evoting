/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.byteArrayToString;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.KEY_DERIVATION_BYTES_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

/**
 * Implements the ExtractCRC algorithm.
 */
@Service
public class ExtractCRCAlgorithm {

	private final Hash hash;
	private final Base64 base64;
	private final Symmetric symmetric;
	private final KeyDerivation keyDerivation;

	public ExtractCRCAlgorithm(
			final Hash hash,
			final Base64 base64,
			final Symmetric symmetric,
			final KeyDerivation keyDerivation) {
		this.hash = hash;
		this.base64 = base64;
		this.symmetric = symmetric;
		this.keyDerivation = keyDerivation;
	}

	/**
	 * Extracts the short Choice Return Codes CC<sub>id</sub> from the Return Codes Mapping table CMtable.
	 *
	 * @param context the {@link ExtractCRCContext} containing necessary group and ids. Must be non-null.
	 * @param input   the {@link ExtractCRCInput} containing all needed inputs. Must be non-null.
	 * @return the short Choice Return Codes CC<sub>id</sub>.
	 * @throws NullPointerException     if any of the fields is null.
	 * @throws IllegalStateException    if an encrypted short Choice Return Code cannot be retrieved from the CMtable.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The blank correctness information and long Choice Return Code shares do not have the same size &psi;.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public ExtractCRCOutput extractCRC(final ExtractCRCContext context, final ExtractCRCInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.getGroup()), "The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final String vc_id = context.verificationCardId();
		final ImmutableList<String> tau_hat = context.blankCorrectnessInformation();

		// Input.
		final ImmutableList<GroupVector<GqElement, GqGroup>> lCC_j_id_vector = input.longChoiceReturnCodeShares();
		final ReturnCodesMappingTable CMtable = input.returnCodesMappingTable();

		// Cross-check.
		final int psi = tau_hat.size();
		checkArgument(psi == lCC_j_id_vector.get(0).size(),
				"The blank correctness information and long Choice Return Code shares must have the same size psi.");

		// Operation.
		ImmutableList<String> CC_id = ImmutableList.emptyList();
		for (int i = 0; i < psi; i++) {
			final int final_i = i;
			final GqElement identity = p_q_g.getIdentity();
			final GqElement pC_id_i = lCC_j_id_vector.stream()
					.map(lCC_j_id -> lCC_j_id.get(final_i))
					.reduce(identity, GqElement::multiply);

			final ImmutableByteArray lCC_id_i = hash.recursiveHash(pC_id_i, HashableString.from(vc_id), HashableString.from(ee),
					HashableString.from(tau_hat.get(i)));

			final String key = base64.base64Encode(hash.recursiveHash(lCC_id_i));

			final Optional<String> ctCC_id_i_encoded_optional = CMtable.get(key);
			if (ctCC_id_i_encoded_optional.isEmpty()) {
				throw new IllegalStateException(
						String.format(
								"Encrypted short Choice Return Code not found in CMtable. [electionEventId: %s, verificationCardId: %s, index: %s]",
								ee, vc_id, i));
			} else {
				final String ctCC_id_i_encoded = ctCC_id_i_encoded_optional.get();

				final ImmutableByteArray ctCC_id_i_combined = base64.base64Decode(ctCC_id_i_encoded);

				final int length = ctCC_id_i_combined.length();

				final int split = length - symmetric.getNonceLength();

				final ImmutableByteArray ctCC_id_i_ciphertext = ImmutableByteArray.copyOfRange(ctCC_id_i_combined, 0, split);

				final ImmutableByteArray ctCC_id_i_nonce = ImmutableByteArray.copyOfRange(ctCC_id_i_combined, split, length);

				final int l_KD = KEY_DERIVATION_BYTES_LENGTH;
				final ImmutableByteArray skcc_id_i = keyDerivation.KDF(lCC_id_i, ImmutableList.emptyList(), l_KD);

				final ImmutableByteArray CC_id_i_bytes = symmetric.getPlaintextSymmetric(skcc_id_i, ctCC_id_i_ciphertext, ctCC_id_i_nonce,
						ImmutableList.emptyList());

				final String CC_id_i = byteArrayToString(CC_id_i_bytes);
				CC_id = CC_id.append(CC_id_i);
			}
		}

		return new ExtractCRCOutput(CC_id);
	}

}
