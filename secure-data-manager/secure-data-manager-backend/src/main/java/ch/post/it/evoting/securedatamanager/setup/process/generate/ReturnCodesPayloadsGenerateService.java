/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.process.Status.GENERATED;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.ChoiceReturnCodeToEncodedVotingOptionEntry;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.domain.configuration.VoterReturnCodes;
import ch.post.it.evoting.domain.configuration.VoterReturnCodesPayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ReturnCodesPayloadsGenerateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesPayloadsGenerateService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final VerificationCardSetService verificationCardSetService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final ReturnCodesPayloadsPersistenceService returnCodesPayloadsPersistenceService;
	private final VerificationCardSetDataGenerationService verificationCardSetDataGenerationService;
	private final int chunkSize;

	public ReturnCodesPayloadsGenerateService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final VerificationCardSetService verificationCardSetService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final ReturnCodesPayloadsPersistenceService returnCodesPayloadsPersistenceService,
			final VerificationCardSetDataGenerationService verificationCardSetDataGenerationService,
			@Value("${sdm.process.generate.CMTable-chunk-size}")
			final int chunkSize) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.verificationCardSetService = verificationCardSetService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.verificationCardSetDataGenerationService = verificationCardSetDataGenerationService;
		this.returnCodesPayloadsPersistenceService = returnCodesPayloadsPersistenceService;
		this.chunkSize = chunkSize;
	}

	/**
	 * Generates and persists on the file system the following payloads:
	 * <ul>
	 *     <li>Setup component CMTable payload</li>
	 *     <li>Voter Return Codes payload</li>
	 *     <li>Setup component LVCC allow list payload</li>
	 * </ul>
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public void generate(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.info("Generating return codes payloads... [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		// ReturnCodesGenerationOutput
		final ReturnCodesGenerationOutput returnCodesGenerationOutput = verificationCardSetDataGenerationService.generate(electionEventId,
				verificationCardSetId);

		// Setup Component CMTable Payloads
		final SetupComponentCMTablePayloadChunks setupComponentCMTablePayloadChunks = getSetupComponentCMTablePayloads(electionEventId,
				returnCodesGenerationOutput);
		LOGGER.debug("Successfully created the setup component CMTable payloads. [electionEventId: {}, verificationCardSetId: {}, chunkCount: {}]",
				electionEventId, returnCodesGenerationOutput.getVerificationCardSetId(), setupComponentCMTablePayloadChunks.getChunkCount());

		// Voter Return Codes payload
		final VoterReturnCodesPayload voterReturnCodesPayload = generateVoterReturnCodesPayload(returnCodesGenerationOutput);

		// Setup component LVCC allow list payload
		final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload =
				generateLongVoteCastReturnCodesAllowListPayload(returnCodesGenerationOutput);

		// Persist the payloads
		returnCodesPayloadsPersistenceService.save(returnCodesGenerationOutput.getElectionEventId(), verificationCardSetId,
				setupComponentCMTablePayloadChunks, voterReturnCodesPayload, setupComponentLVCCAllowListPayload);

		LOGGER.info("Return codes payloads are successfully generated and persisted. [electionEventId: {}, verificationCardSetId: {}]",
				returnCodesGenerationOutput.getElectionEventId(), verificationCardSetId);

		verificationCardSetService.updateStatus(verificationCardSetId, GENERATED);
		LOGGER.info("Verification card set is generated. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);
	}

	private SetupComponentCMTablePayloadChunks getSetupComponentCMTablePayloads(final String electionEventId,
			final ReturnCodesGenerationOutput returnCodesGenerationOutput) {

		final String verificationCardSetId = returnCodesGenerationOutput.getVerificationCardSetId();
		final ImmutableMap<String, String> returnCodesMappingTable = returnCodesGenerationOutput.getReturnCodesMappingTable();
		final Iterator<ImmutableMap.Entry<String, String>> iterator = returnCodesMappingTable.entrySet().iterator();

		final List<SetupComponentCMTablePayload> payloadChunks = new ArrayList<>();
		Map<String, String> returnCodesMappingTableChunk = new TreeMap<>();
		for (int i = 0; i < returnCodesMappingTable.size(); i++) {

			final ImmutableMap.Entry<String, String> entry = iterator.next();
			returnCodesMappingTableChunk.put(entry.key(), entry.value());

			// If the chunked CMTable is full then we create the payload, sign it and add it to the list of payloads.
			if (returnCodesMappingTableChunk.size() == chunkSize || !iterator.hasNext()) {
				final SetupComponentCMTablePayload setupComponentCMTablePayload = new SetupComponentCMTablePayload.Builder()
						.setElectionEventId(electionEventId)
						.setVerificationCardSetId(verificationCardSetId)
						.setChunkId(i / chunkSize)
						.setReturnCodesMappingTable(ImmutableMap.from(returnCodesMappingTableChunk, TreeMap::new))
						.build();

				final Hashable additionalContextData = ChannelSecurityContextData.setupComponentCMTable(electionEventId, verificationCardSetId);
				final CryptoPrimitivesSignature setupComponentCMTablePayloadSignature = getPayloadSignature(setupComponentCMTablePayload,
						additionalContextData);
				setupComponentCMTablePayload.setSignature(setupComponentCMTablePayloadSignature);

				payloadChunks.add(setupComponentCMTablePayload);

				// Prepare for the next chunk
				returnCodesMappingTableChunk = new TreeMap<>();
			}
		}
		return new SetupComponentCMTablePayloadChunks(ImmutableList.from(payloadChunks));
	}

	private VoterReturnCodesPayload generateVoterReturnCodesPayload(final ReturnCodesGenerationOutput returnCodesGenerationOutput) {
		final String electionEventId = returnCodesGenerationOutput.getElectionEventId();
		final String verificationCardSetId = returnCodesGenerationOutput.getVerificationCardSetId();
		final GqGroup encryptionGroup = electionEventContextPayloadService.loadEncryptionGroup(electionEventId);

		final PrimesMappingTable primesMappingTable = electionEventContextPayloadService.loadPrimesMappingTable(electionEventId,
				verificationCardSetId);
		final GroupVector<PrimeGqElement, GqGroup> encodedVotingOptions = primesMappingTable.pTable().stream()
				.parallel()
				.map(PrimesMappingTableEntry::encodedVotingOption)
				.collect(GroupVector.toGroupVector());

		final ImmutableList<String> verificationCardIds = returnCodesGenerationOutput.getVerificationCardIds();
		final ImmutableList<ImmutableList<String>> shortChoiceReturnCodesList = returnCodesGenerationOutput.getShortChoiceReturnCodes();
		final ImmutableList<String> shortVoteCastReturnCodes = returnCodesGenerationOutput.getShortVoteCastReturnCodes();

		final ImmutableList<VoterReturnCodes> voterReturnCodes = IntStream.range(0, verificationCardIds.size())
				.parallel()
				.mapToObj(i -> {
					final ImmutableList<String> shortChoiceReturnCodes = shortChoiceReturnCodesList.get(i);

					final GroupVector<ChoiceReturnCodeToEncodedVotingOptionEntry, GqGroup> choiceReturnCodesToEncodedVotingOptions =
							IntStream.range(0, encodedVotingOptions.size())
									.parallel()
									.mapToObj(idx ->
											new ChoiceReturnCodeToEncodedVotingOptionEntry(shortChoiceReturnCodes.get(idx),
													encodedVotingOptions.get(idx)))
									.collect(GroupVector.toGroupVector());

					return new VoterReturnCodes(verificationCardIds.get(i), shortVoteCastReturnCodes.get(i), choiceReturnCodesToEncodedVotingOptions);
				}).collect(toImmutableList());

		return new VoterReturnCodesPayload(encryptionGroup, electionEventId, verificationCardSetId, voterReturnCodes);
	}

	private SetupComponentLVCCAllowListPayload generateLongVoteCastReturnCodesAllowListPayload(
			final ReturnCodesGenerationOutput returnCodesGenerationOutput) {

		final String electionEventId = returnCodesGenerationOutput.getElectionEventId();
		final String verificationCardSetId = returnCodesGenerationOutput.getVerificationCardSetId();
		final ImmutableList<String> longVoteCastReturnCodesAllowList = returnCodesGenerationOutput.getLongVoteCastReturnCodesAllowList();

		final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload =
				new SetupComponentLVCCAllowListPayload(electionEventId, verificationCardSetId, longVoteCastReturnCodesAllowList);
		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentLVCCAllowList(electionEventId, verificationCardSetId);

		setupComponentLVCCAllowListPayload.setSignature(getPayloadSignature(setupComponentLVCCAllowListPayload, additionalContextData));

		LOGGER.debug("Successfully signed setup component LVCC allow list payload. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);

		return setupComponentLVCCAllowListPayload;
	}

	private CryptoPrimitivesSignature getPayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format(
					"Failed to generate the payload signature. [name: %s]", payload.getClass().getName()), e);
		}
	}

}
