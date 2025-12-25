/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TOO_SMALL_CHUNK_SIZE_MESSAGE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.securedatamanager.setup.process.precompute.PrecomputeContext;

/**
 * Service that invokes the GenVerDat algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenVerDatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenVerDatService.class);

	private final GenVerDatAlgorithm genVerDatAlgorithm;

	public GenVerDatService(final GenVerDatAlgorithm genVerDatAlgorithm) {
		this.genVerDatAlgorithm = genVerDatAlgorithm;
	}

	/**
	 * Invokes the GenVerDat algorithm.
	 *
	 * @param precomputeContext           the context identifiers. Must be non-null.
	 * @param numberOfEligibleVoters      the number of eligible voters. Must be positive.
	 * @param chunkSize                   the size of the chunks. Must be positive.
	 * @param electionEventContextPayload the election event context payload. Must be non-null.
	 * @param setupPublicKey              the setup public key. Must be non-null.
	 * @param primesMappingTable          the primes mapping table. Must be non-null.
	 */
	public ImmutableList<GenVerDatOutput> genVerDat(final PrecomputeContext precomputeContext, final int numberOfEligibleVoters,
			final int chunkSize, final ElectionEventContextPayload electionEventContextPayload, final ElGamalMultiRecipientPublicKey setupPublicKey,
			final PrimesMappingTable primesMappingTable) {
		checkNotNull(precomputeContext);
		checkArgument(numberOfEligibleVoters >= 0, "The number of eligible voters must be positive.");
		checkArgument(chunkSize > 0, TOO_SMALL_CHUNK_SIZE_MESSAGE);
		checkNotNull(electionEventContextPayload);
		checkNotNull(setupPublicKey);
		checkNotNull(primesMappingTable);

		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final int maximumNumberOfVotingOptions = electionEventContextPayload.getElectionEventContext().maximumNumberOfVotingOptions();

		checkArgument(encryptionGroup.equals(setupPublicKey.getGroup()), "The group of the setup public key must be equal to the encryption group.");
		checkArgument(encryptionGroup.equals(primesMappingTable.getEncryptionGroup()),
				"The group of the primes mapping table must be equal to the encryption group.");

		checkArgument(electionEventId.equals(electionEventContextPayload.getElectionEventContext().electionEventId()),
				"The election event identifier must be equal to the election event identifier in the election event context payload.");

		// Build full-sized chunks (i.e. with `chunkSize` elements)
		final int fullChunkCount = numberOfEligibleVoters / chunkSize;
		ImmutableList<GenVerDatOutput> genVerDatOutputs = IntStream.range(0, fullChunkCount).parallel()
				.mapToObj(chunkId -> {

					LOGGER.debug("Generating verification data... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]", electionEventId,
							verificationCardSetId, chunkId);

					// Generate verification data. Since we chunk the payloads, we are not directly working with the number of eligible voters (N_E),
					// but rather with the chunk size as context of the algorithm.
					final GenVerDatContext genVerDatContext = new GenVerDatContext(encryptionGroup, electionEventId, chunkSize, primesMappingTable,
							maximumNumberOfVotingOptions);
					final GenVerDatOutput genVerDatOutput = genVerDatAlgorithm.genVerDat(genVerDatContext, setupPublicKey);

					LOGGER.info("Successfully generated the verification data. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					return genVerDatOutput;
				})
				.collect(toImmutableList());

		// Build an eventual last chunk with the remaining elements.
		final int lastChunkSize = numberOfEligibleVoters % chunkSize;
		if (lastChunkSize > 0) {
			LOGGER.debug("Generating verification data... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]", electionEventId,
					verificationCardSetId, fullChunkCount);
			final GenVerDatContext genVerDatContext = new GenVerDatContext(encryptionGroup, electionEventId, lastChunkSize, primesMappingTable,
					maximumNumberOfVotingOptions);
			genVerDatOutputs = genVerDatOutputs.append(genVerDatAlgorithm.genVerDat(genVerDatContext, setupPublicKey));
			LOGGER.info("Successfully generated the verification data. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
					electionEventId, verificationCardSetId, fullChunkCount);
		}

		LOGGER.info("Successfully executed the GenVerDat algorithm. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		return genVerDatOutputs;
	}

}

