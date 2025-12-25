/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Candidate, ChosenCandidate } from 'e-voting-libraries-ui-kit';

export function getAccumulation(
	candidate: Candidate,
	chosenCandidates: ChosenCandidate[],
) {
	return chosenCandidates.reduce(
		(currentCandidateAccumulation, chosenCandidate) => {
			if (
				chosenCandidate.candidateIdentification ===
				candidate.candidateIdentification
			)
				currentCandidateAccumulation++;
			return currentCandidateAccumulation;
		},
		0,
	);
}
