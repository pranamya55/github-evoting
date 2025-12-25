/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.dataexchange;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

public record ImportExportManifest(String electionEventId, String electionEventSeed, int exchangeIndex) {

	public ImportExportManifest {
		validateUUID(electionEventId);
		validateSeed(electionEventSeed);
		checkArgument(exchangeIndex >= 0, "Exchange index must be positive.");
	}

}
