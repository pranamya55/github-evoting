/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import { Candidate } from 'e-voting-libraries-ui-kit';
import { getAccumulation } from '@vp/voter-portal-util-helpers';

const candidate = {
	candidateIdentification: 'test-candidate-identification',
} as Candidate;

describe('getAccumulation', () => {
	it('should return 0 when no candidates match', () => {
		const chosenCandidates = [
			{ candidateIdentification: 'test-other-candidate-identification' },
			{ candidateIdentification: null },
		];

		const result = getAccumulation(candidate, chosenCandidates);
		expect(result).toBe(0);
	});

	it('should return the number of candidates that match', () => {
		const chosenCandidates = [
			{ candidateIdentification: 'test-other-candidate-identification' },
			{ candidateIdentification: null },
			{ candidateIdentification: candidate.candidateIdentification },
			{ candidateIdentification: candidate.candidateIdentification },
			{ candidateIdentification: candidate.candidateIdentification },
		];

		const result = getAccumulation(candidate, chosenCandidates);
		expect(result).toBe(3);
	});
});
