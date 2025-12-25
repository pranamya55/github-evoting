/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { createFeatureSelector, createSelector } from '@ngrx/store';
import {
	CandidateShortChoiceReturnCode,
	SHARED_FEATURE_KEY,
	SharedState,
	VoteShortChoiceReturnCode,
} from '@vp/voter-portal-util-types';
import { VoterAnswers } from 'e-voting-libraries-ui-kit';

export const getSharedState =
	createFeatureSelector<SharedState>(SHARED_FEATURE_KEY);

export const getLoading = createSelector(
	getSharedState,
	(state: SharedState) => state.loading,
);

export const getIsAuthenticated = createSelector(
	getSharedState,
	(state: SharedState) => state?.isAuthenticated,
);

export const getBackendError = createSelector(
	getSharedState,
	(state: SharedState) => state.error,
);

export const hasBackendError = createSelector(
	getSharedState,
	(state: SharedState) => !!state.error,
);

export const getVotesTexts = createSelector(
	getSharedState,
	(state: SharedState) => state.votesTexts ?? [],
);

export const getElectionsTexts = createSelector(
	getSharedState,
	(state: SharedState) => state.electionsTexts ?? [],
);

export const getVoteAnswers = createSelector(
	getSharedState,
	(state: SharedState) => state.voteAnswers ?? [],
);

export const getElectionAnswers = createSelector(
	getSharedState,
	(state: SharedState) => state.electionAnswers ?? [],
);

export const getAnswers = createSelector(
	getVoteAnswers,
	getElectionAnswers,
	(voteAnswers, electionAnswers) =>
		({
			voteAnswers: voteAnswers,
			electionAnswers: electionAnswers,
		}) as VoterAnswers,
);

const getVoteAnswer = (voteIdentification: string) =>
	createSelector(getVoteAnswers, (voteAnswers) =>
		voteAnswers.find(
			(voteAnswer) => voteAnswer.voteIdentification === voteIdentification,
		),
	);

export const getChosenAnswer = (props: {
	voteIdentification: string;
	ballotIdentification: string;
	questionIdentification: string;
}) =>
	createSelector(getVoteAnswer(props.voteIdentification), (voteAnswers) =>
		voteAnswers?.chosenAnswers.find(
			(chosenAnswer) =>
				chosenAnswer.ballotIdentification === props.ballotIdentification &&
				chosenAnswer.questionIdentification === props.questionIdentification,
		),
	);

const getElectionAnswer = (electionGroupIdentification: string) =>
	createSelector(getElectionAnswers, (electionAnswers) =>
		electionAnswers.find(
			(electionAnswer) =>
				electionAnswer.electionGroupIdentification ===
				electionGroupIdentification,
		),
	);

export const getElectionInformationAnswer = (props: {
	electionGroupIdentification: string;
	electionIdentification: string;
}) =>
	createSelector(
		getElectionAnswer(props.electionGroupIdentification),
		(electionAnswers) =>
			electionAnswers?.electionsInformation.find(
				(electionInformationAnswer) =>
					electionInformationAnswer.electionIdentification ===
					props.electionIdentification,
			),
	);

export const getConfig = createSelector(
	getSharedState,
	(state: SharedState) => state?.config,
);

export const getElectionEventId = createSelector(
	getSharedState,
	(state: SharedState) => state.config.electionEventId,
);

export const getCurrentLanguage = createSelector(
	getSharedState,
	(state: SharedState) => state?.currentLanguage,
);

export const getHasAcceptedLegalTerms = createSelector(
	getSharedState,
	(state: SharedState) => state?.hasAcceptedLegalTerms,
);

export const getHasSubmittedAnswer = createSelector(
	getSharedState,
	(state: SharedState) => state?.hasSubmittedAnswer,
);

export const getShortChoiceReturnCodes = createSelector(
	getSharedState,
	(state: SharedState) => state.shortChoiceReturnCodes,
);

export const getVoteShortChoiceReturnCode = (questionIdentification: string) =>
	createSelector(getShortChoiceReturnCodes, (shortChoiceReturnCodes) =>
		shortChoiceReturnCodes
			?.filter(
				(shortChoiceReturnCode) =>
					'questionIdentification' in shortChoiceReturnCode,
			)
			.map(
				(shortChoiceReturnCode) =>
					shortChoiceReturnCode as VoteShortChoiceReturnCode,
			)
			.find(
				(shortChoiceReturnCode) =>
					shortChoiceReturnCode.questionIdentification ===
					questionIdentification,
			),
	);

const getElectionShortChoiceReturnCodes = (electionIdentification: string) =>
	createSelector(getShortChoiceReturnCodes, (shortChoiceReturnCodes) =>
		shortChoiceReturnCodes?.filter(
			(shortChoiceReturnCode) =>
				'electionIdentification' in shortChoiceReturnCode &&
				shortChoiceReturnCode.electionIdentification === electionIdentification,
		),
	);

export const getListShortChoiceReturnCode = (electionIdentification: string) =>
	createSelector(
		getElectionShortChoiceReturnCodes(electionIdentification),
		(shortChoiceReturnCodes) =>
			shortChoiceReturnCodes?.find(
				(shortChoiceReturnCode) => !('position' in shortChoiceReturnCode),
			),
	);

export const getCandidateShortChoiceReturnCode = (
	electionIdentification: string,
	position: number,
) =>
	createSelector(
		getElectionShortChoiceReturnCodes(electionIdentification),
		(shortChoiceReturnCodes) =>
			shortChoiceReturnCodes
				?.filter((shortChoiceReturnCode) => 'position' in shortChoiceReturnCode)
				.map(
					(shortChoiceReturnCode) =>
						shortChoiceReturnCode as CandidateShortChoiceReturnCode,
				)
				.find(
					(shortChoiceReturnCode) =>
						shortChoiceReturnCode.position === position,
				),
	);

export const getVoteCastReturnCode = createSelector(
	getSharedState,
	(state: SharedState) => state.voteCastReturnCode,
);

export const getVoteSentButNotCastInPreviousSession = createSelector(
	getSharedState,
	(state: SharedState) => state.voteSentButNotCastInPreviousSession,
);

export const getVoteCastInPreviousSession = createSelector(
	getSharedState,
	(state: SharedState) => state.voteCastInPreviousSession,
);

export const getWriteInAlphabet = createSelector(
	getSharedState,
	(state: SharedState) => state.writeInAlphabet,
);
