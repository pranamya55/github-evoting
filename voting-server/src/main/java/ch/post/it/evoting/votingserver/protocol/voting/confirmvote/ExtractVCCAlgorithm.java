/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.byteArrayToString;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.KEY_DERIVATION_BYTES_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

/**
 * Implements the ExtractVCC algorithm.
 */
@Service
public class ExtractVCCAlgorithm {

	private final Hash hash;
	private final Symmetric symmetric;
	private final KeyDerivation keyDerivation;

	public ExtractVCCAlgorithm(
			final Hash hash,
			final Symmetric symmetric,
			final KeyDerivation keyDerivation) {
		this.hash = hash;
		this.symmetric = symmetric;
		this.keyDerivation = keyDerivation;
	}

	/**
	 * Extracts the short Vote Cast Return Code VCC<sub>id</sub> from the Return Codes Mapping table CMtable.
	 *
	 * @param context the {@link ExtractVCCContext}. Must be non-null.
	 * @param input   the {@link ExtractVCCInput}. Must be non-null.
	 * @return the short Vote Cast Return Code VCC<sub>id</sub>.
	 * @throws NullPointerException     if any of the fields is null.
	 * @throws IllegalArgumentException if the context and input do not have the same group.
	 * @throws IllegalStateException    if the encrypted short Vote Cast Return Code cannot be retrieved from the CMtable.
	 */
	@SuppressWarnings("java:S117")
	public ExtractVCCOutput extractVCC(final ExtractVCCContext context, final ExtractVCCInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.getGroup()), "The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final String vc_id = context.verificationCardId();

		// Input.
		final GroupVector<GqElement, GqGroup> lVCC_id_vector = input.longVoteCastReturnCodeShares();
		final ReturnCodesMappingTable CMtable = input.returnCodesMappingTable();

		// Operation.
		final GqElement identity = p_q_g.getIdentity();
		final GqElement pVCC_id = lVCC_id_vector.stream().reduce(identity, GqElement::multiply);

		final ImmutableByteArray lVCC_id = hash.recursiveHash(pVCC_id, HashableString.from(vc_id), HashableString.from(ee));

		final String key = Base64.getEncoder().encodeToString(hash.recursiveHash(lVCC_id).elements());

		final Optional<String> ctVCC_id_encoded_optional = CMtable.get(key);
		if (ctVCC_id_encoded_optional.isEmpty()) {
			throw new IllegalStateException(
					String.format("Encrypted short Vote Cast Return Code not found in CMtable. [electionEventId: %s, verificationCardId: %s]", ee,
							vc_id));
		} else {
			final String ctVCC_id_encoded = ctVCC_id_encoded_optional.get();

			final ImmutableByteArray ctVCC_id_combined = new ImmutableByteArray(Base64.getDecoder().decode(ctVCC_id_encoded));

			final int length = ctVCC_id_combined.length();

			final int split = length - symmetric.getNonceLength();

			final ImmutableByteArray ctVCC_id_ciphertext = ImmutableByteArray.copyOfRange(ctVCC_id_combined, 0, split);

			final ImmutableByteArray ctVCC_id_nonce = ImmutableByteArray.copyOfRange(ctVCC_id_combined, split, length);

			final int l_KD = KEY_DERIVATION_BYTES_LENGTH;
			final ImmutableByteArray skvcc_id = keyDerivation.KDF(lVCC_id, ImmutableList.emptyList(), l_KD);

			final ImmutableByteArray VCC_id_bytes = symmetric.getPlaintextSymmetric(skvcc_id, ctVCC_id_ciphertext, ctVCC_id_nonce, ImmutableList.emptyList());

			final String VCC_id = byteArrayToString(VCC_id_bytes);

			return new ExtractVCCOutput(VCC_id);
		}
	}

}
