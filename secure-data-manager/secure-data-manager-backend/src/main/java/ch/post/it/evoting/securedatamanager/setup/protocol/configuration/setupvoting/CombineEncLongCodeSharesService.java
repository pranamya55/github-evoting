/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedNodeLongReturnCodeSharesChunk;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedSingleNodeLongReturnCodeSharesChunk;

@Service
@ConditionalOnProperty("role.isSetup")
public class CombineEncLongCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CombineEncLongCodeSharesService.class);

	private final CombineEncLongCodeSharesAlgorithm combineEncLongCodeSharesAlgorithm;

	public CombineEncLongCodeSharesService(final CombineEncLongCodeSharesAlgorithm combineEncLongCodeSharesAlgorithm) {
		this.combineEncLongCodeSharesAlgorithm = combineEncLongCodeSharesAlgorithm;
	}

	/**
	 * Invokes the CombineEncLongCodeShares algorithm.
	 *
	 * @param electionEventContextPayload            the election event context payload. Must be non-null.
	 * @param primesMappingTable                     the primes mapping table. Must be non-null.
	 * @param verificationCardSetId                  the verification card set id. Must be non-null and a valid UUID.
	 * @param encryptedNodeLongReturnCodeSharesChunk the encrypted node long return code shares chunk. Must be non-null.
	 * @param setupSecretKey                         the setup secret key. Must be non-null.
	 */
	public CombineEncLongCodeSharesOutput combineEncLongCodeShares(final ElectionEventContextPayload electionEventContextPayload,
			final PrimesMappingTable primesMappingTable, final String verificationCardSetId,
			final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk,
			final ElGamalMultiRecipientPrivateKey setupSecretKey) {
		validate(electionEventContextPayload, primesMappingTable, verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey);

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final int maximumNumberOfVotingOptions = electionEventContext.maximumNumberOfVotingOptions();

		final CombineEncLongCodeSharesContext combineEncLongCodeSharesContext = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(encryptedNodeLongReturnCodeSharesChunk.getVerificationCardIds())
				.setNumberOfVotingOptions(primesMappingTable.getNumberOfVotingOptions())
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.build();
		final CombineEncLongCodeSharesInput combineEncLongCodeSharesInput = prepareCombineEncLongCodeSharesInput(
				encryptedNodeLongReturnCodeSharesChunk, setupSecretKey);

		final int chunkId = encryptedNodeLongReturnCodeSharesChunk.getChunkId();
		LOGGER.debug("Performing CombineEncLongCodeShares algorithm... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, chunkId);

		return combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(combineEncLongCodeSharesContext, combineEncLongCodeSharesInput);
	}

	private static void validate(final ElectionEventContextPayload electionEventContextPayload, final PrimesMappingTable primesMappingTable,
			final String verificationCardSetId, final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk,
			final ElGamalMultiRecipientPrivateKey setupSecretKey) {
		checkNotNull(electionEventContextPayload);
		checkNotNull(primesMappingTable);
		validateUUID(verificationCardSetId);
		checkNotNull(encryptedNodeLongReturnCodeSharesChunk);
		checkNotNull(setupSecretKey);

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();

		checkArgument(electionEventId.equals(encryptedNodeLongReturnCodeSharesChunk.getElectionEventId()),
				"The encrypted node long return code shares chunk does not correspond to the expected election event id. [expected: %s, actual: %s]",
				electionEventId, encryptedNodeLongReturnCodeSharesChunk.getElectionEventId());
		checkArgument(verificationCardSetId.equals(encryptedNodeLongReturnCodeSharesChunk.getVerificationCardSetId()),
				"The encrypted node long return code shares chunk does not correspond to the expected verification card set id. [expected: %s, actual: %s]",
				verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk.getVerificationCardSetId());
		checkArgument(electionEventContextPayload.getEncryptionGroup().equals(primesMappingTable.getEncryptionGroup()),
				"The encryption group of the election event context payload and the primes mapping table must be equal.");
		checkArgument(electionEventContextPayload.getEncryptionGroup().hasSameOrderAs(setupSecretKey.getGroup()),
				"The encryption group of the election event context payload and the setup secret key must be of same order.");

		checkArgument(electionEventContext.verificationCardSetContexts().stream()
						.parallel()
						.map(VerificationCardSetContext::getVerificationCardSetId)
						.anyMatch(vcs -> vcs.equals(verificationCardSetId)),
				"The given verification card set id does not exist. [verificationCardSetId: %s]", verificationCardSetId);
	}

	private static CombineEncLongCodeSharesInput prepareCombineEncLongCodeSharesInput(
			final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk,
			final ElGamalMultiRecipientPrivateKey setupSecretKey) {
		// Prepare the vectors of Voter Choice Return Code Generation Public Keys.
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks = encryptedNodeLongReturnCodeSharesChunk.getControlComponentCodeSharesChunks();
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getVoterChoiceReturnCodeGenerationPublicKeys)
				.collect(toGroupVector());

		// Prepare the vectors of Voter Vote Cast Return Code Generation Public Keys.
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getVoterVoteCastReturnCodeGenerationPublicKeys)
				.collect(toGroupVector());

		// Prepare the Matrix of exponentiated, encrypted, hashed partial Choice Return Codes.
		final GroupVector<GroupVector<ElGamalMultiRecipientCiphertext, GqGroup>, GqGroup> partialChoiceReturnCodesColumns = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getExponentiatedEncryptedHashedPartialChoiceReturnCodes)
				.map(GroupVector::from)
				.collect(toGroupVector());
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> partialChoiceReturnCodesMatrix = GroupMatrix.fromColumns(
				partialChoiceReturnCodesColumns);

		// Prepare the Matrix of exponentiated, encrypted, hashed Confirmation Keys.
		final GroupVector<GroupVector<ElGamalMultiRecipientCiphertext, GqGroup>, GqGroup> confirmationKeysColumns = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getExponentiatedEncryptedHashedConfirmationKey)
				.map(GroupVector::from)
				.collect(toGroupVector());
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> confirmationKeysMatrix = GroupMatrix.fromColumns(confirmationKeysColumns);

		// Prepare the exponentiation proofs.
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> partialChoiceReturnCodesExponentiationProofs = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getProofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.collect(toGroupVector());
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> confirmationKeyExponentiationProofs = controlComponentCodeSharesChunks.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getProofsOfCorrectConfirmationKeyExponentiation)
				.collect(toGroupVector());

		return new CombineEncLongCodeSharesInput.Builder()
				.setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedNodeLongReturnCodeSharesChunk.getEncryptedHashedPartialChoiceReturnCodes())
				.setEncryptedHashedConfirmationKeys(encryptedNodeLongReturnCodeSharesChunk.getEncryptedHashedConfirmationKeys())
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(partialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(partialChoiceReturnCodesExponentiationProofs)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(confirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(confirmationKeyExponentiationProofs)
				.build();
	}
}
