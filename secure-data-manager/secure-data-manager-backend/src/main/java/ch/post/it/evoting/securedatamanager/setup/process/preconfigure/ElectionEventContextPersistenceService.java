/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SignatureException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.ElectionTexts;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.mapper.ElectionTextsMapper;
import ch.post.it.evoting.evotinglibraries.xml.mapper.VoteTextsMapper;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoteInformationType;
import ch.post.it.evoting.securedatamanager.setup.process.TenantService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenSetupDataOutput;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventRepository;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ElectionEventContextPersistenceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextPersistenceService.class);

	private final TenantService tenantService;
	private final EvotingConfigService evotingConfigService;
	private final ElectionEventRepository electionEventRepository;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final VerificationCardSetService verificationCardSetService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	public ElectionEventContextPersistenceService(
			final TenantService tenantService,
			final EvotingConfigService evotingConfigService,
			final ElectionEventRepository electionEventRepository,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final VerificationCardSetService verificationCardSetService,
			final ElectionEventContextPayloadService electionEventContextPayloadService
	) {
		this.tenantService = tenantService;
		this.evotingConfigService = evotingConfigService;
		this.electionEventRepository = electionEventRepository;
		this.signatureKeystoreService = signatureKeystoreService;
		this.verificationCardSetService = verificationCardSetService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * Persists the election event context.
	 *
	 * @param electionEventId    the election event id for which to persist the context. Must be non-null and a valid UUID.
	 * @param seed               the encryption parameters seed. Must be non-null and a valid seed.
	 * @param genSetupDataOutput the output of the GenSetupData algorithm as a {@link GenSetupDataOutput}. Must be non-null.
	 */
	public void persist(final String electionEventId, final String seed, final GenSetupDataOutput genSetupDataOutput) {
		validateUUID(electionEventId);
		validateSeed(seed);
		checkNotNull(genSetupDataOutput);

		LOGGER.debug("Building Verification Card Set Contexts... [electionEventId: {}]", electionEventId);

		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = buildVerificationCardSetContexts(electionEventId,
				genSetupDataOutput);

		LOGGER.debug("Built Verification Card Set Contexts. [electionEventId: {}]", electionEventId);

		final ElectionEventEntity electionEventEntity = electionEventRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(String.format("Election event not found. [electionEventId: %s]", electionEventId)));

		final String electionEventAlias = electionEventEntity.getAlias();
		final String electionEventDescription = electionEventEntity.getDefaultDescription();
		final LocalDateTime startTime = electionEventEntity.getDateFrom();
		final LocalDateTime finishTime = electionEventEntity.getDateTo();
		final int maximumNumberOfVotingOptions = genSetupDataOutput.getMaximumNumberOfVotingOptions();
		final int maximumNumberOfSelections = genSetupDataOutput.getMaximumNumberOfSelections();
		final int maximumNumberOfWriteInsPlusOne = genSetupDataOutput.getMaximumNumberOfWriteInsPlusOne();

		final Configuration configuration = evotingConfigService.load();
		final ImmutableList<VoteTexts> votesTexts = configuration.getContest().getVoteInformation().stream()
				.map(VoteInformationType::getVote)
				.map(VoteTextsMapper.INSTANCE::voteTypeToVoteTexts)
				.collect(toImmutableList());
		final ImmutableList<ElectionTexts> electionsTexts = configuration.getContest().getElectionGroupBallot()
				.stream()
				.map(ElectionTextsMapper.INSTANCE::electionTypeToElectionTexts)
				.collect(toImmutableList());

		final ElectionEventContext electionEventContext = new ElectionEventContext(electionEventId, electionEventAlias, electionEventDescription,
				verificationCardSetContexts, startTime, finishTime, maximumNumberOfVotingOptions, maximumNumberOfSelections,
				maximumNumberOfWriteInsPlusOne, votesTexts, electionsTexts);

		final GqGroup encryptionGroup = genSetupDataOutput.getEncryptionGroup();
		final GroupVector<PrimeGqElement, GqGroup> smallPrimes = genSetupDataOutput.getSmallPrimes();
		final ElectionEventContextPayload electionEventContextPayload = createElectionEventContextPayload(encryptionGroup, seed, smallPrimes,
				electionEventContext);
		electionEventContextPayloadService.save(electionEventContextPayload);

		LOGGER.info("Built and persisted the election event context payload. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Builds the list of {@link VerificationCardSetContext} for the given election event id.
	 *
	 * @param electionEventId    the election event id. Must be non-null and a valid UUID.
	 * @param genSetupDataOutput the output of the GenSetupData algorithm as a {@link GenSetupDataOutput}. Must be non-null.
	 * @return the list of {@link VerificationCardSetContext}.
	 * @throws NullPointerException      if the input is null.
	 * @throws FailedValidationException if the input is not a valid UUID.
	 */
	private ImmutableList<VerificationCardSetContext> buildVerificationCardSetContexts(final String electionEventId,
			final GenSetupDataOutput genSetupDataOutput) {
		validateUUID(electionEventId);
		checkNotNull(genSetupDataOutput);

		final ImmutableList<VerificationCardSetEntity> verificationCardSetEntities = verificationCardSetService.getVerificationCardSets(
				electionEventId);
		final ImmutableMap<String, PrimesMappingTable> primesMappingTables = genSetupDataOutput.getPrimesMappingTables();

		return verificationCardSetEntities.stream()
				.parallel()
				.map(verificationCardSet -> {
					final BallotBoxEntity ballotBoxEntity = verificationCardSet.getBallotBoxEntity();
					final PrimesMappingTable primesMappingTable = primesMappingTables.get(verificationCardSet.getVerificationCardSetId());

					return new VerificationCardSetContext.Builder()
							.setVerificationCardSetId(verificationCardSet.getVerificationCardSetId())
							.setVerificationCardSetAlias(verificationCardSet.getAlias())
							.setVerificationCardSetDescription(verificationCardSet.getDefaultDescription())
							.setBallotBoxId(verificationCardSet.getBallotBoxEntity().getBallotBoxId())
							.setBallotBoxStartTime(ballotBoxEntity.getStartTime())
							.setBallotBoxFinishTime(ballotBoxEntity.getFinishTime())
							.setTestBallotBox(ballotBoxEntity.isTest())
							.setNumberOfEligibleVoters(verificationCardSet.getNumberOfEligibleVoters())
							.setGracePeriod(ballotBoxEntity.getGracePeriod())
							.setPrimesMappingTable(primesMappingTable)
							.setDomainsOfInfluence(verificationCardSet.getDomainsOfInfluence())
							.build();
				})
				.collect(toImmutableList());
	}

	private ElectionEventContextPayload createElectionEventContextPayload(final GqGroup group, final String seed,
			final GroupVector<PrimeGqElement, GqGroup> smallPrimes, final ElectionEventContext electionEventContext) {
		final String electionEventId = electionEventContext.electionEventId();
		final ElectionEventContextPayload electionEventContextPayload = new ElectionEventContextPayload(group, seed, smallPrimes,
				electionEventContext, tenantService.getTenantId());

		final Hashable additionalContextData = ChannelSecurityContextData.electionEventContext(electionEventId);

		final CryptoPrimitivesSignature electionEventContextPayloadSignature = getPayloadSignature(electionEventContextPayload,
				additionalContextData);
		electionEventContextPayload.setSignature(electionEventContextPayloadSignature);

		return electionEventContextPayload;
	}

	private CryptoPrimitivesSignature getPayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {

		final ImmutableByteArray signature;
		try {
			signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Failed to generate payload signature. [name: %s]", payload.getClass().getName()));
		}

		return new CryptoPrimitivesSignature(signature);
	}
}
