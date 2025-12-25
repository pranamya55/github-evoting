/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.setup.process.generate.ControlComponentCodeSharesPayloadService.ControlComponentCodeSharesPayloadsChunk;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.setup.process.SetupKeyPairService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.CombineEncLongCodeSharesOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.CombineEncLongCodeSharesService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCMTableOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCMTableService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@Service
@ConditionalOnProperty("role.isSetup")
public class VerificationCardSetDataGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetDataGenerationService.class);

	private final GenCMTableService genCMTableService;
	private final SetupKeyPairService setupKeyPairService;
	private final ElectionEventService electionEventService;
	private final VerificationCardSetService verificationCardSetService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final CombineEncLongCodeSharesService combineEncLongCodeSharesService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final ControlComponentCodeSharesPayloadService controlComponentCodeSharesPayloadService;
	private final EncryptedNodeLongReturnCodeSharesService encryptedNodeLongReturnCodeSharesService;

	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public VerificationCardSetDataGenerationService(
			final GenCMTableService genCMTableService,
			final SetupKeyPairService setupKeyPairService,
			final ElectionEventService electionEventService,
			final VerificationCardSetService verificationCardSetService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final CombineEncLongCodeSharesService combineEncLongCodeSharesService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final ControlComponentCodeSharesPayloadService controlComponentCodeSharesPayloadService,
			final EncryptedNodeLongReturnCodeSharesService encryptedNodeLongReturnCodeSharesService,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		this.genCMTableService = genCMTableService;
		this.setupKeyPairService = setupKeyPairService;
		this.electionEventService = electionEventService;
		this.verificationCardSetService = verificationCardSetService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.combineEncLongCodeSharesService = combineEncLongCodeSharesService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.controlComponentCodeSharesPayloadService = controlComponentCodeSharesPayloadService;
		this.encryptedNodeLongReturnCodeSharesService = encryptedNodeLongReturnCodeSharesService;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	/**
	 * Calls the CombineEncLongCodeShares and GenCMTable algorithms.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return a {@link ReturnCodesGenerationOutput} containing outputs of the CombineEncLongCodeShares and GenCMTable algorithms.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public ReturnCodesGenerationOutput generate(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(electionEventService.exists(electionEventId), "The given election event ID does not exist. [electionEventId: %s]",
				electionEventId);
		checkArgument(verificationCardSetService.exists(verificationCardSetId),
				"The given verification card set ID does not belong to the given election event ID. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, electionEventId);

		// Load control component code shares payloads chunk paths
		final ImmutableList<Path> controlComponentCodeSharesPayloadsChunkPaths = controlComponentCodeSharesPayloadService.findAllPathsOrderedByChunkId(
				electionEventId,
				verificationCardSetId);
		verifyChunkPaths(electionEventId, verificationCardSetId, controlComponentCodeSharesPayloadsChunkPaths);

		LOGGER.info(
				"Control component code shares payload chunk paths successfully loaded. [electionEventId: {}, verificationCardSetId: {}, chunks: {}]",
				electionEventId, verificationCardSetId, controlComponentCodeSharesPayloadsChunkPaths.size());

		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);
		final PrimesMappingTable primesMappingTable = electionEventContextPayloadService.loadPrimesMappingTable(electionEventId,
				verificationCardSetId);
		final ImmutableList<String> correctnessInformation = primesMappingTableAlgorithms.getCorrectnessInformation(primesMappingTable,
				ImmutableList.emptyList());
		final ElGamalMultiRecipientPrivateKey setupSecretKey = setupKeyPairService.load(electionEventId).getPrivateKey();

		final ImmutableList<ReturnCodesGenerationOutputChunk> returnCodesGenerationOutputChunks = controlComponentCodeSharesPayloadsChunkPaths.stream()
				.parallel()
				.map(controlComponentCodeSharesPayloadsPath -> {
					// Load control component code shares payloads chunk and convert it
					final ControlComponentCodeSharesPayloadsChunk controlComponentCodeSharesPayloadsChunk = controlComponentCodeSharesPayloadService.load(
							controlComponentCodeSharesPayloadsPath);
					final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk = encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(
							electionEventId, verificationCardSetId, controlComponentCodeSharesPayloadsChunk);

					final int chunkId = encryptedNodeLongReturnCodeSharesChunk.getChunkId();

					final CombineEncLongCodeSharesOutput combineEncLongCodeSharesOutput = combineEncLongCodeSharesService.combineEncLongCodeShares(
							electionEventContextPayload, primesMappingTable, verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk,
							setupSecretKey);
					LOGGER.info(
							"Encrypted long return code shares successfully combined. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					final ImmutableList<String> verificationCardIds = encryptedNodeLongReturnCodeSharesChunk.getVerificationCardIds();
					final GenCMTableOutput genCMTableOutput = genCMTableService.genCMTable(electionEventContextPayload, correctnessInformation,
							verificationCardSetId, verificationCardIds, chunkId, setupSecretKey, combineEncLongCodeSharesOutput);
					LOGGER.info(
							"Return codes mapping table successfully generated. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					return new ReturnCodesGenerationOutputChunk(verificationCardIds,
							combineEncLongCodeSharesOutput.getLongVoteCastReturnCodesAllowList(), genCMTableOutput);
				}).collect(toImmutableList());

		final int numberOfEligibleVoters = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().stream().parallel()
				.filter(verificationCardSetContext -> verificationCardSetContext.getVerificationCardSetId().equals(verificationCardSetId))
				.map(VerificationCardSetContext::getNumberOfEligibleVoters)
				.collect(MoreCollectors.onlyElement());
		final ImmutableSet<String> treatedVerificationCardIds = returnCodesGenerationOutputChunks.stream()
				.flatMap(chunk -> chunk.verificationCardIds().stream())
				.collect(toImmutableSet());
		checkState(numberOfEligibleVoters == treatedVerificationCardIds.size());

		LOGGER.info("Return codes generation finished. [electionEventId: {}, verificationCardSetId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId, verificationCardSetId);

		final List<String> verificationCardIds = new ArrayList<>();
		final List<String> longVoteCastReturnCodesAllowList = new ArrayList<>();
		final List<String> shortVoteCastReturnCodes = new ArrayList<>();
		final List<ImmutableList<String>> shortChoiceReturnCodes = new ArrayList<>();
		final Map<String, String> returnCodesMappingTable = new TreeMap<>();
		returnCodesGenerationOutputChunks.forEach(returnCodesGenerationOutputChunk -> {
			verificationCardIds.addAll(returnCodesGenerationOutputChunk.verificationCardIds().asList());
			longVoteCastReturnCodesAllowList.addAll(returnCodesGenerationOutputChunk.longVoteCastReturnCodesAllowList().asList());
			shortChoiceReturnCodes.addAll(returnCodesGenerationOutputChunk.genCMTableOutput().shortChoiceReturnCodes().asList());
			shortVoteCastReturnCodes.addAll(returnCodesGenerationOutputChunk.genCMTableOutput().shortVoteCastReturnCodes().asList());
			returnCodesMappingTable.putAll(returnCodesGenerationOutputChunk.genCMTableOutput().returnCodesMappingTable().asMap());
		});

		return new ReturnCodesGenerationOutput.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(ImmutableList.from(verificationCardIds))
				.setShortVoteCastReturnCodes(ImmutableList.from(shortVoteCastReturnCodes))
				.setLongVoteCastReturnCodesAllowList(ImmutableList.from(longVoteCastReturnCodesAllowList))
				.setReturnCodesMappingTable(ImmutableMap.from(returnCodesMappingTable, TreeMap::new))
				.setShortChoiceReturnCodes(ImmutableList.from(shortChoiceReturnCodes))
				.build();
	}

	/**
	 * Verifies the consistency of the chunks' paths between the SetupComponentVerificationDataPayloads and ControlComponentCodeSharesPayloads.
	 */
	private void verifyChunkPaths(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<Path> codeSharesPayloadsChunkPaths) {
		checkState(!codeSharesPayloadsChunkPaths.isEmpty(),
				"No control component code shares payloads. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final ImmutableList<Path> verificationDataPayloadChunkPaths = setupComponentVerificationDataPayloadFileRepository.findAllPathsOrderByChunkId(
				electionEventId, verificationCardSetId);
		checkState(codeSharesPayloadsChunkPaths.size() == verificationDataPayloadChunkPaths.size(),
				"The SetupComponentVerificationDataPayloads and ControlComponentCodeSharesPayloads have different chunk counts. "
						+ "[electionEventId: %s, verificationCardSetId: %s, setupComponentChunkCount: %s, , controlComponentChunkCount: %s]",
				electionEventId, verificationCardSetId, verificationDataPayloadChunkPaths.size(), codeSharesPayloadsChunkPaths.size());

		IntStream.range(0, codeSharesPayloadsChunkPaths.size()).parallel()
				.forEach(i -> {
					final Path codeSharesPayloadsChunkPath = codeSharesPayloadsChunkPaths.get(i);
					final Path verificationDataPayloadChunkPath = verificationDataPayloadChunkPaths.get(i);
					final int codeSharesPayloadsChunkId = controlComponentCodeSharesPayloadService.getChunkId(codeSharesPayloadsChunkPath);
					final int verificationDataPayloadChunkId = setupComponentVerificationDataPayloadFileRepository.getChunkId(
							verificationDataPayloadChunkPath);
					checkState(i == codeSharesPayloadsChunkId && codeSharesPayloadsChunkId == verificationDataPayloadChunkId,
							"The chunk ID sequence is interrupted or incomplete. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
							verificationCardSetId);
				});
	}

	record ReturnCodesGenerationOutputChunk(ImmutableList<String> verificationCardIds, ImmutableList<String> longVoteCastReturnCodesAllowList,
											GenCMTableOutput genCMTableOutput) {
	}

}
