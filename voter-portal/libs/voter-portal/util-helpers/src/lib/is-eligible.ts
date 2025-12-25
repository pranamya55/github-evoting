/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	Candidate,
	ChosenCandidate,
	Eligibility,
} from 'e-voting-libraries-ui-kit';

export function isEligible(
	candidate: Candidate,
	candidatesChosenInPrimaryElection: Array<ChosenCandidate> | undefined,
) {
	if (
		!candidatesChosenInPrimaryElection ||
		candidate.eligibility !== Eligibility.EXPLICIT
	) {
		return true;
	}

	return candidatesChosenInPrimaryElection.some((chosenCandidate) => {
		return (
			chosenCandidate.candidateIdentification ===
			candidate.candidateIdentification
		);
	});
}
