/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Candidate, Eligibility } from 'e-voting-libraries-ui-kit';
import { isEligible } from '@vp/voter-portal-util-helpers';

const candidate = {
	candidateIdentification: 'test-candidate-identification',
	eligibility: Eligibility.EXPLICIT,
} as Candidate;

const candidatesChosenInPrimaryElection = [
	{ candidateIdentification: candidate.candidateIdentification },
	{ candidateIdentification: null },
];

describe('isEligible', () => {
	it('should return true if candidatesChosenInPrimaryElection is undefined', () => {
		const result = isEligible(candidate, undefined);
		expect(result).toBe(true);
	});

	it('should return true if candidate eligibility is IMPLICIT', () => {
		const candidateWithImplicitEligibility = {
			...candidate,
			eligibility: Eligibility.IMPLICIT,
		};

		const result = isEligible(
			candidateWithImplicitEligibility,
			candidatesChosenInPrimaryElection,
		);
		expect(result).toBe(true);
	});

	it('should return true if candidate is in the chosen candidates list', () => {
		const result = isEligible(candidate, candidatesChosenInPrimaryElection);
		expect(result).toBe(true);
	});

	it('should return false if candidate is not in the chosen candidates list', () => {
		const otherCandidate = {
			candidateIdentification: 'test-other-candidate-identification',
			eligibility: Eligibility.EXPLICIT,
		} as Candidate;

		const result = isEligible(
			otherCandidate,
			candidatesChosenInPrimaryElection,
		);
		expect(result).toBe(false);
	});
});
