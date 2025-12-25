/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.TallyComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.VerifiablePlaintextDecryption;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsOutput;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.MixDecOfflineOutput;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.MixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.ProcessPlaintextsService;

/**
 * Handles the offline mixing steps.
 */
@Service
@ConditionalOnProperty("role.isTally")
public class MixOfflineFacade {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixOfflineFacade.class);

	private final BallotBoxService ballotBoxService;
	private final MixDecOfflineService mixDecOfflineService;
	private final VerifyMixOfflineService verifyMixOfflineService;
	private final ProcessPlaintextsService processPlaintextsService;
	private final TallyComponentVotesService tallyComponentVotesService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final TallyComponentShufflePayloadFileRepository tallyComponentShufflePayloadFileRepository;

	@Autowired
	MixOfflineFacade(final BallotBoxService ballotBoxService,
			final MixDecOfflineService mixDecOfflineService,
			final VerifyMixOfflineService verifyMixOfflineService,
			final ProcessPlaintextsService processPlaintextsService,
			final TallyComponentVotesService tallyComponentVotesService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final TallyComponentShufflePayloadFileRepository tallyComponentShufflePayloadFileRepository) {
		this.ballotBoxService = ballotBoxService;
		this.mixDecOfflineService = mixDecOfflineService;
		this.verifyMixOfflineService = verifyMixOfflineService;
		this.processPlaintextsService = processPlaintextsService;
		this.tallyComponentVotesService = tallyComponentVotesService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.tallyComponentShufflePayloadFileRepository = tallyComponentShufflePayloadFileRepository;
	}

	/**
	 * Coordinates the offline mixing: mixing, decryption, factorisation and persistence.
	 *
	 * @param electionEventId                the id of the election event for which we want to mix a ballot box. Must be non-null and a valid UUID.
	 * @param ballotBoxId                    the id of the ballot box to mix. Must be non-null and a valid UUID.
	 * @param electoralBoardMembersPasswords the list of electoral board members' password. Not null
	 */
	public void mixOffline(final String electionEventId, final String ballotBoxId,
			final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(electoralBoardMembersPasswords);
		checkArgument(electoralBoardMembersPasswords.size() >= 2);

		final ElectionEventContextPayload electionEventContextPayload = loadElectionEventContextPayload(electionEventId);
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();

		validateConsistency(electionEventId, ballotBoxId);

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final VerificationCardSetContext verificationCardSetContext = electionEventContext.verificationCardSetContexts().stream()
				.filter(vcsContext -> vcsContext.getBallotBoxId().equals(ballotBoxId))
				.collect(MoreCollectors.onlyElement());

		final PrimesMappingTable primesMappingTable = verificationCardSetContext.getPrimesMappingTable();

		final ControlComponentShufflePayload controlComponentShufflePayload = verifyMixOfflineService.verifyMixDecrypt(electionEventId, ballotBoxId,
				verificationCardSetContext);

		ballotBoxService.updateStatus(ballotBoxId, BallotBoxStatus.DECRYPTING);
		LOGGER.info("Mixing and decrypting. [electionEventId: {},  ballotBoxId: {}]", electionEventId, ballotBoxId);

		final MixDecOfflineOutput mixDecOfflineOutput = mixDecOfflineService.mixDecOffline(electionEventId, ballotBoxId, primesMappingTable,
				controlComponentShufflePayload, electoralBoardMembersPasswords);
		LOGGER.info("Successfully mixed the votes and performed the final decryption. [electionEventId: {},  ballotBoxId: {}]", electionEventId,
				ballotBoxId);

		final TallyComponentShufflePayload tallyComponentShufflePayload = createAndPersistTallyComponentShufflePayload(electionEventId,
				ballotBoxId, encryptionGroup, mixDecOfflineOutput);
		LOGGER.info("Persisted tally component shuffle payload. [electionEventId: {},  ballotBoxId: {}]", electionEventId,
				ballotBoxId);

		final ProcessPlaintextsOutput processPlaintextsOutput = processPlaintextsService.processPlaintexts(electionEventId, ballotBoxId,
				tallyComponentShufflePayload, primesMappingTable);
		LOGGER.info("Voter selections factorized. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		createAndPersistTallyComponentVotesPayload(electionEventId, ballotBoxId, encryptionGroup, processPlaintextsOutput);
		LOGGER.info("Persisted tally component votes payload. [electionEventId: {}, ballotBoxId: {}]", electionEventId,
				ballotBoxId);
	}

	private void validateConsistency(final String electionEventId, final String ballotBoxId) {
		checkState(ballotBoxService.isDownloaded(ballotBoxId),
				"Ballot box has not been downloaded, hence it cannot be mixed. [electionEventId: %s, ballotBoxId: %S]", electionEventId, ballotBoxId);

		validateMixIsAllowed(electionEventId, ballotBoxId, LocalDateTime::now);
		validateBallotBoxConsistency(electionEventId);
	}

	@VisibleForTesting
	void validateMixIsAllowed(final String electionEventId, final String ballotBoxId, final Supplier<LocalDateTime> now) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(now);

		final int gracePeriod = ballotBoxService.getGracePeriod(ballotBoxId);
		final boolean isTestBallotBox = ballotBoxService.isTestBallotBox(ballotBoxId);
		final ElectionEventContext electionEventContext = electionEventContextPayloadService.load(electionEventId).getElectionEventContext();

		final boolean afterGracePeriod = now.get().isAfter(electionEventContext.finishTime().plusSeconds(gracePeriod));

		// Test ballot boxes can be mixed and decrypted at any time. Real ballot boxes can be mixed and decrypted only after the election event period ended.
		checkState(isTestBallotBox || afterGracePeriod,
				BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE + "[isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
				isTestBallotBox, electionEventContext.finishTime(), electionEventId, ballotBoxId);
	}

	@VisibleForTesting
	void validateBallotBoxConsistency(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<String> ballotBoxIdsFromContext = electionEventContextPayloadService.load(electionEventId).getElectionEventContext()
				.verificationCardSetContexts().stream()
				.map(VerificationCardSetContext::getBallotBoxId)
				.collect(toImmutableList());

		final ImmutableList<String> ballotBoxIdsFromDb = ballotBoxService.getBallotBoxIds(electionEventId);

		final boolean isCountEquals = ballotBoxIdsFromDb.size() == ballotBoxIdsFromContext.size();
		checkState(isCountEquals,
				"The number of ballot boxes in the DB and in the context mismatch. [electionEventId: %s, dbCount: %s, contextCount: %s]",
				electionEventId, ballotBoxIdsFromDb.size(), ballotBoxIdsFromContext.size());

		checkState(hasNoDuplicates(ballotBoxIdsFromDb), "There are duplicate values. [electionEventId: %s, dbContent: %s]", electionEventId,
				ballotBoxIdsFromDb);
		checkState(hasNoDuplicates(ballotBoxIdsFromContext), "There are duplicate values. [electionEventId: %s, contextContent: %s]", electionEventId,
				ballotBoxIdsFromContext);

		final ImmutableSet<String> uniqueBallotBoxIdsFromContext = ballotBoxIdsFromContext.toImmutableSet();
		final ImmutableSet<String> uniqueBallotBoxIdsFromDb = ballotBoxIdsFromDb.toImmutableSet();
		final boolean isIdsStrictlyEqual = uniqueBallotBoxIdsFromContext.equals(uniqueBallotBoxIdsFromDb);
		checkState(isIdsStrictlyEqual,
				"The ballot boxes are not the same in the DB and in the context. [electionEventId: %s, dbContent: %s, contextContent: %s]",
				electionEventId, uniqueBallotBoxIdsFromDb, uniqueBallotBoxIdsFromContext);
	}

	private ElectionEventContextPayload loadElectionEventContextPayload(final String electionEventId) {
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);

		final CryptoPrimitivesSignature signature = electionEventContextPayload.getSignature();

		checkState(signature != null, "The signature of the election event context payload is null. [electionEventId: %s]", electionEventId);

		final Hashable additionalContextData = ChannelSecurityContextData.electionEventContext(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, electionEventContextPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the election event context. [electionEventId: %s]", electionEventId));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ElectionEventContextPayload.class,
					String.format("[electionEventId: %s]", electionEventId));
		}
		return electionEventContextPayload;
	}

	private TallyComponentShufflePayload createAndPersistTallyComponentShufflePayload(final String electionEventId, final String ballotBoxId,
			final GqGroup encryptionGroup, final MixDecOfflineOutput mixDecOfflineOutput) {

		final VerifiableShuffle verifiableShuffle = mixDecOfflineOutput.getVerifiableShuffle();
		final VerifiablePlaintextDecryption verifiablePlaintextDecryption = mixDecOfflineOutput.getVerifiablePlaintextDecryption();

		final TallyComponentShufflePayload tallyComponentShufflePayload = new TallyComponentShufflePayload(encryptionGroup, electionEventId,
				ballotBoxId, verifiableShuffle, verifiablePlaintextDecryption);

		final Hashable additionalContextData = ChannelSecurityContextData.tallyComponentShuffle(electionEventId, ballotBoxId);
		final CryptoPrimitivesSignature signature = getPayloadSignature(tallyComponentShufflePayload, additionalContextData);
		tallyComponentShufflePayload.setSignature(signature);

		tallyComponentShufflePayloadFileRepository.savePayload(electionEventId, ballotBoxId, tallyComponentShufflePayload);

		return tallyComponentShufflePayload;
	}

	private void createAndPersistTallyComponentVotesPayload(final String electionEventId, final String ballotBoxId, final GqGroup encryptionGroup,
			final ProcessPlaintextsOutput processPlaintextsOutput) {

		final GroupVector<GroupVector<PrimeGqElement, GqGroup>, GqGroup> encodedSelectedVotingOptions = processPlaintextsOutput.getListOfDecryptedVotes();
		final ImmutableList<ImmutableList<String>> actualSelectedVotingOptions = processPlaintextsOutput.getListOfDecodedVotes();
		final ImmutableList<ImmutableList<String>> decodedWriteInVotes = processPlaintextsOutput.getListOfDecodedWriteIns();

		final TallyComponentVotesPayload tallyComponentVotesPayload = new TallyComponentVotesPayload(encryptionGroup, electionEventId, ballotBoxId,
				encodedSelectedVotingOptions, actualSelectedVotingOptions, decodedWriteInVotes);

		final Hashable additionalContextData = ChannelSecurityContextData.tallyComponentVotes(electionEventId, ballotBoxId);
		final CryptoPrimitivesSignature signature = getPayloadSignature(tallyComponentVotesPayload, additionalContextData);
		tallyComponentVotesPayload.setSignature(signature);

		tallyComponentVotesService.save(tallyComponentVotesPayload);
	}

	private CryptoPrimitivesSignature getPayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException se) {
			throw new IllegalStateException(
					String.format("Failed to generate payload signature [%s, %s]", payload.getClass().getSimpleName(), additionalContextData), se);
		}
	}
}
