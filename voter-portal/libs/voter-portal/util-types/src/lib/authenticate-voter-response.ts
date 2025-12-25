/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ElectionTexts, VoteTexts } from 'e-voting-libraries-ui-kit';
import { ShortChoiceReturnCode } from './short-choice-return-code';

export type AuthenticateVoterResponse =
	| AuthenticateVoterResponseForUnsentVote
	| AuthenticateVoterResponseForSentVote
	| AuthenticateVoterResponseForCastVote;

export interface AuthenticateVoterResponseForUnsentVote
	extends AuthenticateVoterResponseBase {
	votesTexts: VoteTexts[];
	electionsTexts: ElectionTexts[];
	writeInAlphabet: string;
}

export interface AuthenticateVoterResponseForSentVote
	extends AuthenticateVoterResponseBase {
	votesTexts: VoteTexts[];
	electionsTexts: ElectionTexts[];
	writeInAlphabet: string;
	shortChoiceReturnCodes: ShortChoiceReturnCode[];
}

export interface AuthenticateVoterResponseForCastVote
	extends AuthenticateVoterResponseBase {
	voteCastReturnCode: string;
}

interface AuthenticateVoterResponseBase {
	verificationCardState: VerificationCardState;
}

export enum VerificationCardState {
	INITIAL = 'INITIAL',
	SENT = 'SENT',
	CONFIRMED = 'CONFIRMED',
}
