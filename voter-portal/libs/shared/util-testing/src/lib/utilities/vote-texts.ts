/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {
	AnswerType,
	StandardAnswer,
	StandardQuestion,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';

let votePosition = 0;
let questionPosition = 0;
let answerPosition = 0;

export const MockVoteTexts = (): VoteTexts => {
	votePosition++;
	return {
		voteIdentification: `voteIdentification-${votePosition}`,
		domainOfInfluence: `domainOfInfluence-${votePosition}`,
		votePosition: votePosition,
		voteDescription: {
			DE: `voteDescription-DE-${votePosition}`,
			FR: `voteDescription-FR-${votePosition}`,
			IT: `voteDescription-IT-${votePosition}`,
			RM: `voteDescription-RM-${votePosition}`,
		},
		ballots: [],
	};
};

export const MockStandardQuestion = (): StandardQuestion => {
	questionPosition++;
	return {
		questionPosition: questionPosition,
		questionIdentification: `questionIdentification-${questionPosition}`,
		questionNumber: `questionNumber-${questionPosition}`,
		ballotQuestion: {
			question: {
				DE: `question-DE-${questionPosition}`,
				FR: `question-FR-${questionPosition}`,
				IT: `question-IT-${questionPosition}`,
				RM: `question-RM-${questionPosition}`,
			},
		},
		answers: [],
	};
};

export const MockStandardAnswer = (
	props: {
		isEmptyAnswer?: boolean;
	} = {},
): StandardAnswer => {
	answerPosition++;
	return {
		answerType: props.isEmptyAnswer ? AnswerType.EMPTY : AnswerType.YES,
		answerIdentification: `answerIdentification-${answerPosition}`,
		answerPosition: answerPosition,
		hiddenAnswer: props.isEmptyAnswer ? true : false,
		answerInformation: {
			DE: `answerInformation-DE-${answerPosition}`,
			FR: `answerInformation-FR-${answerPosition}`,
			IT: `answerInformation-IT-${answerPosition}`,
			RM: `answerInformation-RM-${answerPosition}`,
		},
	};
};
