/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;

public record CandidatePositionSummary(String candidateIdentification) {

	public CandidatePositionSummary {
		validateXsToken(candidateIdentification);
	}
}
