/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.evotinglibraries.domain.election.PartialPrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Implements the GenSetupData algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenSetupDataAlgorithm {

	private final Random random;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final GetElectionEventEncryptionParametersAlgorithm getElectionEventEncryptionParametersAlgorithm;

	public GenSetupDataAlgorithm(final Random random,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final GetElectionEventEncryptionParametersAlgorithm getElectionEventEncryptionParametersAlgorithm) {
		this.random = random;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.getElectionEventEncryptionParametersAlgorithm = getElectionEventEncryptionParametersAlgorithm;
	}

	/**
	 * Generates the encryption parameters, the small primes, the primes mapping table for each verification card set and a key pair to encrypt the
	 * partial Choice Return Codes pCC<sub>id</sub> during the configuration phase.
	 *
	 * @param context                  the context as a {@link GenSetupDataContext}. Must be non-null.
	 * @param encryptionParametersSeed seed, the encryption parameter's seed. Must be non-null and a valid seed.
	 * @return the generated setup data as a {@link GenSetupDataOutput}.
	 */
	@SuppressWarnings("java:S117")
	public GenSetupDataOutput genSetupData(final GenSetupDataContext context, final String encryptionParametersSeed) {
		checkNotNull(context);

		// Context.
		final ImmutableList<String> vcs_vector = context.getVerificationsCardSetIds();
		// vcs_to_v_tilde_sigma_tau_vector contains the actual voting options, the semantic information and the correctness information for each vcs.
		final ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> vcs_to_v_tilde_sigma_tau_vector = context.getpartialPTableEntriesPerVerificationCardSetId();
		final int n_sup = context.getMaximumSupportedNumberOfVotingOptions();
		final int psi_sup = context.getMaximumSupportedNumberOfSelections();
		final int delta_sup = context.getMaximumSupportedNumberOfWriteInsPlusOne();

		// Input.
		final String seed = validateSeed(encryptionParametersSeed);

		// Require.
		vcs_to_v_tilde_sigma_tau_vector.values().stream()
				.parallel()
				.map(ImmutableList::size)
				.forEach(n -> checkState(1 <= n && n <= n_sup,
						"The size of the actual voting options, semantic information and correctness information must be in range [1, %s]. [n: %s]",
						n_sup, n));

		// Operation.
		final GetElectionEventEncryptionParametersOutput p_q_g_p_vector = getElectionEventEncryptionParametersAlgorithm.getElectionEventEncryptionParameters(
				seed);
		final GqGroup p_q_g = p_q_g_p_vector.encryptionGroup();
		final GroupVector<PrimeGqElement, GqGroup> p_vector = p_q_g_p_vector.smallPrimes();

		final Map<String, PrimeGqElement> p_map = new HashMap<>();

		int k = 0;
		int n_max = 0;
		int psi_max = 0;
		int delta_max = 0;

		final Map<String, PrimesMappingTable> pTable = new HashMap<>();
		for (final String vcs : vcs_vector) {

			final List<PrimesMappingTableEntry> v_p_tilde_sigma_tau = new ArrayList<>();
			// for i in [0, n)
			for (final PartialPrimesMappingTableEntry v_tilde_sigma_tau_i : vcs_to_v_tilde_sigma_tau_vector.get(vcs)) {

				final PrimeGqElement p_i_tilde;
				final String v_i = v_tilde_sigma_tau_i.actualVotingOption();
				if (p_map.containsKey(v_i)) {

					p_i_tilde = p_map.get(v_i);
				} else {

					checkState(k < n_sup,
							"The amount of distinct voting options across all verification card set must not exceed the maximum supported number of voting options. [n_sup: %s]",
							n_sup);

					p_i_tilde = p_vector.get(k);

					p_map.put(v_i, p_i_tilde);

					k++;
				}

				final String sigma_i = v_tilde_sigma_tau_i.semanticInformation();
				final String tau_i = v_tilde_sigma_tau_i.correctnessInformation();
				v_p_tilde_sigma_tau.add(new PrimesMappingTableEntry(v_i, p_i_tilde, sigma_i, tau_i));
			}

			final PrimesMappingTable pTable_vcs = new PrimesMappingTable(GroupVector.from(v_p_tilde_sigma_tau));

			final int psi = primesMappingTableAlgorithms.getPsi(pTable_vcs);
			final int delta = primesMappingTableAlgorithms.getDelta(pTable_vcs);
			checkState(delta - 1 <= psi,
					"The number of write-ins of a verification card set must not exceed the number of selections. [delta: %s, psi: %s]", delta, psi);

			n_max = Math.max(n_max, pTable_vcs.getNumberOfVotingOptions());
			psi_max = Math.max(psi_max, psi);
			delta_max = Math.max(delta_max, delta);

			pTable.put(vcs, pTable_vcs);
		}
		checkState(psi_max <= psi_sup && delta_max <= delta_sup,
				"The maximum amount of selections or write-ins must not exceed the supported values. [psi_max: %s, psi_sup: %s, delta_max: %s, delta_sup: %s]",
				psi_max, psi_sup, delta_max, delta_sup);

		final ElGamalMultiRecipientKeyPair pk_setup_sk_setup = ElGamalMultiRecipientKeyPair.genKeyPair(p_q_g, n_max, random);

		return new GenSetupDataOutput.Builder()
				.setEncryptionGroup(p_q_g)
				.setSmallPrimes(p_vector)
				.setMaximumNumberOfVotingOptions(n_max)
				.setMaximumNumberOfSelections(psi_max)
				.setMaximumNumberOfWriteInsPlusOne(delta_max)
				.setPrimesMappingTables(ImmutableMap.from(pTable))
				.setSetupKeyPair(pk_setup_sk_setup)
				.build();
	}

}
