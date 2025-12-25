/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

public record VerifierDatasetManifest(String electionEventId, String electionVersion, VerifierExportType exportType) {

	public VerifierDatasetManifest {
		validateUUID(electionEventId);
		checkArgument(Arrays.asList(VerifierExportType.values()).contains(exportType), "Export type is not valid.");
	}

}
