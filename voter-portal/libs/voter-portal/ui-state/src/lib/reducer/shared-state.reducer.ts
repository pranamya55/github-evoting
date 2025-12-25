/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Action, createReducer, on } from '@ngrx/store';
import { ExtendedFactor, SharedState } from '@vp/voter-portal-util-types';
import {
	ChooseActions,
	InitializationActions,
	LanguageSelectorActions,
	LegalTermsActions,
	ReviewActions,
	SharedActions,
	StartVotingActions,
	VerifyActions,
} from '@vp/voter-portal-ui-state';

export const initialState: SharedState = {
	// set initial required properties
	hasAcceptedLegalTerms: false,
	hasSubmittedAnswer: false,
	isAuthenticated: false,
	loading: false,
	error: null,
	config: {
		electionEventId: '',
		identification: ExtendedFactor.DateOfBirth,
		contestsCapabilities: {
			writeIns: false,
		},
		requestTimeout: {
			authenticateVoter: 30000,
			sendVote: 30000,
			confirmVote: 30000,
		},
	},
	currentLanguage: '',
	voteSentButNotCastInPreviousSession: false,
	voteCastInPreviousSession: false,
};

const stateReducer = createReducer(
	initialState,
	on(InitializationActions.initializationLoaded, (state, { config }) => ({
		...initialState,
		currentLanguage: state.currentLanguage,
		config,
	})),
	on(InitializationActions.initializationFailed, (state, { error }) => ({
		...state,
		error,
		loading: false,
	})),
	on(LanguageSelectorActions.languageSelected, (state, { lang }) => ({
		...state,
		currentLanguage: lang,
	})),
	on(LegalTermsActions.agreeClicked, (state) => ({
		...state,
		hasAcceptedLegalTerms: true,
	})),
	on(StartVotingActions.startClicked, (state) => ({
		...state,
		isAuthenticated: false,
		votesTexts: null,
		electionsTexts: null,
		writeInAlphabet: null,
		shortChoiceReturnCodes: null,
		voteSentButNotCastInPreviousSession: false,
		voteCastInPreviousSession: false,
		error: null,
		confirmationKey: null,
		voteCastReturnCode: null,
		hasSubmittedAnswer: false,
		loading: true,
	})),
	// Authentication, verification card state is INITIAL
	on(
		StartVotingActions.textsLoaded,
		(state, { votesTexts, electionsTexts, writeInAlphabet }) => ({
			...state,
			votesTexts,
			electionsTexts,
			writeInAlphabet,
			isAuthenticated: true,
			voteAnswers: null,
			electionAnswers: null,
			error: null,
			loading: false,
		}),
	),
	// Authentication, verification card state is SENT
	on(
		StartVotingActions.shortChoiceReturnCodesLoaded,
		(
			state,
			{ shortChoiceReturnCodes, votesTexts, electionsTexts, writeInAlphabet },
		) => ({
			...state,
			votesTexts,
			electionsTexts,
			writeInAlphabet,
			isAuthenticated: true,
			shortChoiceReturnCodes,
			voteAnswers: null,
			electionAnswers: null,
			error: null,
			voteSentButNotCastInPreviousSession: true,
			loading: false,
		}),
	),
	// Authentication, verification card state is CONFIRMED
	on(
		StartVotingActions.voteCastReturnCodeLoaded,
		(state, { voteCastReturnCode }) => ({
			...state,
			voteCastReturnCode,
			isAuthenticated: true,
			voteCastInPreviousSession: true,
			error: null,
			loading: false,
		}),
	),
	on(StartVotingActions.authenticationFailed, (state, { error }) => ({
		...state,
		error,
		loading: false,
	})),
	on(ChooseActions.formSubmitted, (state) => ({
		...state,
		hasSubmittedAnswer: true,
	})),
	on(ChooseActions.reviewClicked, (state, { voterAnswers }) => ({
		...state,
		voteAnswers: voterAnswers.voteAnswers,
		electionAnswers: voterAnswers.electionAnswers,
	})),
	on(ReviewActions.sealVoteClicked, (state) => ({
		...state,
		shortChoiceReturnCodes: null,
		loading: true,
		error: null,
	})),
	on(ReviewActions.sealedVoteLoaded, (state, { shortChoiceReturnCodes }) => ({
		...state,
		shortChoiceReturnCodes,
		loading: false,
		error: null,
	})),
	on(ReviewActions.sealedVoteLoadFailed, (state, { error }) => ({
		...state,
		shortChoiceReturnCodes: null,
		loading: false,
		error: error,
	})),
	on(VerifyActions.castVoteClicked, (state, { confirmationKey }) => ({
		...state,
		confirmationKey,
		loading: true,
		error: null,
	})),
	on(VerifyActions.castVoteLoaded, (state, { voteCastReturnCode }) => ({
		...state,
		voteCastReturnCode,
		confirmationKey: null,
		votesTexts: null,
		electionsTexts: null,
		writeInAlphabet: null,
		shortChoiceReturnCodes: null,
		voteAnswers: null,
		electionAnswers: null,
		loading: false,
		error: null,
	})),
	on(VerifyActions.castVoteLoadFailed, (state, { error }) => ({
		...state,
		error,
		loading: false,
	})),
	on(SharedActions.loggedOut, (state) => ({
		...initialState,
		config: state.config,
		error: state.error,
		currentLanguage: state.currentLanguage,
		hasAcceptedLegalTerms: true,
	})),
	on(SharedActions.serverErrorCleared, (state) => ({
		...state,
		loading: false,
		error: null,
	})),
);

export function reducer(state: SharedState | undefined, action: Action) {
	return stateReducer(state, action);
}
