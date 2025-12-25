/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_CAST_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.KEY_DERIVATION_BYTES_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricCiphertext;
import ch.post.it.evoting.cryptoprimitives.utils.Conversions;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;

/**
 * Implements the GenCMTable algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenCMTableAlgorithm {

	static final int ENCODED_CHOICE_RETURN_CODE_LENGTH = BASE64_ENCODED_HASH_OUTPUT_LENGTH;
	static final int ENCODED_CAST_RETURN_CODE_LENGTH = BASE64_ENCODED_CAST_RETURN_CODE_LENGTH;

	private final Hash hash;
	private final Base64 base64;
	private final Random random;
	private final ElGamal elGamal;
	private final Symmetric symmetric;
	private final KeyDerivation keyDerivation;

	public GenCMTableAlgorithm(
			final Hash hash,
			final Base64 base64,
			final Random random,
			final ElGamal elGamal,
			final Symmetric symmetric,
			final KeyDerivation keyDerivation) {
		this.hash = hash;
		this.base64 = base64;
		this.random = random;
		this.elGamal = elGamal;
		this.symmetric = symmetric;
		this.keyDerivation = keyDerivation;
	}

	/**
	 * Generates the Return Codes Mapping table CMtable that allows the voting server to retrieve the short Choice Return Codes and the short Vote
	 * Cast Return Code.
	 *
	 * @param context the {@link GenCMTableContext} containing necessary ids, keys and group. Non-null.
	 * @param input   the {@link GenCMTableInput} containing all needed inputs. Non-null.
	 * @return the Return Codes Mapping table, the short Choice Return Codes and the short Vote Cast Return Codes encapsulated in the
	 * {@link GenCMTableOutput}.
	 * @throws NullPointerException     if context or input parameters are null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The setup secret key has not n<sub>max</sub> elements.</li>
	 *                                      <li>The size of the vector of encrypted pre-Choice Return Codes differs from the number of eligible voters.</li>
	 *                                      <li>The size of the encrypted pre-Choice Return Code differs from the number of voting options.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public GenCMTableOutput genCMTable(final GenCMTableContext context, final GenCMTableInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group checks.
		checkArgument(context.getEncryptionGroup().equals(input.getGroup()), "The context and input must have the same group.");

		// Context.
		final String ee = context.getElectionEventId();
		final ImmutableList<String> vc = context.getVerificationCardIds();
		final ImmutableList<String> tau = context.getCorrectnessInformation();
		final int n_max = context.getMaximumNumberOfVotingOptions();

		// Input.
		final ElGamalMultiRecipientPrivateKey sk_setup = input.setupSecretKey();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_pC = input.encryptedPreChoiceReturnCodes();
		final GroupVector<GqElement, GqGroup> pVCC = input.preVoteCastReturnCodes();

		// Cross-checks.
		checkArgument(n_max == sk_setup.size(), "The size of the setup secret key must be equal to the maximum number of voting options. [n_max: %s]",
				n_max);
		final int N_E = vc.size();
		checkArgument(N_E == c_pC.size(),
				"The size of the vector of encrypted pre-Choice Return Codes must be equal to the number of eligible voters. [N_E: %s]", N_E);
		final int n = tau.size();
		checkArgument(n == c_pC.getElementSize(),
				"The size of the encrypted pre-Choice Return Code must be equal to the number of voting options. [n: %s]", n);

		final int L_CC = SHORT_CHOICE_RETURN_CODE_LENGTH;
		final int L_VCC = SHORT_VOTE_CAST_RETURN_CODE_LENGTH;

		// Operation.
		final Map<String, String> CMtable = new ConcurrentHashMap<>();

		record ReturnCodes(ImmutableList<String> CC_id, String VCC) {
		}

		final ImmutableList<ReturnCodes> returnCodes = IntStream.range(0, N_E).parallel()
				.mapToObj(id -> {
					final ImmutableList<String> CC_id = random.genUniqueDecimalStrings(L_CC, n);
					final ElGamalMultiRecipientMessage pC_id = elGamal.getMessage(c_pC.get(id), sk_setup);

					final String vc_id = vc.get(id);

					for (int k = 0; k < n; k++) {
						final ImmutableByteArray lCC_id_k = hash.recursiveHash(pC_id.get(k), HashableString.from(vc_id), HashableString.from(ee),
								HashableString.from(tau.get(k)));

						final ImmutableByteArray skcc_id_k = keyDerivation.KDF(lCC_id_k, ImmutableList.emptyList(), KEY_DERIVATION_BYTES_LENGTH);

						final SymmetricCiphertext ctCC_id_k = symmetric.genCiphertextSymmetric(skcc_id_k, Conversions.stringToByteArray(CC_id.get(k)),
								ImmutableList.emptyList());

						final String lCC_id_k_HB64 = base64.base64Encode(hash.recursiveHash(lCC_id_k));
						final String ctCC_id_k_B64 = base64.base64Encode(ImmutableByteArray.concat(ctCC_id_k.ciphertext(), ctCC_id_k.nonce()));
						CMtable.put(lCC_id_k_HB64, ctCC_id_k_B64);
					}

					final GqElement pVCC_id = pVCC.get(id);

					final ImmutableByteArray lVCC_id = hash.recursiveHash(pVCC_id, HashableString.from(vc_id), HashableString.from(ee));

					final String VCC_id = random.genUniqueDecimalStrings(L_VCC, 1).get(0);

					final ImmutableByteArray skvcc_id = keyDerivation.KDF(lVCC_id, ImmutableList.emptyList(), KEY_DERIVATION_BYTES_LENGTH);

					final SymmetricCiphertext ctVCC_id = symmetric.genCiphertextSymmetric(skvcc_id, Conversions.stringToByteArray(VCC_id),
							ImmutableList.emptyList());

					final String lVCC_id_HB64 = base64.base64Encode(hash.recursiveHash(lVCC_id));
					final String ctVCC_id_B64 = base64.base64Encode(ImmutableByteArray.concat(ctVCC_id.ciphertext(), ctVCC_id.nonce()));
					CMtable.put(lVCC_id_HB64, ctVCC_id_B64);

					return new ReturnCodes(CC_id, VCC_id);
				})
				.collect(toImmutableList());

		final ImmutableList<ImmutableList<String>> CC = returnCodes.stream().map(ReturnCodes::CC_id).collect(toImmutableList());
		final ImmutableList<String> VCC = returnCodes.stream().map(ReturnCodes::VCC).collect(toImmutableList());

		// Order(CMtable, 1). The TreeMap reorders the entries by their key to ensure that the original order of insertion is completely lost.
		final ImmutableMap<String, String> ordered_CMtable = ImmutableMap.from(CMtable, TreeMap::new);

		return new GenCMTableOutput(ordered_CMtable, CC, VCC);
	}

}
