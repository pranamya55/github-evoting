/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.dataexchange;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.dataexchange.DataExchange;
import ch.post.it.evoting.securedatamanager.shared.process.dataexchange.ImportExportService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;

@Service
@ConditionalOnProperty("role.isSetup")
public class DataExchangeService extends DataExchange {

	public DataExchangeService(
			final PathResolver pathResolver,
			final WorkflowStepRunner workflowStepRunner,
			final ImportExportService importExportService) {
		super(pathResolver, workflowStepRunner, importExportService);
	}

}
