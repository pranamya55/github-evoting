/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AnswerBase, AnswerType } from 'e-voting-libraries-ui-kit';

export function isEmptyAnswer<TAnswer extends AnswerBase>(
	answer: TAnswer,
): boolean {
	const isHiddenAnswer: boolean =
		'hiddenAnswer' in answer && answer.hiddenAnswer === true;
	const isEmptyAnswer: boolean =
		'answerType' in answer && answer.answerType === AnswerType.EMPTY;

	return isHiddenAnswer || isEmptyAnswer;
}
