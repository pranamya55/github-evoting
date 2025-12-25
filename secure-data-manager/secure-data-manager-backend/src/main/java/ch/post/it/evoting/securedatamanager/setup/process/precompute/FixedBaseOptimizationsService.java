/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.min;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.BigIntegersOptimizations;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.securedatamanager.setup.process.SetupKeyPairService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;

/**
 * Service that deals with the pre-computation of verification card sets.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class FixedBaseOptimizationsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FixedBaseOptimizationsService.class);

	private final SetupKeyPairService setupKeyPairService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	@Value("${sdm.process.precompute.fixed-based-optimisation.max-cache-size}")
	private int maxCacheSize;

	@Autowired
	public FixedBaseOptimizationsService(
			final SetupKeyPairService setupKeyPairService,
			final ElectionEventContextPayloadService electionEventContextPayloadService) {
		this.setupKeyPairService = setupKeyPairService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * We limit the number of elements saved in the cache.
	 * <p>
	 * Let k=min(numberOfSmallPrimes, cacheSize / 2) and l=min(setupPublicKeyLength, k). We save the first k small primes and the first l setup public
	 * key's elements. The cache will have a total of k+l elements.
	 * </p>
	 */
	public void prepareFixedBaseOptimizations(final String electionEventId) {
		validateUUID(electionEventId);

		// Get the encryption parameters' encryption group and small primes.
		final GqGroup encryptionGroup = electionEventContextPayloadService.loadEncryptionGroup(electionEventId);
		final GroupVector<PrimeGqElement, GqGroup> smallPrimes = electionEventContextPayloadService.loadSmallPrimes(electionEventId);

		// Get the setup public key
		final ElGamalMultiRecipientPublicKey setupPublicKey = setupKeyPairService.load(electionEventId).getPublicKey();
		checkState(encryptionGroup.equals(setupPublicKey.getGroup()), "The setup public key's group must be equal to the encryption group.");

		// Add to the GMP cache the fixed-base exponentiation.
		final int numberOfSmallPrimesSaved = min(smallPrimes.size(), maxCacheSize / 2);
		final int numberOfSetupPublicKeyElementsSaved = min(setupPublicKey.size(), numberOfSmallPrimesSaved);

		LOGGER.debug(
				"Preparing for fixed-base optimizations if supported. [cacheSize: {}, numberOfSmallPrimesSaved: {}, numberOfSetupPublicKeyElementsSaved: {}]",
				maxCacheSize, numberOfSmallPrimesSaved, numberOfSetupPublicKeyElementsSaved);

		IntStream.range(0, numberOfSmallPrimesSaved)
				.parallel()
				.forEach(i -> BigIntegersOptimizations.prepareFixedBaseOptimizations(smallPrimes.get(i).getValue(), encryptionGroup.getP()));
		IntStream.range(0, numberOfSetupPublicKeyElementsSaved)
				.parallel()
				.forEach(i -> BigIntegersOptimizations.prepareFixedBaseOptimizations(setupPublicKey.get(i).getValue(), encryptionGroup.getP()));

		LOGGER.info(
				"Prepared fixed-base optimizations if supported. [cacheSize: {}, numberOfSmallPrimesSaved: {}, numberOfSetupPublicKeyElementsSaved: {}]",
				maxCacheSize, numberOfSmallPrimesSaved, numberOfSetupPublicKeyElementsSaved);

	}
}

