/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.dataexchange;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.dataexchange.DataExchange;
import ch.post.it.evoting.securedatamanager.shared.process.dataexchange.ExportInfo;
import ch.post.it.evoting.securedatamanager.shared.process.dataexchange.ImportExportService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;

@Service
@ConditionalOnProperty("role.isTally")
public class DataExchangeService extends DataExchange {

	public DataExchangeService(
			final PathResolver pathResolver,
			final WorkflowStepRunner workflowStepRunner,
			final ImportExportService importExportService) {
		super(pathResolver, workflowStepRunner, importExportService);
	}

	@Override
	public void exportSDMData(final String electionEventId, final int exchangeIndex) {
		throw new UnsupportedOperationException(
				"The tally component does not support SDM data export. [electionEventId: %s, exchangeIndex: %s]".formatted(electionEventId,
						exchangeIndex));
	}

	@Override
	public ExportInfo getExportInfo(final int exchangeIndex) {
		throw new UnsupportedOperationException("The tally component does not support SDM data export. [exchangeIndex: %s]".formatted(exchangeIndex));
	}
}
