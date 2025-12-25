/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@Service
public class DecryptPCCService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DecryptPCCService.class);

	private final DecryptPCCAlgorithm decryptPCCAlgorithm;
	private final EncryptedVerifiableVoteService encryptedVerifiableVoteService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	@Value("${nodeID}")
	private int nodeId;

	public DecryptPCCService(final DecryptPCCAlgorithm decryptPCCAlgorithm,
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.decryptPCCAlgorithm = decryptPCCAlgorithm;
		this.encryptedVerifiableVoteService = encryptedVerifiableVoteService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Invokes the DecryptPCC algorithm.
	 *
	 * @param encryptionGroup                        the encryption group. Must be non-null.
	 * @param primesMappingTable                     the primes mapping table. Must be non-null.
	 * @param controlComponentPartialDecryptPayloads the control component partial decrypt payloads. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the group of the primes mapping table is not equal to the encryption group.
	 */
	public GroupVector<GqElement, GqGroup> decryptPCC(final GqGroup encryptionGroup, final PrimesMappingTable primesMappingTable,
			final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) {
		checkNotNull(encryptionGroup);
		checkArgument(primesMappingTable.getEncryptionGroup().equals(encryptionGroup),
				"The group of the primes mapping table must be equal to the encryption group.");
		validate(controlComponentPartialDecryptPayloads);

		final Map<Boolean, ImmutableList<ControlComponentPartialDecryptPayload>> controlComponentPartialDecryptPayloadsMap = controlComponentPartialDecryptPayloads.stream()
				.collect(Collectors.partitioningBy(payload -> payload.getPartiallyDecryptedEncryptedPCC().nodeId() == nodeId, toImmutableList()));

		final ContextIds contextIds = controlComponentPartialDecryptPayloadsMap.get(true)
				.get(0)
				.getPartiallyDecryptedEncryptedPCC()
				.contextIds();

		final DecryptPCCContext decryptPCCContext = buildDecryptPCCContext(encryptionGroup, primesMappingTable, contextIds);

		final DecryptPCCInput decryptPCCInput = buildDecryptPCCInput(controlComponentPartialDecryptPayloadsMap, contextIds);

		LOGGER.debug("Performing DecryptPCC algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return decryptPCCAlgorithm.decryptPCC(decryptPCCContext, decryptPCCInput);
	}

	private DecryptPCCContext buildDecryptPCCContext(final GqGroup encryptionGroup, final PrimesMappingTable primesMappingTable,
			final ContextIds contextIds) {
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();

		final int numberOfSelections = primesMappingTableAlgorithms.getPsi(primesMappingTable);
		final int numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final ImmutableList<ControlComponentPublicKeys> combinedControlComponentPublicKeys = setupComponentPublicKeysService.getCombinedControlComponentPublicKeys(
				electionEventId);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcrjEncryptionPublicKeys = combinedControlComponentPublicKeys.stream()
				.filter(ccpk -> ccpk.nodeId() != nodeId)
				.map(ControlComponentPublicKeys::ccrjChoiceReturnCodesEncryptionPublicKey)
				.collect(GroupVector.toGroupVector());

		return new DecryptPCCContext.Builder()
				.setNodeId(nodeId)
				.setVerificationCardId(verificationCardId)
				.setNumberOfSelections(numberOfSelections)
				.setNumberOfWriteInsPlusOne(numberOfWriteInsPlusOne)
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setOtherCcrChoiceReturnCodesEncryptionKeys(otherCcrjEncryptionPublicKeys)
				.build();
	}

	private DecryptPCCInput buildDecryptPCCInput(
			final Map<Boolean, ImmutableList<ControlComponentPartialDecryptPayload>> controlComponentPartialDecryptPayloadsMap,
			final ContextIds contextIds) {
		final GroupVector<GqElement, GqGroup> exponentiatedGammas = controlComponentPartialDecryptPayloadsMap.get(true).get(0)
				.getPartiallyDecryptedEncryptedPCC()
				.exponentiatedGammas();

		final ImmutableList<PartiallyDecryptedEncryptedPCC> otherPartiallyDecryptedEncryptedPCC = controlComponentPartialDecryptPayloadsMap.get(false)
				.stream()
				.map(ControlComponentPartialDecryptPayload::getPartiallyDecryptedEncryptedPCC)
				.sorted(Comparator.comparingInt(PartiallyDecryptedEncryptedPCC::nodeId))
				.collect(toImmutableList());

		final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammas = otherPartiallyDecryptedEncryptedPCC.stream()
				.map(PartiallyDecryptedEncryptedPCC::exponentiatedGammas)
				.collect(toGroupVector());

		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs = otherPartiallyDecryptedEncryptedPCC.stream()
				.map(PartiallyDecryptedEncryptedPCC::exponentiationProofs)
				.collect(toGroupVector());

		final String verificationCardId = contextIds.verificationCardId();
		final EncryptedVerifiableVote encryptedVerifiableVote = encryptedVerifiableVoteService.getEncryptedVerifiableVote(verificationCardId);

		return new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammas)
				.setOtherCcrExponentiatedGammaElements(otherCcrExponentiatedGammas)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVerifiableVote.encryptedVote())
				.setExponentiatedEncryptedVote(encryptedVerifiableVote.exponentiatedEncryptedVote())
				.setEncryptedPartialChoiceReturnCodes(encryptedVerifiableVote.encryptedPartialChoiceReturnCodes())
				.build();
	}
}
