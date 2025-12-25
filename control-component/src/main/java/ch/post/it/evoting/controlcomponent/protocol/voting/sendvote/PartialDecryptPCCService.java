/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@Service
public class PartialDecryptPCCService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartialDecryptPCCService.class);

	private final PartialDecryptPCCAlgorithm partialDecryptPCCAlgorithm;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	@Value("${nodeID}")
	private int nodeId;

	public PartialDecryptPCCService(
			final PartialDecryptPCCAlgorithm partialDecryptPCCAlgorithm,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.partialDecryptPCCAlgorithm = partialDecryptPCCAlgorithm;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Invokes the PartialDecryptPCC algorithm.
	 *
	 * @param encryptionGroup         the encryption group. Must be non-null.
	 * @param electionEventId         the election event id. Must be non-null and a valid UUID.
	 * @param primesMappingTable      the primes mapping table. Must be non-null.
	 * @param encryptedVerifiableVote the encrypted vote. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the inputs have different encryption groups.
	 */
	public PartialDecryptPCCOutput partialDecryptPCC(final GqGroup encryptionGroup, final String electionEventId,
			final PrimesMappingTable primesMappingTable, final EncryptedVerifiableVote encryptedVerifiableVote) {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		checkNotNull(encryptedVerifiableVote);
		checkArgument(allEqual(
				Stream.of(encryptionGroup, primesMappingTable.getEncryptionGroup(), encryptedVerifiableVote.encryptedVote().getGroup()),
				Function.identity()));

		final ContextIds contextIds = encryptedVerifiableVote.contextIds();
		checkArgument(electionEventId.equals(contextIds.electionEventId()),
				"The encrypted verifiable vote's election event id does not match the provided one.");

		final String verificationCardId = contextIds.verificationCardId();

		final int numberOfSelections = primesMappingTableAlgorithms.getPsi(primesMappingTable);
		final int numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = ccrjReturnCodesKeysService.getCcrjChoiceReturnCodesEncryptionKeyPair(
				electionEventId);

		// Perform partial decryption of the encrypted partial Choice Return codes.
		final DecryptPCCContext decryptPCCContext = new DecryptPCCContext.Builder()
				.setNodeId(nodeId)
				.setVerificationCardId(verificationCardId)
				.setNumberOfSelections(numberOfSelections)
				.setNumberOfWriteInsPlusOne(numberOfWriteInsPlusOne)
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.build();

		final PartialDecryptPCCInput partialDecryptPCCInput = new PartialDecryptPCCInput.Builder()
				.setEncryptedVote(encryptedVerifiableVote.encryptedVote())
				.setExponentiatedEncryptedVote(encryptedVerifiableVote.exponentiatedEncryptedVote())
				.setEncryptedPartialChoiceReturnCodes(encryptedVerifiableVote.encryptedPartialChoiceReturnCodes())
				.setCcrjChoiceReturnCodesEncryptionPublicKey(ccrjChoiceReturnCodesEncryptionKeyPair.getPublicKey())
				.setCcrjChoiceReturnCodesEncryptionSecretKey(ccrjChoiceReturnCodesEncryptionKeyPair.getPrivateKey())
				.build();

		LOGGER.debug("Performing Partial Decrypt PCC algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return partialDecryptPCCAlgorithm.partialDecryptPCC(decryptPCCContext, partialDecryptPCCInput);
	}
}
