/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

/**
 * Precompute context containing the election event identifier, the ballot box identifier and the verification card set identifier.
 */
public record PrecomputeContext(String electionEventId, String ballotBoxId, String verificationCardSetId) {

	public PrecomputeContext {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		validateUUID(verificationCardSetId);
	}
}
