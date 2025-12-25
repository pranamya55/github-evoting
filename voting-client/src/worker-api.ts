/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {SendVoteProcess} from "./process/send-vote.process";
import {SendVoteResponse} from "./domain/send-vote.types";
import {ConfirmVoteProcess} from "./process/confirm-vote.process";
import {ConfirmVoteResponse} from "./domain/confirm-vote.types";
import {AuthenticateVoterProcess} from "./process/authenticate-voter.process";
import {AuthenticateVoterResponse} from "./domain/authenticate-voter.types";
import {VoterAnswers} from "e-voting-libraries-ui-kit";
import {CompatibilityCheckProcess} from "./process/compatibility-check.process";
import {ConfigureVoterPortalProcess} from "./process/configure-voter-portal.process";

/**
 * Provides an api for the worker calls.
 */
class WorkerApi {
	public static authenticateVoter(startVotingKey: string, extendedAuthenticationFactor: string, electionEventId: string, identification: string): Promise<AuthenticateVoterResponse> {
		const authenticateVoterProcess: AuthenticateVoterProcess = new AuthenticateVoterProcess();
		return authenticateVoterProcess.authenticateVoter(startVotingKey, extendedAuthenticationFactor, electionEventId, identification);
	}

	public static sendVote(voterAnswers: VoterAnswers): Promise<SendVoteResponse> {
		const sendVoteProcess: SendVoteProcess = new SendVoteProcess();
		return sendVoteProcess.sendVote(voterAnswers);
	}

	public static confirmVote(ballotCastingKey: string): Promise<ConfirmVoteResponse> {
		const confirmVoteProcess: ConfirmVoteProcess = new ConfirmVoteProcess();
		return confirmVoteProcess.confirmVote(ballotCastingKey);
	}

	public static isBrowserCompatible(): Promise<boolean> {
		const compatibilityCheckProcess: CompatibilityCheckProcess = new CompatibilityCheckProcess();
		return compatibilityCheckProcess.isBrowserCompatible();
	}

	public static configureVoterPortal(electionEventId: string): Promise<{}> {
		const configureVoterPortalProcess: ConfigureVoterPortalProcess = new ConfigureVoterPortalProcess();
		return configureVoterPortalProcess.configureVoterPortal(electionEventId);
	}
}

/**
 * @typedef {object} WorkerRequestPayload.
 * @property {string} operation, the operation to invoke.
 * @property {Array} args, arguments for the invoked operation.
 */

/**
 * @typedef {object} MessageEvent.
 * @property {WorkerRequestPayload} data, the request from the api.
 */

/**
 * Handles a request from the api.
 * @param {MessageEvent} workerMessage - the message received from the worker.
 * @returns {Promise<unknown>} - operation response.
 */
self.onmessage = function handleRequestFromApi(workerMessage: MessageEvent): void {
	const {operation, args} = workerMessage.data;
	// Validate operation
	if (!operation || !WorkerApi[operation]) {
		return;
	}
	let response;
	try {
		response = WorkerApi[operation](...args);
	} catch (error) {
		throw new Error(`Error calling the Worker API. [operation: "${operation}, error:${error}]`);
	}

	// Handling only the result and error (no progress or pending).
	response.then(
		(promiseResult: any) => {
			self.postMessage({
				operation: operation,
				result: promiseResult
			});
		},
		(promiseError: any) => {
			// Prepare log error
			let loggedMessage: string;
			let loggedCause: string;
			if (!promiseError.cause) {
				loggedMessage = "Unexpected error.";
				loggedCause = (promiseError.message ? promiseError.message : promiseError.name);
			} else {
				loggedMessage = promiseError.message;
				loggedCause = promiseError.cause;
			}

			const consoleError: ConsoleError = {
				operation: operation,
				message: loggedMessage,
				cause: loggedCause
			};
			console.error("Error:", consoleError);

			// Bubble up the error
			self.postMessage({
				operation: operation,
				error: promiseError.errorResponse ? promiseError.errorResponse : {status: 0}
			});
		}
	);
};

interface ConsoleError {
	operation: string;
	message: string;
	cause: string;
}
