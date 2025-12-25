/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.configuration.VoterReturnCodes;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.VotingCardList;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService;
import ch.post.it.evoting.securedatamanager.setup.process.VoterReturnCodesPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Service
@ConditionalOnProperty("role.isSetup")
public class EvotingPrintService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvotingPrintService.class);

	private final PathResolver pathResolver;
	private final EvotingConfigService evotingConfigService;
	private final EvotingPrintFileRepository evotingPrintFileRepository;
	private final VoterReturnCodesPayloadService voterReturnCodesPayloadService;
	private final VoterInitialCodesPayloadService voterInitialCodesPayloadService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	public EvotingPrintService(
			final PathResolver pathResolver,
			final EvotingConfigService evotingConfigService,
			final EvotingPrintFileRepository evotingPrintFileRepository,
			final VoterReturnCodesPayloadService voterReturnCodesPayloadService,
			final VoterInitialCodesPayloadService voterInitialCodesPayloadService,
			final ElectionEventContextPayloadService electionEventContextPayloadService) {
		this.pathResolver = pathResolver;
		this.evotingConfigService = evotingConfigService;
		this.evotingPrintFileRepository = evotingPrintFileRepository;
		this.voterReturnCodesPayloadService = voterReturnCodesPayloadService;
		this.voterInitialCodesPayloadService = voterInitialCodesPayloadService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * Generates and persists the setup component evoting print file.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public void generate(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Generating setup component evoting print... [electionEventId: {}]", electionEventId);

		// Gathering all data to generate evoting print file.
		final Configuration configuration = evotingConfigService.load();

		final ImmutableMap<String, VoterInitialCodesPayloadService.VoterInitialCodesByVcs> voterInitialCodesMap = voterInitialCodesPayloadService.loadVoterInitialCodesMap(
				electionEventId);
		final ImmutableMap<String, VoterReturnCodes> voterReturnCodesMap = voterReturnCodesPayloadService.loadVoterReturnCodesMap(electionEventId);
		checkState(voterInitialCodesMap.size() == voterReturnCodesMap.size(),
				"The voter initial codes and return codes map must have the same size. [voterInitialCodesMap: %s, voterReturnCodesMap: %s]",
				voterInitialCodesMap.size(), voterReturnCodesMap.size());

		final ImmutableMap<String, PrimesMappingTable> primesMappingTableMap = electionEventContextPayloadService.loadAllPrimesMappingTables(
				electionEventId);

		// Map data to the voting card list.
		final VotingCardList votingCardList = VotingCardListMapper.toVotingCardList(configuration, voterInitialCodesMap, voterReturnCodesMap,
				primesMappingTableMap);

		evotingPrintFileRepository.save(electionEventId, votingCardList);
		LOGGER.info("Setup component evoting print file successfully generated. [electionEventId: {}]", electionEventId);

	}

	/**
	 * Gets the print information corresponding to the evoting print file.
	 *
	 * @return the print information.
	 */
	public PrintInfo getPrintInfo() {
		final String filename = evotingPrintFileRepository.getFilename();
		final Path filepath = pathResolver.resolvePrintingOutputPath();

		return new PrintInfo(filepath, filename);
	}
}
