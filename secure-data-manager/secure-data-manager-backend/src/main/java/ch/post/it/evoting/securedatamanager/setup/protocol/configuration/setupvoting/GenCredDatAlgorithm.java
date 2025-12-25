/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray.concat;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashableString.from;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToFixedLengthByteArray;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToByteArray;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricCiphertext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;

/**
 * Implements the GenCredDat algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenCredDatAlgorithm {

	private final Hash hash;
	private final Symmetric symmetric;
	private final Base64 base64;
	private final Argon2 argon2;
	private final GetHashContextAlgorithm getHashContextAlgorithm;

	public GenCredDatAlgorithm(final Hash hash,
			final Symmetric symmetric,
			final Base64 base64,
			@Qualifier("argon2LessMemory")
			final Argon2 argon2,
			final GetHashContextAlgorithm getHashContextAlgorithm) {
		this.hash = hash;
		this.symmetric = symmetric;
		this.base64 = base64;
		this.argon2 = argon2;
		this.getHashContextAlgorithm = getHashContextAlgorithm;
	}

	/**
	 * Generates the voter’s credential data.
	 *
	 * @param context the context data.
	 * @param input   the input data.
	 * @return the generated verification card key stores data as a {@link GenVerDatOutput}.
	 * @throws NullPointerException if {@code GenCredDatContext} or {@code GenCredDatInput} is null.
	 */
	@SuppressWarnings("java:S117")
	public GenCredDatOutput genCredDat(final GenCredDatContext context, final GenCredDatInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group checks.
		checkArgument(context.encryptionGroup().hasSameOrderAs(input.verificationCardSecretKeys().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final String vcs = context.verificationCardSetId();
		final ImmutableList<String> vc = context.getVerificationCardIds();
		final PrimesMappingTable pTable = context.primesMappingTable();
		final ElGamalMultiRecipientPublicKey EL_pk = context.electionPublicKey();
		final ElGamalMultiRecipientPublicKey pk_CCR = context.choiceReturnCodesEncryptionPublicKey();

		// Input.
		final GroupVector<ZqElement, ZqGroup> k = input.verificationCardSecretKeys();
		final ImmutableList<String> SVK = input.startVotingKeys();

		// Cross-check.
		final int N_E = vc.size();
		checkArgument(k.size() == N_E,
				"The size of the vector of verification card secret keys and the vector of Start Voting Keys must be equal to the number of eligible voters. [N_E: %s].",
				N_E);

		// Operation.
		final ImmutableList<String> i_aux = ImmutableList.of("GetKey", getHashContextAlgorithm.getHashContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR));

		final ImmutableList<String> VCks = IntStream.range(0, N_E)
				.parallel()
				.boxed()
				.map(id -> {
					final Argon2Hash argon2Hash = argon2.genArgon2id(stringToByteArray(SVK.get(id)));
					final ImmutableByteArray dSVK_id = argon2Hash.tag();
					final ImmutableByteArray VCks_id_salt = argon2Hash.salt();

					final ImmutableByteArray KSkey_id = hash.recursiveHash(
							HashableList.of(from("VerificationCardKeystore"), from(ee), from(vcs), from(vc.get(id)), dSVK_id));

					final ImmutableByteArray k_id_bytes = integerToFixedLengthByteArray(k.get(id).getValue(),
							Math.ceilDivExact(p_q_g.getQ().bitLength(), Byte.SIZE));
					final SymmetricCiphertext VCks_id_ciphertextSymmetric = symmetric.genCiphertextSymmetric(KSkey_id, k_id_bytes, i_aux);
					final ImmutableByteArray VCks_id_ciphertext = VCks_id_ciphertextSymmetric.ciphertext();
					final ImmutableByteArray VCks_id_nonce = VCks_id_ciphertextSymmetric.nonce();

					return base64.base64Encode(concat(VCks_id_ciphertext, VCks_id_nonce, VCks_id_salt));
				})
				.collect(toImmutableList());

		return new GenCredDatOutput(VCks);
	}
}
