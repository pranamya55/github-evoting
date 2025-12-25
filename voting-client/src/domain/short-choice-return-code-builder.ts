/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {PrimesMappingTable} from "./election/primes-mapping-table";
import {
	CandidateShortChoiceReturnCode,
	ElectionShortChoiceReturnCode,
	ListShortChoiceReturnCode,
	ShortChoiceReturnCode,
	VoteShortChoiceReturnCode
} from "./short-choice-return-code.types";
import {getBlankCorrectnessInformation} from "../protocol/preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";
import {CORRECTNESS_INFORMATION_DELIMITER, CORRECTNESS_INFORMATION_LIST_PREFIX} from "./voting-options-constants";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

/**
 * Gets the short choice return codes mapped to the corresponding identifications using the primes mapping table.
 *
 * @param {PrimesMappingTable} primesMappingTable - the primes mapping table.
 * @param {ImmutableArray<string>} shortChoiceReturnCodes - the short choice return codes.
 *
 * @returns {ShortChoiceReturnCode[]} - the list of short choice return codes.
 */
export function getShortChoiceReturnCodes(primesMappingTable: PrimesMappingTable, shortChoiceReturnCodes: ImmutableArray<string>): ShortChoiceReturnCode[] {
	const electionCandidatePositions: Map<string, number> = new Map();

	return getBlankCorrectnessInformation(primesMappingTable).elements().map((correctnessInformation, i) => {
		const isVoteCorrectnessInformation: boolean = correctnessInformation.split(CORRECTNESS_INFORMATION_DELIMITER).length === 1;
		const shortChoiceReturnCode: string = shortChoiceReturnCodes.get(i);
		return isVoteCorrectnessInformation
			? getVoteShortChoiceReturnCode(correctnessInformation, shortChoiceReturnCode)
			: getElectionShortChoiceReturnCode(correctnessInformation, shortChoiceReturnCode, electionCandidatePositions);
	});
}

/**
 * Gets the short choice return code related to a question identification.
 *
 * The correctness information of an answer of a referendum-style question is represented as 'question identification'.
 *
 * @param {string} correctnessInformation - the correctness information.
 * @param {string} shortChoiceReturnCode - the short choice return code.
 *
 * @returns {VoteShortChoiceReturnCode} - the vote short choice return code.
 */
function getVoteShortChoiceReturnCode(correctnessInformation: string, shortChoiceReturnCode: string): VoteShortChoiceReturnCode {
	return {
		questionIdentification: correctnessInformation,
		shortChoiceReturnCode: shortChoiceReturnCode
	}
}

/**
 * Gets the short choice return code related to an election identification.
 *
 * <ul>
 *     <li>The correctness information of a list in an election (or the empty list) is represented as 'L | election identification'.</li>
 *     <li>The correctness information of a candidate, empty or write-in position in an election is represented as 'C | election identification'.</li>
 * </ul>
 *
 * @param {string} correctnessInformation - the correctness information.
 * @param {string} shortChoiceReturnCode - the short choice return code.
 * @param {Map<string, number>} electionCandidatePositions - the map of election identification to candidate position.
 *
 * @returns {ElectionShortChoiceReturnCode} - the election short choice return code.
 */
function getElectionShortChoiceReturnCode(correctnessInformation: string, shortChoiceReturnCode: string, electionCandidatePositions: Map<string, number>): ElectionShortChoiceReturnCode {
	const identifications: string[] = correctnessInformation.split(CORRECTNESS_INFORMATION_DELIMITER);
	const prefix: string = identifications[0];
	const electionIdentification: string = identifications[1];

	return prefix === CORRECTNESS_INFORMATION_LIST_PREFIX
		? getListShortChoiceReturnCode(electionIdentification, shortChoiceReturnCode)
		: getCandidateShortChoiceReturnCode(electionIdentification, shortChoiceReturnCode, electionCandidatePositions);
}

/**
 * Gets the {@link ListShortChoiceReturnCode}.
 *
 * @param {string} electionIdentification - the election identification.
 * @param {string} shortChoiceReturnCode - the short choice return code.
 *
 * @returns {ListShortChoiceReturnCode} - the list short choice return code.
 */
function getListShortChoiceReturnCode(electionIdentification: string, shortChoiceReturnCode: string): ListShortChoiceReturnCode {
	return {
		electionIdentification: electionIdentification,
		shortChoiceReturnCode: shortChoiceReturnCode
	}
}

/**
 * Gets the {@link CandidateShortChoiceReturnCode} of the given election candidate position and increments the election candidate position.
 *
 * @param {string} electionIdentification - the election identification.
 * @param {string} shortChoiceReturnCode - the short choice return code.
 * @param {Map<string, number>} electionCandidatePositions - the map of election identification to candidate position.
 *
 * @returns {CandidateShortChoiceReturnCode} - the candidate short choice return code.
 */
function getCandidateShortChoiceReturnCode(electionIdentification: string, shortChoiceReturnCode: string, electionCandidatePositions: Map<string, number>): CandidateShortChoiceReturnCode {
	if (!electionCandidatePositions.has(electionIdentification)) {
		electionCandidatePositions.set(electionIdentification, 1);
	}
	const candidateShortChoiceReturnCode: CandidateShortChoiceReturnCode = {
		electionIdentification: electionIdentification,
		position: electionCandidatePositions.get(electionIdentification),
		shortChoiceReturnCode: shortChoiceReturnCode
	}
	electionCandidatePositions.set(electionIdentification, electionCandidatePositions.get(electionIdentification) + 1);
	return candidateShortChoiceReturnCode;
}
