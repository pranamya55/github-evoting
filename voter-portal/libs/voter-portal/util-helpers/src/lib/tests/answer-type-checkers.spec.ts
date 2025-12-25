/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AnswerType,
	StandardAnswer,
	TieBreakAnswer,
} from 'e-voting-libraries-ui-kit';
import { isEmptyAnswer } from '@vp/voter-portal-util-helpers';

const nonEmptyStandardAnswer = {
	answerType: AnswerType.YES,
} as StandardAnswer;

const emptyStandardAnswer = {
	answerType: AnswerType.EMPTY,
} as StandardAnswer;

const nonEmptyTieBreakAnswer = {} as TieBreakAnswer;

const emptyTieBreakAnswer = {
	hiddenAnswer: true,
} as TieBreakAnswer;

describe('isEmptyAnswer', () => {
	it('should identify an empty standard answer correctly', () => {
		expect(isEmptyAnswer(emptyStandardAnswer)).toBe(true);
		expect(isEmptyAnswer(nonEmptyStandardAnswer)).toBe(false);
	});

	it('should identify an empty tie-break answer correctly', () => {
		expect(isEmptyAnswer(emptyTieBreakAnswer)).toBe(true);
		expect(isEmptyAnswer(nonEmptyTieBreakAnswer)).toBe(false);
	});
});
