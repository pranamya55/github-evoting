/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AuthenticateVoterResponse,
	ShortChoiceReturnCode,
	Voter,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import { VoterAnswers } from 'e-voting-libraries-ui-kit';

export abstract class BackendService {
	/**
	 * @throws BackendError
	 */
	abstract authenticateVoter(
		voter: Voter,
		config: VoterPortalConfig,
	): Promise<AuthenticateVoterResponse>;

	abstract sendVote(answers: VoterAnswers): Promise<ShortChoiceReturnCode[]>;

	abstract confirmVote(confirmationKey: string): Promise<string>;

	abstract isBrowserCompatible(): Promise<boolean>;

	abstract configureVoterPortal(
		electionEventId: string,
	): Promise<VoterPortalConfig>;
}
