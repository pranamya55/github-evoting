/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {
	AnswerType,
	Ballot,
	BallotQuestion,
	StandardAnswer,
	StandardBallot,
	StandardQuestion,
	TieBreakAnswer,
	TieBreakQuestion,
	VariantBallot,
	VoteTexts
} from "e-voting-libraries-ui-kit";
import {validateNonBlankUCS, validateTranslatableText, validateXsToken} from "./validations";

/**
 * Validates the input vote texts.
 *
 * @param {VoteTexts} toValidate - the vote texts to validate.
 *
 * @returns {VoteTexts} - the validated vote texts.
 */
export function validateVoteTexts(toValidate: VoteTexts): VoteTexts {
	checkNotNull(toValidate);
	validateXsToken(toValidate.voteIdentification);
	validateXsToken(toValidate.domainOfInfluence);
	checkArgument(toValidate.votePosition >= 0, "The vote position must be positive.");
	validateTranslatableText(toValidate.voteDescription);
	checkNotNull(toValidate.ballots).forEach(ballot => validateBallot(ballot));
	checkArgument(toValidate.ballots.length > 0, "There must be at least one ballot.");
	return toValidate;
}

function validateBallot(ballot: Ballot): Ballot {
	checkNotNull(ballot);
	validateXsToken(ballot.ballotIdentification);
	checkArgument(ballot.ballotPosition >= 0, "The ballot position must be positive.");
	checkArgument(isStandardBallot(ballot) || isVariantBallot(ballot), "The ballot must be either a standard or a variant ballot.");
	isStandardBallot(ballot) ? validateStandardBallot(ballot) : validateVariantBallot(ballot);
	return ballot;
}

function validateStandardBallot(standardBallot: StandardBallot): StandardBallot {
	checkNotNull(standardBallot);
	validateXsToken(standardBallot.questionIdentification);
	validateNonBlankUCS(standardBallot.questionNumber);
	validateBallotQuestion(standardBallot.ballotQuestion);
	checkNotNull(standardBallot.answers).forEach(standardAnswer => validateStandardAnswer(standardAnswer));
	checkArgument(standardBallot.answers.length > 0, "There must be at least one answer.");
	return standardBallot;
}

function validateVariantBallot(variantBallot: VariantBallot): VariantBallot {
	checkNotNull(variantBallot);
	checkNotNull(variantBallot.standardQuestions).forEach(standardQuestion => validateStandardQuestion(standardQuestion));
	checkNotNull(variantBallot.tieBreakQuestions).forEach(tieBreakQuestion => validateTieBreakQuestion(tieBreakQuestion));
	checkArgument(variantBallot.standardQuestions.length >= 2, "There must be at least two standard questions.");
	return variantBallot;
}

function validateStandardQuestion(standardQuestion: StandardQuestion): StandardQuestion {
	checkNotNull(standardQuestion);
	validateXsToken(standardQuestion.questionIdentification);
	checkArgument(standardQuestion.questionPosition >= 0, "The question position must be positive.");
	validateNonBlankUCS(standardQuestion.questionNumber);
	validateBallotQuestion(standardQuestion.ballotQuestion);
	checkNotNull(standardQuestion.answers).forEach(standardAnswer => validateStandardAnswer(standardAnswer));
	checkArgument(standardQuestion.answers.length > 0, "There must be at least one answer.");
	return standardQuestion;
}

function validateTieBreakQuestion(tieBreakQuestion: TieBreakQuestion): TieBreakQuestion {
	checkNotNull(tieBreakQuestion);
	validateXsToken(tieBreakQuestion.questionIdentification);
	checkArgument(tieBreakQuestion.questionPosition >= 0, "The question position must be positive.");
	validateNonBlankUCS(tieBreakQuestion.questionNumber);
	validateBallotQuestion(tieBreakQuestion.ballotQuestion);
	checkNotNull(tieBreakQuestion.answers).forEach(tieBreakAnswer => validateTieBreakAnswer(tieBreakAnswer));
	checkArgument(tieBreakQuestion.answers.length > 0, "There must be at least one answer.");
	return tieBreakQuestion;
}

function validateBallotQuestion(ballotQuestion: BallotQuestion): BallotQuestion {
	checkNotNull(ballotQuestion);
	validateTranslatableText(ballotQuestion.question);
	return ballotQuestion;
}

function validateStandardAnswer(standardAnswer: StandardAnswer): StandardAnswer {
	checkNotNull(standardAnswer);
	validateXsToken(standardAnswer.answerIdentification);
	checkArgument(standardAnswer.answerPosition >= 0, "The answer position must be positive.");
	checkArgument(standardAnswer.answerType as AnswerType !== undefined, "The answer type must be valid.");
	validateTranslatableText(standardAnswer.answerInformation);
	return standardAnswer;
}

function validateTieBreakAnswer(tieBreakAnswer: TieBreakAnswer): TieBreakAnswer {
	checkNotNull(tieBreakAnswer);
	validateXsToken(tieBreakAnswer.answerIdentification);
	checkArgument(tieBreakAnswer.answerPosition >= 0, "The answer position must be positive.");
	validateTranslatableText(tieBreakAnswer.answerInformation);
	return tieBreakAnswer;
}

function isStandardBallot(ballot: Ballot): ballot is StandardBallot {
	return !!(ballot as StandardBallot).questionIdentification;
}

function isVariantBallot(ballot: Ballot): ballot is VariantBallot {
	return !!(ballot as VariantBallot).standardQuestions;
}