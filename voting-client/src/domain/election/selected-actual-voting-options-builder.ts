/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ChosenAnswer, ChosenCandidate, ElectionAnswers, ElectionInformationAnswers, VoteAnswers, VoterAnswers} from "e-voting-libraries-ui-kit";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {ACTUAL_VOTING_OPTION_DELIMITER} from "../voting-options-constants";
import {PrimesMappingTable} from "./primes-mapping-table";
import {checkArgument} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {
	getBlankCorrectnessInformation
} from "../../protocol/preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";
import {getCorrectnessInformation} from "../../protocol/preliminaries/electoral-model/primes-mapping-table/get-correctness-information.algorithm";

/**
 * Gets and validates the selected actual voting options from the voter's answers.
 *
 * @param {VoterAnswers} voterAnswers - the voter's answers.
 * @param {PrimesMappingTable} primesMappingTable - the primes mapping table.
 *
 * @returns {ImmutableArray<string>} - the list of selected actual voting options.
 */
export function getSelectedActualVotingOptions(voterAnswers: VoterAnswers, primesMappingTable: PrimesMappingTable): ImmutableArray<string> {

	const selectedActualVotingOptions: Record<number, ImmutableArray<string>> = {
		...getSelectedActualVotingOptionsAnswers(voterAnswers),
		...getSelectedActualVotingOptionsCandidatesAndLists(voterAnswers)
	};

	const sortedSelectedActualVotingOptions: ImmutableArray<string> = ImmutableArray.from(
		[...Object.keys(selectedActualVotingOptions)]
			.sort((a, b) => a.localeCompare(b))
			.flatMap(position => selectedActualVotingOptions[position].elements())
	);

	checkArgument(JSON.stringify(getBlankCorrectnessInformation(primesMappingTable)) ===
		JSON.stringify(getCorrectnessInformation(primesMappingTable, sortedSelectedActualVotingOptions)),
		"The sorted selected actual voting options contain an invalid combination of actual voting options.");

	return sortedSelectedActualVotingOptions;
}

/**
 * Gets the actual voting options of the chosen question's answers from the voter's answers.
 *
 * The actual voting option of an answer is represented as 'question identification | answer identification'.
 *
 * @param {VoterAnswers} voterAnswers - the voter's answers.
 *
 * @returns {Record<number, ImmutableArray<string>>} - the list of actual voting options of the chosen question's answers by vote position.
 */
function getSelectedActualVotingOptionsAnswers(voterAnswers: VoterAnswers): Record<number, ImmutableArray<string>> {
	const selectedActualVotingOptions: Record<number, ImmutableArray<string>> = {};

	voterAnswers.voteAnswers.forEach((voteAnswer: VoteAnswers) =>
		selectedActualVotingOptions[voteAnswer.votePosition] = ImmutableArray.from(voteAnswer.chosenAnswers
			.map((chosenAnswer: ChosenAnswer) =>
				chosenAnswer.questionIdentification
				+ ACTUAL_VOTING_OPTION_DELIMITER
				+ chosenAnswer.answerIdentification
			)));

	return selectedActualVotingOptions;
}

/**
 * Gets the actual voting options of the chosen candidates and lists from the voter's answers.
 *
 * @param {VoterAnswers} voterAnswers - the voter's answers.
 *
 * @returns {Record<number, ImmutableArray<string>>} - the list of actual voting options of the chosen election's candidates and lists by election group position.
 */
function getSelectedActualVotingOptionsCandidatesAndLists(voterAnswers: VoterAnswers): Record<number, ImmutableArray<string>> {
	const selectedActualVotingOptions: Record<number, ImmutableArray<string>> = {};

	voterAnswers.electionAnswers.forEach((electionAnswer: ElectionAnswers) =>
		selectedActualVotingOptions[electionAnswer.electionGroupPosition] = ImmutableArray.from(electionAnswer.electionsInformation
			.flatMap((electionInformationAnswer: ElectionInformationAnswers) => {
				return electionInformationAnswer.chosenList
					? [
						getSelectedActualVotingOptionList(electionInformationAnswer),
						...getSelectedActualVotingOptionsCandidates(electionInformationAnswer)
					]
					: getSelectedActualVotingOptionsCandidates(electionInformationAnswer).elements();
			})));

	return selectedActualVotingOptions;
}

/**
 * Gets the actual voting options of the chosen candidates from the voter's answers.
 *
 * <ul>
 *     <li>The actual voting option of a write-in is represented as 'election identification | write-in candidate identification'.</li>
 *     <li>The actual voting options of a blank candidate is represented as 'election identification | blank candidate identification'.</li>
 *     <li>The actual voting option of a candidate is represented as 'election identification | candidate identification | candidate accumulation'.</li>
 * </ul>
 *
 * @param {ElectionInformationAnswers} electionInformationAnswer - the election information answer.
 *
 * @returns {string} - the actual voting options of the chosen candidates.
 */
function getSelectedActualVotingOptionsCandidates(electionInformationAnswer: ElectionInformationAnswers): ImmutableArray<string> {
	const candidateAccumulations: {} = {};

	return ImmutableArray.from(
		electionInformationAnswer.chosenCandidates.map((chosenCandidate: ChosenCandidate, index: number) => {
			const candidateChosen: boolean = chosenCandidate.candidateIdentification !== null;
			const writeInsAllowed: boolean = electionInformationAnswer.chosenWriteIns !== undefined;
			const writeInChosen: boolean = writeInsAllowed && electionInformationAnswer.chosenWriteIns[index].writeIn !== null;

			if (candidateChosen) {
				const candidateAccumulation: number = candidateAccumulations[chosenCandidate.candidateIdentification] ?? 0;
				const actualVotingOptionCandidate: string = electionInformationAnswer.electionIdentification
					+ ACTUAL_VOTING_OPTION_DELIMITER
					+ chosenCandidate.candidateIdentification
					+ ACTUAL_VOTING_OPTION_DELIMITER
					+ candidateAccumulation;
				candidateAccumulations[chosenCandidate.candidateIdentification] = candidateAccumulation + 1;
				return actualVotingOptionCandidate;
			} else if (writeInChosen) {
				return electionInformationAnswer.electionIdentification
					+ ACTUAL_VOTING_OPTION_DELIMITER
					+ electionInformationAnswer.chosenWriteIns[index].writeInPositionIdentification;
			} else {
				return electionInformationAnswer.electionIdentification
					+ ACTUAL_VOTING_OPTION_DELIMITER
					+ electionInformationAnswer.emptyListIds.emptyPositionIds[index].emptyPositionIdentification;
			}
		})
	);
}

/**
 * Gets the actual voting option of the chosen list from the voter's answers.
 *
 * The actual voting option of a list is represented as 'election identification | list identification'.
 *
 * @param {ElectionInformationAnswers} electionInformationAnswer - the election information answer.
 *
 * @returns {string} - the actual voting option of the chosen list.
 */
function getSelectedActualVotingOptionList(electionInformationAnswer: ElectionInformationAnswers): string {
	if (electionInformationAnswer.chosenList) {
		return electionInformationAnswer.electionIdentification
			+ ACTUAL_VOTING_OPTION_DELIMITER
			+ (electionInformationAnswer.chosenList.listIdentification ?? electionInformationAnswer.emptyListIds.listIdentification);
	}
	return '';
}
