/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {getSelectedActualVotingOptions} from "../../../src/domain/election/selected-actual-voting-options-builder";
import {Ballot, ElectionAnswers, ElectionTexts, StandardBallot, VoteAnswers, VoterAnswers, VoteTexts} from "e-voting-libraries-ui-kit";
import {PrimesMappingTable} from "../../../src/domain/election/primes-mapping-table";
import {parsePrimitivesParams} from "../../../src/domain/primitives-params-parser";
import {
	getBlankCorrectnessInformation
} from "../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";
import {
	getCorrectnessInformation
} from "../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-correctness-information.algorithm";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

import authenticateVoterResponseJson from "../../tools/data/authenticate-voter-response.json";
import voterAnswersJson from "../../tools/data/voter-answers.json";

describe('Selected actual voting options builder', function (): void {
	const voterAnswers: VoterAnswers = {
		voteAnswers: createVoteAnswers(),
		electionAnswers: createElectionAnswers()
	};

	test('with valid voter answers file should not throw', function (): void {
		const primesMappingTable: PrimesMappingTable = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		).primesMappingTable;

		expect(() => getSelectedActualVotingOptions(voterAnswersJson, primesMappingTable)).not.toThrow();
	});

	test('with valid voter answers should build valid actual voting options', function (): void {
		const primesMappingTable: PrimesMappingTable = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		).primesMappingTable;

		expect(() => getSelectedActualVotingOptions(voterAnswers, primesMappingTable)).not.toThrow();
	});

	test('with wrong order voter answers should build sorted actual voting options', function (): void {
		const primesMappingTable: PrimesMappingTable = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		).primesMappingTable;

		const wrongOrderVoterAnswers: VoterAnswers = {
			voteAnswers: createVoteAnswers().reverse(),
			electionAnswers: createElectionAnswers().reverse()
		}

		const selectedActualVotingOptions: ImmutableArray<string> = getSelectedActualVotingOptions(wrongOrderVoterAnswers, primesMappingTable);
		expect(JSON.stringify(getCorrectnessInformation(primesMappingTable, selectedActualVotingOptions)))
			.toStrictEqual(JSON.stringify(getBlankCorrectnessInformation(primesMappingTable)))
	});

	function createVoteAnswers(): VoteAnswers[] {
		return (authenticateVoterResponseJson.voterMaterial.votesTexts as VoteTexts[]).map(voteText => ({
			voteIdentification: voteText.voteIdentification,
			votePosition: voteText.votePosition,
			chosenAnswers: voteText.ballots.flatMap(ballot => {
				if (isStandardBallot(ballot)) {
					// First answer chosen for all standard ballots
					return [{
						ballotIdentification: ballot.ballotIdentification,
						questionIdentification: ballot.questionIdentification,
						answerIdentification: ballot.answers[0].answerIdentification
					}];
				} else {
					// Second answer chosen for all standard questions of the variant ballots
					const standardQuestionsChosenAnswers = ballot.standardQuestions.flatMap(question => ({
						ballotIdentification: ballot.ballotIdentification,
						questionIdentification: question.questionIdentification,
						answerIdentification: question.answers[1].answerIdentification
					}));
					// First answer chosen for all tie-break questions of the variant ballots
					const tieBreakQuestionsChosenAnswers = ballot.tieBreakQuestions?.flatMap(question => ({
						ballotIdentification: ballot.ballotIdentification,
						questionIdentification: question.questionIdentification,
						answerIdentification: question.answers[0].answerIdentification
					}));
					return [...standardQuestionsChosenAnswers, ...tieBreakQuestionsChosenAnswers];
				}
			})
		}));
	}

	function createElectionAnswers(): ElectionAnswers[] {
		return (authenticateVoterResponseJson.voterMaterial.electionsTexts as ElectionTexts[]).map(electionText => ({
			electionGroupIdentification: electionText.electionGroupIdentification,
			electionGroupPosition: electionText.electionGroupPosition,
			electionsInformation: electionText.electionsInformation.map((electionInformation, electionIndex) => ({
				electionIdentification: electionInformation.election.electionIdentification,
				// First candidate chosen for the second position of all elections
				chosenCandidates: electionInformation.emptyList.emptyPositions.map((_, candidateIndex) => ({
					candidateIdentification: candidateIndex === 1 ? electionInformation.candidates[candidateIndex].candidateIdentification : null,
				})),
				// First list chosen for the first election, empty list chosen for the rest
				chosenList: electionInformation.lists.length > 0 ? {
					listIdentification: electionIndex === 0 ? electionInformation.lists[0].listIdentification : electionInformation.emptyList.listIdentification
				} : undefined,
				emptyListIds: {
					listIdentification: electionInformation.emptyList.listIdentification,
					// Empty position chosen for the third and following positions of all elections
					emptyPositionIds: electionInformation.emptyList.emptyPositions.map(emptyPosition => ({
						emptyPositionIdentification: emptyPosition.emptyPositionIdentification
					}))
				},
				chosenWriteIns: electionInformation.election.writeInsAllowed ?
					// Write-in chosen for the first position of all elections that allow write-ins
					electionInformation.writeInPositions.map((writeInPosition, writeInIndex) => ({
							writeInPositionIdentification: writeInPosition.writeInPositionIdentification,
							writeIn: writeInIndex === 0 ? 'WriteIn' : null
						})
					) : undefined
			}))
		}));
	}
});

function isStandardBallot(ballot: Ballot): ballot is StandardBallot {
	return !!(ballot as StandardBallot).questionIdentification;
}
