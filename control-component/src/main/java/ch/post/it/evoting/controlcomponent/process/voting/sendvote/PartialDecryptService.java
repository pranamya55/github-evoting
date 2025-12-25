/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.PartialDecryptPCCOutput;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.PartialDecryptPCCService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.VerifyBallotCCRService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Verifies the encrypted vote's zero-knowledge proofs and partially decrypts the partial Choice Return Codes.
 */
@Service
public class PartialDecryptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartialDecryptService.class);

	private final VerifyBallotCCRService verifyBallotCCRService;
	private final VerificationCardService verificationCardService;
	private final IdentifierValidationService identifierValidationService;
	private final PartialDecryptPCCService partialDecryptPCCService;
	private final ElectionEventContextService electionEventContextService;
	private final ElectionEventService electionEventService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final BallotBoxService ballotBoxService;
	private final EncryptedVerifiableVoteService encryptedVerifiableVoteService;

	@Value("${nodeID}")
	private int nodeId;

	public PartialDecryptService(
			final VerifyBallotCCRService verifyBallotCCRService,
			final VerificationCardService verificationCardService,
			final IdentifierValidationService identifierValidationService,
			final PartialDecryptPCCService partialDecryptPCCService,
			final ElectionEventContextService electionEventContextService,
			final ElectionEventService electionEventService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final BallotBoxService ballotBoxService,
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService) {
		this.verifyBallotCCRService = verifyBallotCCRService;
		this.verificationCardService = verificationCardService;
		this.identifierValidationService = identifierValidationService;
		this.partialDecryptPCCService = partialDecryptPCCService;
		this.electionEventContextService = electionEventContextService;
		this.electionEventService = electionEventService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.ballotBoxService = ballotBoxService;
		this.encryptedVerifiableVoteService = encryptedVerifiableVoteService;
	}

	/**
	 * Verifies the {@link EncryptedVerifiableVote} in the VerifyBallotCCR_j algorithm and then partially decrypts the encrypted partial Choice Return
	 * Codes with the CCR_j Choice Return Codes encryption secret key in the PartialDecryptPCC_j algorithm.
	 *
	 * @param encryptedVerifiableVote the object containing the encrypted vote and the corresponding zero-knowledge proofs.
	 * @return the partially decrypted encrypted Partial Choice Return Codes as a {@link PartiallyDecryptedEncryptedPCC}.
	 */
	@Transactional
	public PartiallyDecryptedEncryptedPCC performPartialDecrypt(final EncryptedVerifiableVote encryptedVerifiableVote) {

		checkNotNull(encryptedVerifiableVote);

		final ContextIds contextIds = encryptedVerifiableVote.contextIds();

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();

		final String verificationCardSetId = verificationCardService.getVerificationCardEntity(verificationCardId).getVerificationCardSetEntity()
				.getVerificationCardSetId();
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		validateVoteIsAllowed(electionEventId, verificationCardId, LocalDateTime::now, ballotBoxEntity);

		LOGGER.debug("Starting partial decryption of partial Choice Return Codes. [contextIds: {}]", contextIds);

		final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId);
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeysService.getElectionPublicKey(electionEventId);
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = setupComponentPublicKeysService.getChoiceReturnCodesEncryptionPublicKey(
				electionEventId);

		// Verify the encrypted vote's zero-knowledge proofs.
		if (!verifyBallotCCRService.verifyBallotCCR(encryptionGroup, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey,
				encryptedVerifiableVote)) {
			LOGGER.error("The client's encrypted vote zero-knowledge proofs are invalid. [contextIds: {}]", contextIds);
			throw new IllegalStateException("The client's encrypted vote zero-knowledge proofs are invalid.");
		}
		LOGGER.debug("The client's encrypted vote zero-knowledge proofs are valid. [contextIds: {}]", contextIds);

		// We store the encrypted verifiable vote payload only after all the relevant checks and verifications of the encrypted vote have passed.
		encryptedVerifiableVoteService.save(encryptedVerifiableVote);
		LOGGER.info("Saved encrypted verifiable vote. [contextIds: {}]", contextIds);

		final PartialDecryptPCCOutput partialDecryptPCCOutput = partialDecryptPCCService.partialDecryptPCC(encryptionGroup, electionEventId,
				primesMappingTable, encryptedVerifiableVote);

		LOGGER.info(
				"Partial Decrypt PCC algorithm successfully performed. Successfully partially decrypted the encrypted partial Choice Return Codes. [contextIds: {}, nodeId: {}]",
				contextIds, nodeId);

		final GroupVector<GqElement, GqGroup> exponentiatedGammas = partialDecryptPCCOutput.exponentiatedGammas();
		final GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs = partialDecryptPCCOutput.exponentiationProofs();

		return new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, exponentiatedGammas, exponentiationProofs);
	}

	@VisibleForTesting
	void validateVoteIsAllowed(final String electionEventId, final String verificationCardId, final Supplier<LocalDateTime> now,
			final BallotBoxEntity ballotBox) {
		validateUUID(electionEventId);
		validateUUID(verificationCardId);
		checkNotNull(now);
		checkNotNull(ballotBox);

		final LocalDateTime electionStartTime = electionEventContextService.getElectionEventStartTime(electionEventId);
		final LocalDateTime electionEndTime = electionEventContextService.getElectionEventFinishTime(electionEventId);
		final LocalDateTime currentTime = now.get();

		final boolean afterStartTime = currentTime.isAfter(electionStartTime) || currentTime.isEqual(electionStartTime);
		final boolean beforeEndTime = currentTime.isBefore(electionEndTime.plusSeconds(ballotBox.getGracePeriod())) || currentTime
				.isEqual(electionEndTime.plusSeconds(ballotBox.getGracePeriod()));

		checkState(afterStartTime && beforeEndTime,
				"Impossible to vote before or after the dedicated time. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s, "
						+ "startTime: %s, finishTime: %s, gracePeriod: %s]",
				electionEventId, ballotBox.getBallotBoxId(), verificationCardId, electionStartTime, electionEndTime, ballotBox.getGracePeriod());
		checkState(!ballotBox.isMixed(),
				"Impossible to vote in an already mixed ballot box. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s]",
				electionEventId, ballotBox.getBallotBoxId(), verificationCardId);
	}
}
