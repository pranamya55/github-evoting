/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { BackendError } from './backend-error';
import {
	ElectionAnswers,
	ElectionTexts,
	VoteAnswers,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import { ShortChoiceReturnCode } from './short-choice-return-code';
import { VoterPortalConfig } from './voter-portal-config';

export const SHARED_FEATURE_KEY = 'sharedState';

export interface SharedState {
	hasAcceptedLegalTerms: boolean;
	hasSubmittedAnswer: boolean;
	isAuthenticated: boolean;
	config: VoterPortalConfig;
	currentLanguage?: string;
	error?: BackendError | null; // last known error (if any)
	startVotingKey?: string;
	votesTexts?: VoteTexts[] | null;
	electionsTexts?: ElectionTexts[] | null;
	writeInAlphabet?: string | null;
	voteAnswers?: VoteAnswers[] | null;
	electionAnswers?: ElectionAnswers[] | null;
	shortChoiceReturnCodes?: ShortChoiceReturnCode[] | null;
	confirmationKey?: string | null;
	voteCastReturnCode?: string | null;
	loading: boolean;
	voteSentButNotCastInPreviousSession: boolean; //User sent vote in last session.
	voteCastInPreviousSession: boolean; //User cast vote in last session
}
