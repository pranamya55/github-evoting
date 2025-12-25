/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
	BackendError,
	ShortChoiceReturnCode,
	Voter,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import {
	ElectionTexts,
	VoterAnswers,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';

export const LanguageSelectorActions = createActionGroup({
	source: 'Language Selector',
	events: {
		'Language Selected': props<{ lang: string }>(),
	},
});

export const InitializationActions = createActionGroup({
	source: 'Initialization Page',
	events: {
		'Initialization Loading': props<{ electionEventId: string }>(),
		'Initialization Loaded': props<{ config: VoterPortalConfig }>(),
		'Initialization Failed': props<{ error: BackendError }>(),
	},
});

export const LegalTermsActions = createActionGroup({
	source: 'Legal Terms Page',
	events: {
		'Agree Clicked': emptyProps(),
	},
});

export const StartVotingActions = createActionGroup({
	source: 'Start Voting Page',
	events: {
		'Start Clicked': props<{ voter: Voter }>(),
		'Texts Loaded': props<{
			votesTexts: VoteTexts[] | undefined;
			electionsTexts: ElectionTexts[] | undefined;
			writeInAlphabet: string;
		}>(),
		'Short Choice Return Codes Loaded': props<{
			shortChoiceReturnCodes: ShortChoiceReturnCode[];
			votesTexts: VoteTexts[] | undefined;
			electionsTexts: ElectionTexts[] | undefined;
			writeInAlphabet: string;
		}>(),
		'Vote Cast Return Code Loaded': props<{ voteCastReturnCode: string }>(),
		'Authentication Failed': props<{ error: BackendError }>(),
	},
});

export const ChooseActions = createActionGroup({
	source: 'Choose Page',
	events: {
		'Form Submitted': emptyProps(),
		'Review Clicked': props<{ voterAnswers: VoterAnswers }>(),
	},
});

export const ReviewActions = createActionGroup({
	source: 'Review Page',
	events: {
		'Seal Vote Clicked': emptyProps(),
		'Seal Vote Canceled': emptyProps(),
		'Sealed Vote Loaded': props<{
			shortChoiceReturnCodes: ShortChoiceReturnCode[];
		}>(),
		'Sealed Vote Load Failed': props<{ error: BackendError }>(),
	},
});

export const VerifyActions = createActionGroup({
	source: 'Review Page',
	events: {
		'Cast Vote Clicked': props<{ confirmationKey: string }>(),
		'Cast Vote Loaded': props<{ voteCastReturnCode: string }>(),
		'Cast Vote Load Failed': props<{ error: BackendError }>(),
	},
});

export const ConfirmActions = createActionGroup({
	source: 'Confirm Page',
	events: {
		'End Clicked': emptyProps(),
	},
});

export const SharedActions = createActionGroup({
	source: 'Generic',
	events: {
		'Logged out': emptyProps(),
		'Server Error Cleared': emptyProps(),
	},
});
