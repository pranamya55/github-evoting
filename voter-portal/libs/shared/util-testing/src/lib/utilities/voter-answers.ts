/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ElectionInformationAnswers } from 'e-voting-libraries-ui-kit';

let electionPosition = 0;

export const MockElectionInformationAnswers =
	(): ElectionInformationAnswers => {
		electionPosition++;

		return {
			electionIdentification: `electionIdentification-${electionPosition}`,
			chosenCandidates: [],
			emptyListIds: {
				listIdentification: `listIdentification-${electionPosition}`,
				emptyPositionIds: [],
			},
		};
	};
