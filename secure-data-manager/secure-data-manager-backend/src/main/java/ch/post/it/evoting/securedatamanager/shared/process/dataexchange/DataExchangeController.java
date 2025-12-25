/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.dataexchange;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

/**
 * The election event end-point.
 */
@RestController
@RequestMapping("/sdm-shared/data-exchange")
public class DataExchangeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataExchangeController.class);
	private final DataExchange dataExchange;
	private final ElectionEventService electionEventService;

	public DataExchangeController(
			final DataExchange dataExchange,
			final ElectionEventService electionEventService) {
		this.dataExchange = dataExchange;
		this.electionEventService = electionEventService;
	}

	@PostMapping("export")
	@ResponseStatus(value = HttpStatus.CREATED)
	public void exportSDMData(
			@RequestBody
			final int exchangeIndex
	) {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Starting SDM data export... [electionEventId: {}, exchangeIndex: {}]", electionEventId, exchangeIndex);

		dataExchange.exportSDMData(electionEventId, exchangeIndex);

		LOGGER.info("SDM data export has been successfully started. [electionEventId: {}, exchangeIndex: {}]", electionEventId, exchangeIndex);
	}

	@GetMapping("export")
	@ResponseStatus(value = HttpStatus.OK)
	public ExportInfo getExportInfo(
			@RequestParam(name = "exchangeIndex")
			final int exchangeIndex) {
		return dataExchange.getExportInfo(exchangeIndex);
	}

	@PostMapping("/import")
	public void importSDMData(
			@RequestParam("file")
			final MultipartFile zip,
			@RequestParam("exchangeIndex")
			final int exchangeIndex) {
		checkNotNull(zip);
		LOGGER.debug("Importing SDM data... [exchangeIndex: {}]", exchangeIndex);

		dataExchange.importSDMData(exchangeIndex, zip);

		LOGGER.info("SDM data import has been successfully started. [exchangeIndex: {}]", exchangeIndex);
	}
}
