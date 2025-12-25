/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToInteger;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BCK_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SVK_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.UsabilityBase32Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Implements the GenVerDat algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenVerDatAlgorithm {

	private final ElGamal elGamal;
	private final Hash hash;
	private final Random random;
	private final Base64 base64;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	public GenVerDatAlgorithm(
			final ElGamal elGamal,
			final Hash hash,
			final Random random,
			final Base64 base64,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.elGamal = elGamal;
		this.hash = hash;
		this.random = random;
		this.base64 = base64;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Initialize the control components' computation of the return codes.
	 *
	 * @param context        the {@link GenVerDatContext}. Must be non-null.
	 * @param setupPublicKey pk<sub>setup</sub>, the setup public key. Must be non-null.
	 * @return the generated verification data as a {@link GenVerDatOutput}.
	 * @throws NullPointerException     if any non-nullable input is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>{@code eligibleVoters} is not strictly greater than 0.</li>
	 *                                      <li>The number of voting options is greater than the secret key length.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public GenVerDatOutput genVerDat(final GenVerDatContext context, final ElGamalMultiRecipientPublicKey setupPublicKey) {
		checkNotNull(context);
		checkNotNull(setupPublicKey);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(setupPublicKey.getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final int N_E = context.numberOfEligibleVoters();
		final PrimesMappingTable pTable = context.primesMappingTable();
		final int n_max = context.maximumNumberOfVotingOptions();
		final int n = pTable.getNumberOfVotingOptions();

		// Input.
		final ElGamalMultiRecipientPublicKey pk_setup = setupPublicKey;

		// Cross-check.
		checkArgument(pk_setup.size() == n_max, "The setup public key length must be equal to the maximum number of voting options. [n_max: %s]",
				n_max);

		final Alphabet A_base16 = Base16Alphabet.getInstance();
		final Alphabet A_u32 = UsabilityBase32Alphabet.getInstance();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(p_q_g);
		final int l_BCK = BCK_LENGTH;
		final int l_ID = ID_LENGTH;
		final int l_SVK = SVK_LENGTH;

		// Output variables.
		record VerificationData(String vc_id,
								String SVK_id,
								ElGamalMultiRecipientKeyPair keyPair_id,
								List<String> L_pCC_id,
								String BCK_id,
								ElGamalMultiRecipientCiphertext c_pCC_id,
								ElGamalMultiRecipientCiphertext c_ck_id) {
		}

		// Operation.
		final GroupVector<PrimeGqElement, GqGroup> p_tilde = primesMappingTableAlgorithms.getEncodedVotingOptions(pTable, ImmutableList.emptyList());
		final ImmutableList<String> tau = primesMappingTableAlgorithms.getCorrectnessInformation(pTable, ImmutableList.emptyList());

		final ImmutableList<VerificationData> verificationData = IntStream.range(0, N_E).parallel()
				.mapToObj(id -> {
					final String vc_id = random.genRandomString(l_ID, A_base16);
					final String SVK_id = random.genRandomString(l_SVK, A_u32);
					final ElGamalMultiRecipientKeyPair K_id_k_id = ElGamalMultiRecipientKeyPair.genKeyPair(p_q_g, 1, random);

					// Compute hpCC_id.
					final List<GqElement> hpCC_id_elements = new ArrayList<>();
					final ZqElement k_id = K_id_k_id.getPrivateKey().get(0);
					final List<String> L_pCC_id = new ArrayList<>();
					for (int k = 0; k < n; k++) {
						final PrimeGqElement p_k_tilde = p_tilde.get(k);
						final GqElement pCC_id_k = p_k_tilde.exponentiate(k_id);

						final GqElement hpCC_id_k = hash.hashAndSquare(pCC_id_k.getValue(), p_q_g);

						final String ci = tau.get(k);
						final ImmutableByteArray lpCC_id_k = hash.recursiveHash(hpCC_id_k, HashableString.from(vc_id), HashableString.from(ee),
								HashableString.from(ci));

						L_pCC_id.add(base64.base64Encode(lpCC_id_k));
						hpCC_id_elements.add(hpCC_id_k);
					}
					final ElGamalMultiRecipientMessage hpCC_id = new ElGamalMultiRecipientMessage(GroupVector.from(hpCC_id_elements));

					// Compute c_pCC_id.
					final ZqElement hpCC_id_exponent = ZqElement.create(random.genRandomInteger(p_q_g.getQ()), zqGroup);
					final ElGamalMultiRecipientCiphertext c_pCC_id = elGamal.getCiphertext(hpCC_id, hpCC_id_exponent, pk_setup);

					// Generate BCK_id.
					String BCK_id;

					do {
						BCK_id = random.genUniqueDecimalStrings(l_BCK, 1).get(0);
					} while (stringToInteger(BCK_id).equals(BigInteger.ZERO));

					// Compute c_ck_id.
					final GqElement hBCK_id = hash.hashAndSquare(stringToInteger(BCK_id), p_q_g);
					final GqElement CK_id = hBCK_id.exponentiate(k_id);

					final ElGamalMultiRecipientMessage hCK_id = new ElGamalMultiRecipientMessage(
							GroupVector.of(hash.hashAndSquare(CK_id.getValue(), p_q_g)));

					final ZqElement hCKExponent = ZqElement.create(random.genRandomInteger(p_q_g.getQ()), zqGroup);
					final ElGamalMultiRecipientCiphertext c_ck_id = elGamal.getCiphertext(hCK_id, hCKExponent, pk_setup);

					return new VerificationData(vc_id, SVK_id, K_id_k_id, L_pCC_id, BCK_id, c_pCC_id, c_ck_id);
				})
				.collect(toImmutableList());

		final ImmutableList<String> L_pCC = verificationData.stream()
				.flatMap(v -> v.L_pCC_id.stream())
				.sorted() // Lexicographic ordering of the list. Corresponds to the operation LpCC <- Order(LpCC).
				.collect(toImmutableList());

		// Outputs.
		final ImmutableList<String> vc = verificationData.stream()
				.map(VerificationData::vc_id)
				.collect(toImmutableList());
		final ImmutableList<String> SVK = verificationData.stream()
				.map(VerificationData::SVK_id)
				.collect(toImmutableList());
		final ImmutableList<String> BCK = verificationData.stream()
				.map(VerificationData::BCK_id)
				.collect(toImmutableList());
		final ImmutableList<ElGamalMultiRecipientCiphertext> c_pCC = verificationData.stream()
				.map(VerificationData::c_pCC_id)
				.collect(toImmutableList());
		final ImmutableList<ElGamalMultiRecipientCiphertext> c_ck = verificationData.stream()
				.map(VerificationData::c_ck_id)
				.collect(toImmutableList());

		// The object regroups the public and secret keys of the verification card key pair
		final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs = verificationData.stream()
				.map(VerificationData::keyPair_id)
				.collect(toImmutableList());

		return new GenVerDatOutput.Builder()
				.setVerificationCardIds(vc)
				.setStartVotingKeys(SVK)
				.setVerificationCardKeyPairs(verificationCardKeyPairs)
				.setPartialChoiceReturnCodesAllowList(L_pCC)
				.setBallotCastingKeys(BCK)
				.setEncryptedHashedPartialChoiceReturnCodes(GroupVector.from(c_pCC))
				.setEncryptedHashedConfirmationKeys(GroupVector.from(c_ck))
				.build();
	}

}

