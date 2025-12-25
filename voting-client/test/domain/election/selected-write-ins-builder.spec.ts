/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ElectionAnswers, ElectionTexts, VoterAnswers} from "e-voting-libraries-ui-kit";
import {getSelectedWriteIns} from "../../../src/domain/election/selected-write-ins-builder";

import authenticateVoterResponseJson from "../../tools/data/authenticate-voter-response.json";
import voterAnswersJson from "../../tools/data/voter-answers.json";

describe('Selected write-ins builder', function (): void {

	test('with valid voter answers file should not throw', function (): void {
		expect(() => getSelectedWriteIns(voterAnswersJson)).not.toThrow();
	});

	test('with valid voter answers should build the correct list of write-ins', function (): void {
		const electionsTexts: ElectionTexts[] = authenticateVoterResponseJson.voterMaterial.electionsTexts as ElectionTexts[];
		const electionAnswers: ElectionAnswers[] = electionsTexts.map(electionText => ({
			electionGroupIdentification: electionText.electionGroupIdentification,
			electionGroupPosition: electionText.electionGroupPosition,
			electionsInformation: electionText.electionsInformation.map((electionInformation, electionIndex) => ({
				electionIdentification: electionInformation.election.electionIdentification,
				chosenCandidates: [],
				emptyListIds: {
					listIdentification: electionInformation.emptyList.listIdentification,
					emptyPositionIds: []
				},
				chosenWriteIns: electionInformation.election.writeInsAllowed ?
					// Write-in chosen for the first position of all elections that allow write-ins
					electionInformation.writeInPositions.map((writeInPosition, writeInIndex) => ({
							writeInPositionIdentification: writeInPosition.writeInPositionIdentification,
							writeIn: writeInIndex === 0 ? `WriteIn ${electionIndex}` : null
						})
					) : undefined
			}))
		}));

		const voterAnswers: VoterAnswers = {
			voteAnswers: [],
			electionAnswers
		};

		const expectedWriteIns: string[] = electionsTexts.flatMap(electionsTexts => electionsTexts.electionsInformation)
			.filter(electionInformation => electionInformation.election.writeInsAllowed)
			.map((_, electionIndex) => `WriteIn ${electionIndex}`);

		expect(getSelectedWriteIns(voterAnswers).elements()).toStrictEqual(expectedWriteIns);
	});

	test('with no selected write-ins returns an empty list', function (): void {
		const electionsTexts: ElectionTexts[] = authenticateVoterResponseJson.voterMaterial.electionsTexts as ElectionTexts[];
		const electionAnswers: ElectionAnswers[] = electionsTexts.map(electionText => ({
			electionGroupIdentification: electionText.electionGroupIdentification,
			electionGroupPosition: electionText.electionGroupPosition,
			electionsInformation: electionText.electionsInformation.map((electionInformation, electionIndex) => ({
				electionIdentification: electionInformation.election.electionIdentification,
				chosenCandidates: [],
				emptyListIds: {
					listIdentification: electionInformation.emptyList.listIdentification,
					emptyPositionIds: []
				},
				chosenWriteIns: electionInformation.election.writeInsAllowed ?
					// Write-in chosen for the first position of all elections that allow write-ins
					electionInformation.writeInPositions.map(writeInPosition => ({
							writeInPositionIdentification: writeInPosition.writeInPositionIdentification,
							writeIn: null
						})
					) : undefined
			}))
		}));

		const voterAnswers: VoterAnswers = {
			voteAnswers: [],
			electionAnswers
		};

		expect(getSelectedWriteIns(voterAnswers).length).toBe(0);
	});
});
