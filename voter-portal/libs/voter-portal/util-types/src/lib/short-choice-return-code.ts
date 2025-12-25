/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
export type ShortChoiceReturnCode =
	| VoteShortChoiceReturnCode
	| ElectionShortChoiceReturnCode;
export type ElectionShortChoiceReturnCode =
	| ListShortChoiceReturnCode
	| CandidateShortChoiceReturnCode;

export interface VoteShortChoiceReturnCode {
	questionIdentification: string;
	shortChoiceReturnCode: string;
}

export interface ListShortChoiceReturnCode {
	electionIdentification: string;
	shortChoiceReturnCode: string;
}

export interface CandidateShortChoiceReturnCode {
	electionIdentification: string;
	position: number;
	shortChoiceReturnCode: string;
}
