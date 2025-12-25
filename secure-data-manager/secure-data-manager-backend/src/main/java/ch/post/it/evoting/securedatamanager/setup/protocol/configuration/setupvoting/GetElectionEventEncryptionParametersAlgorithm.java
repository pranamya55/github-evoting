/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;

/**
 * Implements the GetElectionEventEncryptionParameters algorithm
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GetElectionEventEncryptionParametersAlgorithm {

	private final ElGamal elGamal;

	public GetElectionEventEncryptionParametersAlgorithm(final ElGamal elGamal) {
		this.elGamal = elGamal;
	}

	/**
	 * Gets the election event's encryption parameters consisting of the encryption group and the small primes.
	 *
	 * @param seed the seed to be used for generating the encryption group. Must be non-null.
	 * @return the {@link GetElectionEventEncryptionParametersOutput} containing the encryption group and the small primes.
	 */
	@SuppressWarnings("java:S117")
	public GetElectionEventEncryptionParametersOutput getElectionEventEncryptionParameters(final String seed) {
		// Context.
		final int n_sup = MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
		final int psi_sup = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;

		// Input.
		validateSeed(seed);

		// Operation.
		final GqGroup p_q_g = elGamal.getEncryptionParameters(seed);
		final GroupVector<PrimeGqElement, GqGroup> p_vector = PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers(p_q_g, n_sup);

		final BigInteger prod_p_i = p_vector.stream()
				.skip((long) n_sup - psi_sup)
				.parallel()
				.reduce(p_vector.getGroup().getIdentity(), GqElement::multiply, GqElement::multiply)
				.getValue();

		final BigInteger p = p_q_g.getP();
		checkState(prod_p_i.compareTo(p) < 0,
				"The product of the psi_sup largest prime numbers cannot be equal to or larger than p. [prod_p_i: %s, p: %s]", prod_p_i, p);

		return new GetElectionEventEncryptionParametersOutput(p_q_g, p_vector);
	}

}
