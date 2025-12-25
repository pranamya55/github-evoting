/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;

@Service
public class MixDecOnlineService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecOnlineService.class);

	private final MixDecOnlineAlgorithm mixDecOnlineAlgorithm;
	private final CcmjElectionKeysService ccmjElectionKeysService;

	@Value("${nodeID}")
	private int nodeId;

	public MixDecOnlineService(final MixDecOnlineAlgorithm mixDecOnlineAlgorithm,
			final CcmjElectionKeysService ccmjElectionKeysService) {
		this.mixDecOnlineAlgorithm = mixDecOnlineAlgorithm;
		this.ccmjElectionKeysService = ccmjElectionKeysService;
	}

	/**
	 * Invokes the MixDecOnline algorithm.
	 *
	 * @param controlComponentVotesHashPayloads the control component votes hash payloads. Must be non-null.
	 * @param encryptionGroup                   the encryption group. Must be non-null.
	 * @param numberOfWriteInsPlusOne           the number of write-ins plus one. Must be in range [1, delta<sub>sup</sub>].
	 * @param ccmElectionPublicKeys             the CCM election public keys. Must be non-null.
	 * @param electoralBoardPublicKey           the electoral board public key. Must be non-null.
	 * @param encryptedConfirmedVotesHash       the hash of the encrypted, confirmed votes done in GetMixnetInitialCiphertexts algorithm. Must be
	 *                                          non-null.
	 * @param partiallyDecryptedVotes           the partially decrypted votes. If nodeId=1, then it's the mix net initial ciphertexts. Otherwise, it
	 *                                          is the partially decrypted votes from the previous node. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the control component votes hash payloads are not valid.
	 */
	public MixDecOnlineOutput mixDecOnline(final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads,
			final GqGroup encryptionGroup,
			final int numberOfWriteInsPlusOne,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys,
			final ElGamalMultiRecipientPublicKey electoralBoardPublicKey,
			final String encryptedConfirmedVotesHash,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes) {
		checkNotNull(controlComponentVotesHashPayloads);
		checkNotNull(encryptionGroup);
		checkNotNull(ccmElectionPublicKeys);
		checkNotNull(electoralBoardPublicKey);
		checkNotNull(encryptedConfirmedVotesHash);
		checkNotNull(partiallyDecryptedVotes);

		final String electionEventId = controlComponentVotesHashPayloads.get(0).getElectionEventId();
		final String ballotBoxId = controlComponentVotesHashPayloads.get(0).getBallotBoxId();

		final ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey = ccmjElectionKeysService.getCcmjElectionKeyPair(electionEventId).getPrivateKey();

		final ImmutableList<String> encryptedConfirmedVotesHashes = controlComponentVotesHashPayloads.stream()
				.map(ControlComponentVotesHashPayload::getEncryptedConfirmedVotesHash)
				.collect(toImmutableList());

		final MixDecOnlineContext mixDecOnlineContext = new MixDecOnlineContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setNodeId(nodeId)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfWriteInsPlusOne)
				.setCcmElectionPublicKeys(ccmElectionPublicKeys)
				.setElectoralBoardPublicKey(electoralBoardPublicKey)
				.build();

		final MixDecOnlineInput mixDecOnlineInput = new MixDecOnlineInput.Builder()
				.setPartiallyDecryptedVotes(partiallyDecryptedVotes)
				.setCcmjElectionSecretKey(ccmjElectionSecretKey)
				.setEncryptedConfirmedVotesHash(encryptedConfirmedVotesHash)
				.setEncryptedConfirmedVotesHashes(encryptedConfirmedVotesHashes)
				.build();

		LOGGER.debug("Performing MixDecOnline algorithm... [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		return mixDecOnlineAlgorithm.mixDecOnline(mixDecOnlineContext, mixDecOnlineInput);
	}
}
