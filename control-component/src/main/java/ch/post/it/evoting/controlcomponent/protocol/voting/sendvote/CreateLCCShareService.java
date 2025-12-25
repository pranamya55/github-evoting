/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.PartialChoiceReturnCodeAllowList;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@Service
public class CreateLCCShareService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateLCCShareService.class);

	private final CreateLCCShareAlgorithm createLCCShareAlgorithm;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	private final VerificationCardSetService verificationCardSetService;

	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	@Value("${nodeID}")
	private int nodeId;

	public CreateLCCShareService(final CreateLCCShareAlgorithm createLCCShareAlgorithm,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService,
			final VerificationCardSetService verificationCardSetService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.createLCCShareAlgorithm = createLCCShareAlgorithm;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
		this.verificationCardSetService = verificationCardSetService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Invokes the CreateLCCShare algorithm.
	 *
	 * @param encryptionGroup                   the encryption group. Must be non-null.
	 * @param contextIds                        the context ids. Must be non-null.
	 * @param primesMappingTable                the primes mapping table. Must be non-null.
	 * @param decryptedPartialChoiceReturnCodes the output of the algorithm DecryptPCC. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the inputs have different encryption groups.
	 */
	public CreateLCCShareOutput createLCCShare(final GqGroup encryptionGroup, final ContextIds contextIds,
			final PrimesMappingTable primesMappingTable, final GroupVector<GqElement, GqGroup> decryptedPartialChoiceReturnCodes) {
		checkNotNull(encryptionGroup);
		checkNotNull(contextIds);
		checkNotNull(decryptedPartialChoiceReturnCodes);
		checkArgument(primesMappingTable.getEncryptionGroup().equals(encryptionGroup),
				"The group of the primes mapping table must be equal to the encryption group.");
		checkArgument(decryptedPartialChoiceReturnCodes.getGroup().equals(encryptionGroup),
				"The group of the decrypted partial choice return codes must be equal to the encryption group.");

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final PartialChoiceReturnCodeAllowList allowList = verificationCardSetService.getPartialChoiceReturnCodesAllowList(verificationCardSetId);
		final ImmutableList<String> blankCorrectnessInformation = primesMappingTableAlgorithms.getBlankCorrectnessInformation(primesMappingTable);

		final ZqElement ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesKeysService.getCcrjReturnCodesGenerationSecretKey(electionEventId);

		final CreateLCCShareContext createLCCShareContext = new CreateLCCShareContext(encryptionGroup, nodeId, electionEventId, verificationCardSetId,
				verificationCardId, blankCorrectnessInformation);

		final CreateLCCShareInput createLCCShareInput = new CreateLCCShareInput(allowList, decryptedPartialChoiceReturnCodes,
				ccrjReturnCodesGenerationSecretKey);

		LOGGER.debug("Performing CreateLCCShare algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return createLCCShareAlgorithm.createLCCShare(createLCCShareContext, createLCCShareInput);
	}
}
