/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {
	Candidate,
	Election,
	ElectionInformation,
	ElectionTexts,
	Eligibility,
	EmptyList,
	EmptyPosition,
	List,
	TranslatableText,
	WriteInPosition,
} from 'e-voting-libraries-ui-kit';
import {RandomArray, RandomInt} from './random';

let electionGroupPosition = 0;
let electionInformationPosition = 0;
let electionPosition = 0;
let emptyListPosition = 0;
let emptyPositionPosition = 0;
let listPosition = 0;
let candidatePosition = 0;
let writeInPositionPosition = 0;

export const MockElectionTexts = (): ElectionTexts => {
	electionGroupPosition++;
	return {
		electionGroupIdentification: `electionGroupIdentification-${electionGroupPosition}`,
		domainOfInfluence: `domainOfInfluence-${electionGroupPosition}`,
		electionGroupPosition: electionGroupPosition,
		electionsInformation: [],
	};
};

export const MockElectionInformation = (
	options: {
		writeInsAllowed?: boolean;
		hasLists?: boolean;
		minimumCandidateAccumulation?: number;
	} = {},
): ElectionInformation => {
	electionInformationPosition++;

	const emptyList = MockEmptyList();

	let writeInPositions: WriteInPosition[] = [];
	let implicitWriteInCandidates: string[] = [];
	if (options.writeInsAllowed) {
		writeInPositionPosition = 0;
		writeInPositions = emptyList.emptyPositions.map((_) =>
			MockWriteInPosition(),
		);
		implicitWriteInCandidates = ['Banana Joe'];
	}

	return {
		election: MockElection(options),
		candidates: RandomArray(() => MockCandidate(), 100, 2),
		lists: options.hasLists ? RandomArray(() => MockList()) : [],
		emptyList,
		writeInPositions: writeInPositions,
		implicitWriteInCandidates,
	};
};

export const MockElection = (options: {
	writeInsAllowed?: boolean;
	minimumCandidateAccumulation?: number;
}): Election => {
	electionPosition++;
	return {
		electionIdentification: `electionIdentification-${electionPosition}`,
		electionDescription: MockTranslatableText({
			key: 'electionDescription',
			position: electionPosition,
		}),
		electionPosition: electionPosition,
		numberOfMandates: RandomInt(5),
		writeInsAllowed: options.writeInsAllowed ?? false,
		candidateAccumulation: RandomInt(
			10,
			options.minimumCandidateAccumulation ?? 1,
		),
		minimalCandidateSelectionInList: RandomInt(5),
	};
};

export const MockEmptyList = (
	props: {
		listDescription?: string;
	} = {},
): EmptyList => {
	emptyListPosition++;
	return {
		listIdentification: `listIdentification-${emptyListPosition}`,
		listDescription: props.listDescription
			? MockTranslatableText({value: props.listDescription})
			: MockTranslatableText({
				key: 'listDescription',
				position: emptyListPosition,
			}),
		emptyPositions: RandomArray(MockEmptyPosition, 100, 2),
	};
};

export const MockEmptyPosition = (): EmptyPosition => {
	emptyPositionPosition++;
	return {
		emptyPositionIdentification: `emptyPositionIdentification-${emptyPositionPosition}`,
		positionOnList: emptyPositionPosition,
		emptyPositionText: MockTranslatableText({
			key: 'emptyPositionText',
			position: emptyPositionPosition,
		}),
	};
};

export const MockList = (
	props: {
		displayListLine1?: string;
	} = {},
): List => {
	listPosition++;
	return {
		listIdentification: `listIdentification-${listPosition}`,
		listIndentureNumber: `listIndentureNumber-${listPosition}`,
		listOrderOfPrecedence: listPosition,
		candidatePositions: [],
		displayListLine1: props.displayListLine1
			? MockTranslatableText({value: props.displayListLine1})
			: MockTranslatableText({
				key: 'displayListLine1',
				position: listPosition,
			}),
		listDescription: MockTranslatableText({
			key: 'listDescription',
			position: listPosition,
		}),
	};
};

export const MockCandidate = (
	props: {
		eligibility?: Eligibility;
		displayCandidateLine1?: string;
		displayCandidateLine2?: string;
		displayCandidateLine3?: string;
		displayCandidateLine4?: string;
	} = {},
): Candidate => {
	candidatePosition++;

	const candidate: Candidate = {
		candidateIdentification: `candidateIdentification-${candidatePosition}`,
		eligibility: props.eligibility ?? Eligibility.IMPLICIT,
		displayCandidateLine1: props.displayCandidateLine1
			? MockTranslatableText({value: props.displayCandidateLine1})
			: MockTranslatableText({
				key: 'displayCandidateLine1',
				position: candidatePosition,
			}),
		displayCandidateLine2: props.displayCandidateLine2
			? MockTranslatableText({value: props.displayCandidateLine2})
			: MockTranslatableText({
				key: 'displayCandidateLine2',
				position: candidatePosition,
			}),
	};

	if (props.displayCandidateLine3) {
		candidate.displayCandidateLine3 = MockTranslatableText({
			value: props.displayCandidateLine3,
		});
	}

	if (props.displayCandidateLine4) {
		candidate.displayCandidateLine4 = MockTranslatableText({
			value: props.displayCandidateLine4,
		});
	}

	return candidate;
};

export const MockWriteInPosition = (): WriteInPosition => {
	writeInPositionPosition++;
	return {
		writeInPositionIdentification: `writeInPositionIdentification-${writeInPositionPosition}`,
		position: writeInPositionPosition,
		returnCodeWriteInDescription: MockTranslatableText({
			key: 'returnCodeWriteInDescription',
			position: writeInPositionPosition,
		}),
	};
};

function MockTranslatableText(
	props: { key: string; position: number } | { value: string },
): TranslatableText {
	return {
		DE: 'value' in props ? props.value : `${props.key}-DE-${props.position}`,
		FR: 'value' in props ? props.value : `${props.key}-FR-${props.position}`,
		IT: 'value' in props ? props.value : `${props.key}-IT-${props.position}`,
		RM: 'value' in props ? props.value : `${props.key}-RM-${props.position}`,
	};
}
