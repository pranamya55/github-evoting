/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysService;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyKeyGenerationSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyKeyGenerationSchnorrProofsInput;

@Service
public class VerifySetupComponentPublicKeysService {

	public static final Logger LOGGER = LoggerFactory.getLogger(VerifySetupComponentPublicKeysService.class);

	private final int nodeId;
	private final ElectionEventService electionEventService;
	private final ElectionEventContextService electionEventContextService;
	private final CcmjElectionKeysService ccmjElectionKeysService;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	private final VerifyKeyGenerationSchnorrProofsAlgorithm verifyKeyGenerationSchnorrProofsAlgorithm;

	public VerifySetupComponentPublicKeysService(
			@Value("${nodeID}")
			final int nodeId,
			final ElectionEventService electionEventService,
			final ElectionEventContextService electionEventContextService,
			final CcmjElectionKeysService ccmjElectionKeysService,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService,
			final VerifyKeyGenerationSchnorrProofsAlgorithm verifyKeyGenerationSchnorrProofsAlgorithm) {
		this.nodeId = nodeId;
		this.electionEventService = electionEventService;
		this.electionEventContextService = electionEventContextService;
		this.ccmjElectionKeysService = ccmjElectionKeysService;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
		this.verifyKeyGenerationSchnorrProofsAlgorithm = verifyKeyGenerationSchnorrProofsAlgorithm;
	}

	/**
	 * Verifies the {@link SetupComponentPublicKeys} by verifying:
	 * <ul>
	 *     <li>the consistency of the encryption group</li>
	 *     <li>the election event is not over</li>
	 *     <li>the received CCRj and CCMj public keys match the saved CCRj and CCMj public keys</li>
	 *     <li>the key generation Schnorr proofs of knowledge with the {@link VerifyKeyGenerationSchnorrProofsAlgorithm}</li>
	 * </ul>
	 *
	 * @param electionEventId          the election event id. Must be non-null and a valid UUID.
	 * @param setupComponentPublicKeys the {@link SetupComponentPublicKeys}. Must be non-null.
	 * @throws IllegalStateException if any consistency or key generation Schnorr proofs validation fails.
	 */
	public void verifySetupComponentPublicKeys(final String electionEventId, final SetupComponentPublicKeys setupComponentPublicKeys) {
		validateUUID(electionEventId);
		checkNotNull(setupComponentPublicKeys);

		// Verify group consistency.
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		checkState(encryptionGroup.equals(setupComponentPublicKeys.electionPublicKey().getGroup()),
				"The Setup Component encryption group does not match the saved encryption group. [electionEventId: %s, nodeId: %s]", electionEventId,
				nodeId);

		// Verify election event is not over.
		final LocalDateTime electionEventFinishTime = electionEventContextService.getElectionEventFinishTime(electionEventId);
		checkState(LocalDateTimeUtils.now().isBefore(electionEventFinishTime), "The election event is over. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		// Verify public keys consistency.
		final ControlComponentPublicKeys receivedControlComponentPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys().stream()
				.filter(controlComponentPublicKeys -> controlComponentPublicKeys.nodeId() == nodeId)
				.collect(MoreCollectors.onlyElement());

		final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey = ccrjReturnCodesKeysService.getCcrjChoiceReturnCodesEncryptionKeyPair(
				electionEventId).getPublicKey();
		checkState(ccrjChoiceReturnCodesEncryptionPublicKey.equals(receivedControlComponentPublicKeys.ccrjChoiceReturnCodesEncryptionPublicKey()),
				"The Setup Component CCRj Return Codes encryption public key does not match the saved CCRj Choice Return Codes encryption public key. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = ccrjReturnCodesKeysService.getCcrjSchnorrProofs(electionEventId);
		checkState(ccrjSchnorrProofs.equals(receivedControlComponentPublicKeys.ccrjSchnorrProofs()),
				"The Setup Component CCRj Return Codes Schnorr proofs do not match the saved CCRj Schnorr proofs. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final ElGamalMultiRecipientPublicKey ccmjElectionPublicKey = ccmjElectionKeysService.getCcmjElectionKeyPair(electionEventId).getPublicKey();
		checkState(ccmjElectionPublicKey.equals(receivedControlComponentPublicKeys.ccmjElectionPublicKey()),
				"The Setup Component CCMj election public key does not match the saved CCMj election public key. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final GroupVector<SchnorrProof, ZqGroup> ccmjSchnorrProofs = ccmjElectionKeysService.getCcmjSchnorrProofs(electionEventId);
		checkState(ccmjSchnorrProofs.equals(receivedControlComponentPublicKeys.ccmjSchnorrProofs()),
				"The Setup Component CCMj Schnorr proofs do not match the saved CCMj Schnorr proofs. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		// Verify public keys Schnorr proofs.
		final ElectionEventContext electionEventContext = electionEventContextService.getElectionEventContext(electionEventId);
		final VerifyKeyGenerationSchnorrProofsInput verifyKeyGenerationSchnorrProofsInput = new VerifyKeyGenerationSchnorrProofsInput(
				setupComponentPublicKeys);
		checkState(verifyKeyGenerationSchnorrProofsAlgorithm.verifyKeyGenerationSchnorrProofs(electionEventContext,
				verifyKeyGenerationSchnorrProofsInput), "The Schnorr proofs are invalid. [electionEventId: %s, nodeId: %s]", electionEventId, nodeId);
	}
}
