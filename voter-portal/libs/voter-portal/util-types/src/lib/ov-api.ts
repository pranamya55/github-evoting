/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AuthenticateVoterResponse } from './authenticate-voter-response';
import { VoterAnswers } from 'e-voting-libraries-ui-kit';
import { ShortChoiceReturnCode } from './short-choice-return-code';
import { VoterPortalConfig } from './voter-portal-config';

export interface OvApi {
	authenticateVoter(
		svk: string,
		extendedFactor: string,
		electionEventId: string,
		identification: string,
	): Promise<AuthenticateVoterResponse>;

	sendVote(
		answers: VoterAnswers,
	): Promise<{ shortChoiceReturnCodes: ShortChoiceReturnCode[] }>;

	confirmVote(confirmationKey: string): Promise<{ voteCastReturnCode: string }>;

	isBrowserCompatible(): Promise<boolean>;

	configureVoterPortal(electionEventId: string): Promise<VoterPortalConfig>;
}
