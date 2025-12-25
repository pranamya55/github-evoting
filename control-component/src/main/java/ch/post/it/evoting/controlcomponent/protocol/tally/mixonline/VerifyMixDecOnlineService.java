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

import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecInput;

@Service
public class VerifyMixDecOnlineService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyMixDecOnlineService.class);

	private final VerifyMixDecOnlineAlgorithm verifyMixDecOnlineAlgorithm;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;

	@Value("${nodeID}")
	private int nodeId;

	public VerifyMixDecOnlineService(final VerifyMixDecOnlineAlgorithm verifyMixDecOnlineAlgorithm,
			final SetupComponentPublicKeysService setupComponentPublicKeysService) {
		this.verifyMixDecOnlineAlgorithm = verifyMixDecOnlineAlgorithm;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
	}

	/**
	 * Invokes the VerifyMixDecOnline algorithm.
	 *
	 * @param controlComponentShufflePayloads the control component shuffle payloads. Must be non-null.
	 * @param encryptionGroup                 the encryption group. Must be non-null.
	 * @param numberOfWriteInsPlusOne         the number of write-ins plus one. Must be in range [1, delta<sub>sup</sub>].
	 * @param ccmElectionPublicKeys           the CCM election public keys. Must be non-null.
	 * @param electoralBoardPublicKey         the electoral board public key. Must be non-null.
	 * @param mixnetInitialCiphertexts        the mix net initial ciphertexts retrieved in GetMixnetInitialCiphertexts algorithm. Must be non-null.
	 * @throws NullPointerException if any parameter is null.
	 */
	public boolean verifyMixDecOnline(final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads,
			final GqGroup encryptionGroup,
			final int numberOfWriteInsPlusOne,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys,
			final ElGamalMultiRecipientPublicKey electoralBoardPublicKey,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> mixnetInitialCiphertexts) {
		checkNotNull(controlComponentShufflePayloads);
		checkNotNull(encryptionGroup);
		checkNotNull(ccmElectionPublicKeys);
		checkNotNull(electoralBoardPublicKey);
		checkNotNull(mixnetInitialCiphertexts);

		final String electionEventId = controlComponentShufflePayloads.get(0).getElectionEventId();
		final String ballotBoxId = controlComponentShufflePayloads.get(0).getBallotBoxId();

		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeysService.getElectionPublicKey(electionEventId);

		final ImmutableList<VerifiableShuffle> precedingVerifiableShuffledVotes = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getVerifiableShuffle)
				.collect(toImmutableList());
		final ImmutableList<VerifiableDecryptions> precedingVerifiableDecryptions = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getVerifiableDecryptions)
				.collect(toImmutableList());

		final VerifyMixDecOnlineContext verifyMixDecOnlineContext = new VerifyMixDecOnlineContext(encryptionGroup, nodeId, electionEventId,
				ballotBoxId, numberOfWriteInsPlusOne, electionPublicKey, ccmElectionPublicKeys, electoralBoardPublicKey);

		final VerifyMixDecInput verifyMixDecInput = new VerifyMixDecInput(mixnetInitialCiphertexts, precedingVerifiableShuffledVotes,
				precedingVerifiableDecryptions);

		LOGGER.debug("Performing VerifyMixDecOnline algorithm... [electionEventId: {}, ballotBoxId: {}, nodeId: {}]", electionEventId, ballotBoxId,
				nodeId);

		return verifyMixDecOnlineAlgorithm.verifyMixDecOnline(verifyMixDecOnlineContext, verifyMixDecInput);
	}
}
